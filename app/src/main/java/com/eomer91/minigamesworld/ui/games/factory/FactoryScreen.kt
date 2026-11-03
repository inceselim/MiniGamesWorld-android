package com.eomer91.minigamesworld.ui.games.factory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private val Green = Color(0xFF22C55E)
private val Steel = Color(0xFF334155)

fun money(d: Double): String = when {
    abs(d) >= 1e9 -> String.format(Locale.US, "$%.2fB", d / 1e9)
    abs(d) >= 1e6 -> String.format(Locale.US, "$%.2fM", d / 1e6)
    abs(d) >= 1e4 -> String.format(Locale.US, "$%.1fK", d / 1e3)
    else -> "$" + d.toLong()
}

fun productName(p: Product): Int = when (p) {
    Product.ORE -> R.string.fa_ore
    Product.WOOD -> R.string.fa_wood
    Product.COTTON -> R.string.fa_cotton
    Product.IRON -> R.string.fa_iron
    Product.PLANK -> R.string.fa_plank
    Product.FABRIC -> R.string.fa_fabric
    Product.TOOLS -> R.string.fa_tools
    Product.FURNITURE -> R.string.fa_furniture
    Product.CLOTHES -> R.string.fa_clothes
    Product.CAR -> R.string.fa_car
}

fun buildingName(b: Building): Int = when (b) {
    Building.MINE -> R.string.fa_b_mine
    Building.LUMBER -> R.string.fa_b_lumber
    Building.FARM -> R.string.fa_b_farm
    Building.SMELTER -> R.string.fa_b_smelter
    Building.SAWMILL -> R.string.fa_b_sawmill
    Building.WEAVER -> R.string.fa_b_weaver
    Building.WORKSHOP -> R.string.fa_b_workshop
    Building.TAILOR -> R.string.fa_b_tailor
    Building.CARPENTER -> R.string.fa_b_carpenter
    Building.CAR_PLANT -> R.string.fa_b_car_plant
}

@Composable
fun FactoryScreen(viewModel: FactoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) viewModel.save() }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            viewModel.save()
        }
    }
    LaunchedEffect(Unit) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(TICK_MS)
                viewModel.tick(TICK_MS / 1000.0)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        if (!state.loading) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Header(viewModel)
                Conveyor(viewModel, Modifier.padding(top = 8.dp))
                EventBanner(viewModel)
                Tabs(state, viewModel)
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
                    when (state.tab) {
                        FactoryTab.PRODUCTION -> Production(viewModel)
                        FactoryTab.MARKET -> Market(viewModel)
                        FactoryTab.ORDERS -> Orders(viewModel)
                    }
                }
            }
        }
        if (state.offlineEarned > 0) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { }, contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(24.dp).clip(RoundedCornerShape(22.dp)).background(AppColors.Surface).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🏭", fontSize = 44.sp)
                    Text(stringResource(R.string.fa_offline_title), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        stringResource(R.string.fa_offline_body, formatDuration(state.offlineSeconds), money(state.offlineEarned)),
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Button(
                        onClick = {
                            showRewarded({ viewModel.closeOffline(doubled = true) }) {
                                scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.fa_double), fontWeight = FontWeight.Black)
                    }
                    TextButton(onClick = { viewModel.closeOffline(doubled = false) }) {
                        Text(stringResource(R.string.fa_collect), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private const val TICK_MS = 200L

private fun formatDuration(s: Long): String = if (s >= 3600) "${s / 3600}h ${(s % 3600) / 60}m" else "${s / 60}m"

@Composable
private fun Header(vm: FactoryViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF14532D), Color(0xFF166534))))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(money(e.cash), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("+" + money(e.incomePerMinute) + " / " + stringResource(R.string.fa_min), color = Color(0xFFBBF7D0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SurfaceHigh)
                .clickable { vm.upgradeWarehouse() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📦 " + e.capacity.toInt(), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
            Text("⬆ " + money(e.warehouseCost), color = if (e.canAfford(e.warehouseCost)) Green else AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Kucuk animasyonlu fabrika bandi: acik binalarin urunleri akar. */
@Composable
private fun Conveyor(vm: FactoryViewModel, modifier: Modifier = Modifier) {
    val paint = remember { android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.CENTER } }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
    ) {
        val f = vm.frame
        val products = vm.engine.unlockedProducts().ifEmpty { listOf(Product.ORE) }
        val beltY = size.height * 0.72f
        drawRoundRect(Steel, Offset(0f, beltY), Size(size.width, size.height * 0.16f), CornerRadius(6f))
        val shift = ((f * 3) % 40).toFloat()
        var x = -40f + shift
        while (x < size.width) {
            drawLine(Color(0xFF64748B), Offset(x, beltY), Offset(x + 14f, beltY + size.height * 0.16f), 3f)
            x += 40f
        }
        paint.textSize = size.height * 0.42f
        val spacing = size.width / 6f
        drawIntoCanvas { c ->
            for (k in 0 until 7) {
                val px = ((k * spacing + f * 2.2f) % (size.width + spacing)) - spacing / 2
                val p = products[(k + (f / 300).toInt()) % products.size]
                c.nativeCanvas.drawText(p.emoji, px, beltY - 4f, paint)
            }
        }
    }
}

@Composable
private fun EventBanner(vm: FactoryViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val ev = vm.engine.event ?: return
    val up = ev.mult > 1
    Text(
        (if (up) "📈 " else "📉 ") + stringResource(if (up) R.string.fa_event_up else R.string.fa_event_down, stringResource(productName(ev.product))) + " ×%.1f · %ds".format(Locale.US, ev.mult, ev.timeLeft.toInt()),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (up) Color(0xFF16A34A) else Color(0xFFDC2626))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun Tabs(state: FactoryUiState, vm: FactoryViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val orders = vm.engine.orders.size
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            FactoryTab.PRODUCTION to "🏭 " + stringResource(R.string.fa_tab_production),
            FactoryTab.MARKET to "💰 " + stringResource(R.string.fa_tab_market),
            FactoryTab.ORDERS to "📋 " + stringResource(R.string.fa_tab_orders) + if (orders > 0) " ($orders)" else "",
        ).forEach { (t, label) ->
            val sel = t == state.tab
            Text(
                label,
                color = if (sel) AppColors.OnPrimary else AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) AppColors.Primary else AppColors.SurfaceHigh)
                    .clickable { vm.selectTab(t) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
            )
        }
    }
}

