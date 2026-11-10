package com.eomer91.minigamesworld.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reklamsiz Premium aboneligi (Google Play Billing).
 *
 * Play Console'da: abonelik urunu [PRODUCT_ID], temel planlar "monthly" (1 ay) ve "yearly" (1 yil),
 * her ikisinde yeni musterilere 3 gun ucretsiz deneme teklifi.
 *
 * Premium iken gecis ve banner reklamlari gosterilmez; odullu reklamlar istege bagli kalir.
 * Durum her acilista ve uygulamaya donuste Play'den dogrulanir; arada son bilinen durum
 * (SharedPreferences) kullanilir, boylece acilista reklam "yanip sonmez".
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    /** Satin alinabilir bir plan (temel plan + uygun teklif) */
    class Plan(
        val basePlanId: String,
        val offerToken: String,
        /** Yinelenen fiyat, Play'in bicimiyle ("₺49,99") */
        val price: String,
        val priceMicros: Long,
        /** ISO 8601 donem: P1M, P1Y */
        val period: String,
        /** Ucretsiz deneme gunu (kullanici uygun degilse 0) */
        val trialDays: Int,
        val productDetails: ProductDetails,
    )

    enum class Status { CONNECTING, READY, UNAVAILABLE }

    enum class Event { PURCHASED, PENDING, RESTORED, ERROR }

    private val prefs = context.getSharedPreferences("billing", Context.MODE_PRIVATE)

    private val _premium = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _premium.asStateFlow()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _status = MutableStateFlow(Status.CONNECTING)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Uygulama acilisinda ve on plana donuste cagrilir */
    private var connecting = false

    fun start() {
        if (client.isReady) { refresh(); return }
        if (connecting) return
        connecting = true
        _status.value = Status.CONNECTING
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _status.value = Status.READY
                    refresh()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                    _status.value = Status.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                // enableAutoServiceReconnection: bir sonraki istekte kendiliginden yeniden baglanir
            }
        })
    }

    /** Urunleri ve mevcut abonelikleri yeniden sorgula ("satin alimlari geri yukle") */
    fun refresh(userInitiated: Boolean = false) {
        if (!client.isReady) { start(); return }
        queryProducts()
        queryPurchases(userInitiated)
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Product query failed: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            val pd = details.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
            _plans.value = if (pd == null) emptyList() else buildPlans(pd)
        }
    }

    /**
     * Temel plan basina bir teklif sec: kullanici uygunsa ucretsiz denemeli teklif
     * (Play yalnizca uygun teklifleri dondurur), degilse temel teklif.
     */
    private fun buildPlans(pd: ProductDetails): List<Plan> {
        val offers = pd.subscriptionOfferDetails ?: return emptyList()
        return offers.groupBy { it.basePlanId }.mapNotNull { (basePlan, list) ->
            val trial = list.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
            val chosen = trial ?: list.firstOrNull { it.offerId == null } ?: list.first()
            val phases = chosen.pricingPhases.pricingPhaseList
            val recurring = phases.lastOrNull() ?: return@mapNotNull null
            val free = phases.firstOrNull { it.priceAmountMicros == 0L }
            Plan(
                basePlanId = basePlan,
                offerToken = chosen.offerToken,
                price = recurring.formattedPrice,
                priceMicros = recurring.priceAmountMicros,
                period = recurring.billingPeriod,
                trialDays = free?.let { periodDays(it.billingPeriod) * it.billingCycleCount.coerceAtLeast(1) } ?: 0,
                productDetails = pd,
            )
        }.sortedBy { periodDays(it.period) }
    }

    private fun queryPurchases(userInitiated: Boolean) {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (userInitiated) _events.tryEmit(Event.ERROR)
                return@queryPurchasesAsync
            }
            // Tam liste: aktif abonelik yoksa premium kapanir (iptal edilip suresi dolmus)
            val active = purchases.filter { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            active.forEach(::acknowledgeIfNeeded)
            setPremium(active.isNotEmpty())
            if (userInitiated) _events.tryEmit(Event.RESTORED)
        }
    }

    /** Abonelik ekranini ac */
    fun launch(activity: Activity, plan: Plan): Boolean {
        if (!client.isReady) { start(); return false }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(plan.productDetails)
                        .setOfferToken(plan.offerToken)
                        .build(),
                ),
            )
            .build()
        val r = client.launchBillingFlow(activity, params)
        return r.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val mine = purchases.orEmpty().filter { PRODUCT_ID in it.products }
                when {
                    mine.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> {
                        mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach(::acknowledgeIfNeeded)
                        setPremium(true)
                        _events.tryEmit(Event.PURCHASED)
                    }
                    mine.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> _events.tryEmit(Event.PENDING)
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh(userInitiated = true)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                Log.w(TAG, "Purchase failed: ${result.responseCode} ${result.debugMessage}")
                _events.tryEmit(Event.ERROR)
            }
        }
    }

    /** Onaylanmayan satin alim 3 gun icinde Play tarafindan iade edilir */
    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
        client.acknowledgePurchase(params) { r ->
            if (r.responseCode != BillingClient.BillingResponseCode.OK) Log.w(TAG, "Acknowledge failed: ${r.debugMessage}")
        }
    }

    private fun setPremium(v: Boolean) {
        _premium.value = v
        prefs.edit().putBoolean(KEY_PREMIUM, v).apply()
    }

    /** Google Play abonelik yonetim sayfasi */
    fun manageUrl(): String = "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=${context.packageName}"

    companion object {
        const val PRODUCT_ID = "premium_no_ads"
        private const val KEY_PREMIUM = "premium"
        private const val TAG = "BillingManager"

        /** P3D, P1W, P1M, P1Y -> yaklasik gun */
        fun periodDays(iso: String): Int {
            val m = Regex("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(iso) ?: return 0
            val (y, mo, w, d) = m.destructured
            return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
        }
    }
}