// ============================================================ Uretim

@Composable
private fun Production(vm: FactoryViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    // Acik binalar + bir sonraki kilitli bina
    val firstLocked = Building.entries.firstOrNull { e.level(it) == 0 }
    val shown = Building.entries.filter { e.level(it) > 0 || it == firstLocked }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(shown, key = { it.name }) { b -> BuildingCard(b, vm) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun BuildingCard(b: Building, vm: FactoryViewModel) {
    val e = vm.engine
    val lvl = e.level(b)
    val cost = b.upgradeCost(lvl)
    val afford = e.canAfford(cost)
    val st = e.status[b] ?: BuildingStatus.IDLE
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceHigh)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(b.emoji, fontSize = 30.sp, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(buildingName(b)), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                if (lvl > 0) Text("  " + stringResource(R.string.fa_level, lvl), color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            val recipe = (if (b.inputs.isEmpty()) "" else b.inputs.entries.joinToString(" + ") { (p, n) -> "$n${p.emoji}" } + " → ") + b.output.emoji
            Text(
                recipe + "   " + "%.2f".format(Locale.US, b.rate * maxOf(lvl, 1)) + "/" + stringResource(R.string.fa_sec),
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
            )
            if (lvl > 0) {
                val (label, color) = when (st) {
                    BuildingStatus.RUNNING -> stringResource(R.string.fa_running) to Green
                    BuildingStatus.NO_INPUT -> stringResource(R.string.fa_no_input) to Color(0xFFF59E0B)
                    BuildingStatus.FULL -> stringResource(R.string.fa_full) to Color(0xFFEF4444)
                    BuildingStatus.IDLE -> "" to AppColors.TextSecondary
                }
                Text("● $label", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Button(
            onClick = { vm.upgrade(b) },
            enabled = afford,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (lvl == 0) AppColors.Gold else AppColors.Primary, contentColor = AppColors.OnPrimary),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (lvl == 0) R.string.fa_build else R.string.fa_upgrade), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(money(cost), fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

// ============================================================ Pazar

@Composable
private fun Market(vm: FactoryViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    val products = e.unlockedProducts()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(stringResource(R.string.fa_market_hint), color = AppColors.TextSecondary, fontSize = 12.sp)
        }
        items(products, key = { it.name }) { p ->
            val stock = e.stock[p] ?: 0.0
            val price = e.price(p)
            val trend = price / p.basePrice
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.SurfaceHigh)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.emoji, fontSize = 28.sp, modifier = Modifier.width(42.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(productName(p)), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                    LinearProgressIndicator(
                        progress = { (stock / e.capacity).toFloat().coerceIn(0f, 1f) },
                        color = if (stock >= e.capacity) Color(0xFFEF4444) else AppColors.Primary,
                        trackColor = Color(0x22000000),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                    Text(
                        "${stock.toInt()} / ${e.capacity.toInt()}  ·  " + String.format(Locale.US, "$%.2f", price) + (if (trend > 1.05) " ▲" else if (trend < 0.95) " ▼" else ""),
                        color = when {
                            trend > 1.05 -> Green
                            trend < 0.95 -> Color(0xFFEF4444)
                            else -> AppColors.TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { vm.sell(p) },
                        enabled = stock >= 1,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                    ) { Text(stringResource(R.string.fa_sell), fontWeight = FontWeight.Black) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.fa_auto), color = AppColors.TextSecondary, fontSize = 10.sp)
                        Switch(checked = p in e.autoSell, onCheckedChange = { vm.toggleAuto(p) }, modifier = Modifier.padding(start = 2.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ============================================================ Siparisler

@Composable
private fun Orders(vm: FactoryViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = vm.frame
    val e = vm.engine
    if (e.orders.isEmpty()) {
        Text(stringResource(R.string.fa_no_orders), color = AppColors.TextSecondary, modifier = Modifier.padding(12.dp))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(e.orders) { i, o ->
            val have = e.stock[o.product] ?: 0.0
            val ready = have >= o.qty
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.SurfaceHigh)
                    .border(if (ready) 2.dp else 0.dp, if (ready) Green else Color.Transparent, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(o.product.emoji, fontSize = 28.sp, modifier = Modifier.width(42.dp))
                Column(Modifier.weight(1f)) {
                    Text("${o.qty} × " + stringResource(productName(o.product)), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
                    LinearProgressIndicator(
                        progress = { (have / o.qty).toFloat().coerceIn(0f, 1f) },
                        color = Green,
                        trackColor = Color(0x22000000),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                    Text("⏱ " + formatClock(o.timeLeft) + "   💰 " + money(o.reward), color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { vm.deliver(i) },
                    enabled = ready,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                ) { Text(stringResource(R.string.fa_deliver), fontWeight = FontWeight.Black) }
            }
        }
    }
}

private fun formatClock(s: Double): String {
    val t = s.toInt().coerceAtLeast(0)
    return "%d:%02d".format(t / 60, t % 60)
}
