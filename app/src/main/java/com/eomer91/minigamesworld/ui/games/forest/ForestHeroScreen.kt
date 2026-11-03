package com.eomer91.minigamesworld.ui.games.forest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.theme.AppColors
import java.util.Locale

private val CoinGold = Color(0xFFF7D046)

fun ItemSlot.icon(): ImageBitmap = when (this) {
    ItemSlot.WEAPON -> ForestSprites.iWeapon
    ItemSlot.HELMET -> ForestSprites.iHelmet
    ItemSlot.ARMOR -> ForestSprites.iArmor
    ItemSlot.BELT -> ForestSprites.iBelt
    ItemSlot.PANTS -> ForestSprites.iPants
    ItemSlot.BOOTS -> ForestSprites.iBoots
    ItemSlot.CLOAK -> ForestSprites.iCloak
    ItemSlot.NECKLACE -> ForestSprites.iNecklace
    ItemSlot.EARRING -> ForestSprites.iEarring
    ItemSlot.RING -> ForestSprites.iRing
}

/** "+12%" ya da "+40" ya da "+0.3" */
fun formatStat(s: Stat, v: Float): String = when {
    s == Stat.MOVE_SPEED -> "+" + String.format(Locale.US, "%.2f", v)
    s.percent -> "+" + String.format(Locale.US, "%.1f", v).removeSuffix(".0") + "%"
    else -> "+" + v.toInt()
}

@Composable
fun itemName(item: OwnedItem): String =
    stringResource(item.itemRarity.nameRes) + (item.itemSet?.let { " · " + stringResource(it.nameRes) } ?: "") + " · " + stringResource(item.itemSlot.nameRes)

@Composable
fun ItemIcon(item: OwnedItem?, slot: ItemSlot, size: Dp, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val border = item?.let { Color(it.itemRarity.color) } ?: AppColors.SurfaceHigh
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(if (item == null) AppColors.Background else Color(0xFF1B2333))
            .border(if (selected) 3.dp else 2.dp, if (selected) Color.White else border, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.16f)) {
            val img = slot.icon()
            drawImage(
                img,
                srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                srcSize = androidx.compose.ui.unit.IntSize(img.width, img.height),
                dstSize = androidx.compose.ui.unit.IntSize(this.size.width.toInt(), this.size.height.toInt()),
                alpha = if (item == null) 0.25f else 1f,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            )
        }
        if (item != null) {
            item.itemSet?.let { set ->
                Box(Modifier.align(Alignment.TopStart).padding(4.dp).size(7.dp).clip(CircleShape).background(Color(set.color)))
            }
            Text(
                item.level.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 1.dp),
            )
        }
    }
}

@Composable
fun HeroContent(state: ForestUiState, viewModel: ForestViewModel, onAdChest: () -> Unit) {
    val p = state.profile
    val stats = state.heroStats
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        // -------- Ekipman: 5 sol, kahraman, 5 sag
        Column(Modifier.fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::closeHero, modifier = Modifier.clip(CircleShape).background(AppColors.Surface)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = AppColors.TextPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.fg_equipment), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ItemSlot.entries.take(5).forEach { slot ->
                        val item = p.equippedIn(slot)
                        ItemIcon(item, slot, 48.dp, onClick = { item?.let { viewModel.selectItem(it.id) } })
                    }
                }
                Box(Modifier.width(110.dp).height(270.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(100.dp)) {
                        val img = ForestSprites.hero0
                        drawImage(
                            img,
                            srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                            srcSize = androidx.compose.ui.unit.IntSize(img.width, img.height),
                            dstSize = androidx.compose.ui.unit.IntSize(this.size.width.toInt(), this.size.height.toInt()),
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ItemSlot.entries.drop(5).forEach { slot ->
                        val item = p.equippedIn(slot)
                        ItemIcon(item, slot, 48.dp, onClick = { item?.let { viewModel.selectItem(it.id) } })
                    }
                }
            }
        }
        Spacer(Modifier.width(14.dp))

        // -------- Statlar ve set bonuslari
        Column(Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.fg_stats), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Stat.entries.forEach { s ->
                val v = ForestItems.value(stats, s)
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(stringResource(s.nameRes), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            s == Stat.MOVE_SPEED -> String.format(Locale.US, "%.2f", v)
                            s.percent -> String.format(Locale.US, "%.1f", v).removeSuffix(".0") + "%"
                            else -> v.toInt().toString()
                        },
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text(stringResource(R.string.fg_set_bonuses), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
            val counts = ForestItems.setCounts(p.equippedItems())
            ItemSet.entries.forEach { set ->
                val n = counts[set] ?: 0
                Text(
                    stringResource(set.nameRes) + "  " + stringResource(R.string.fg_set_piece, n),
                    color = if (n >= 2) Color(set.color) else AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                set.bonuses.forEach { (pieces, bonus) ->
                    Text(
                        "$pieces: " + bonus.entries.joinToString(", ") { (s, v) -> formatStat(s, v) + " " + stringResource(s.nameRes) },
                        color = if (n >= pieces) AppColors.TextPrimary else AppColors.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))

        // -------- Envanter ve sandiklar
        Column(Modifier.weight(1.3f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.fg_inventory, p.items.size, ForestItems.INVENTORY_LIMIT),
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Text("🪙 " + p.coins, color = CoinGold, fontWeight = FontWeight.Black)
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChestType.entries.forEach { c ->
                    Button(
                        onClick = { viewModel.openChest(c) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (c == ChestType.ROYAL) Color(0xFFA66EE0) else Color(0xFF8B5A2B), contentColor = Color.White),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) {
                        Text(stringResource(c.nameRes) + " · " + c.price, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                }
                Button(
                    onClick = onAdChest,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.fg_ad_chest), fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
            val sorted = p.items.sortedWith(compareByDescending<OwnedItem> { it.itemRarity.ordinal }.thenBy { it.itemSlot.ordinal }.thenByDescending { it.level })
            LazyVerticalGrid(
                columns = GridCells.Adaptive(54.dp),
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(sorted, key = { it.id }) { item ->
                    Box {
                        ItemIcon(item, item.itemSlot, 54.dp, selected = state.selectedItemId == item.id, onClick = { viewModel.selectItem(item.id) })
                        if (p.isEquipped(item)) {
                            Text(
                                "E",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).clip(CircleShape).background(Color(0xFF4ADE80)).padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    state.selectedItem?.let { ItemDialog(it, p, viewModel) }
    state.chestItem?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::closeChest,
            title = { Text(stringResource(R.string.fg_new_item), color = Color(item.itemRarity.color), fontWeight = FontWeight.Black) },
            text = { ItemSummary(item) },
            confirmButton = { TextButton(onClick = viewModel::closeChest) { Text(stringResource(R.string.cw_ok)) } },
        )
    }
}

@Composable
fun ItemSummary(item: OwnedItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(item, item.itemSlot, 56.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(itemName(item), color = Color(item.itemRarity.color), fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(stringResource(R.string.fg_lv, item.level) + " / " + item.itemRarity.maxLevel, color = AppColors.TextSecondary, fontSize = 12.sp)
            ForestItems.stats(item).forEach { (s, v) ->
                Text(formatStat(s, v) + " " + stringResource(s.nameRes), color = AppColors.TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ItemDialog(item: OwnedItem, p: ForestProfile, viewModel: ForestViewModel) {
    val equipped = p.isEquipped(item)
    val current = p.equippedIn(item.itemSlot)?.takeIf { it.id != item.id }
    AlertDialog(
        onDismissRequest = { viewModel.selectItem(null) },
        title = null,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ItemSummary(item)
                if (current != null) {
                    Text(stringResource(R.string.fg_current), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    ForestItems.stats(current).forEach { (s, v) ->
                        Text(formatStat(s, v) + " " + stringResource(s.nameRes), color = AppColors.TextSecondary, fontSize = 12.sp)
                    }
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (equipped) viewModel.unequip(item) else viewModel.equip(item) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (equipped) AppColors.SurfaceHigh else Color(0xFF4ADE80), contentColor = if (equipped) AppColors.TextPrimary else AppColors.OnPrimary),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(if (equipped) R.string.fg_unequip else R.string.fg_equip), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) }
                    Button(
                        onClick = { viewModel.upgrade(item) },
                        enabled = !item.maxed,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoinGold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (item.maxed) stringResource(R.string.cw_max) else stringResource(R.string.fg_upgrade, ForestItems.upgradeCost(item)),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                TextButton(onClick = { viewModel.salvage(item) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.fg_salvage, ForestItems.salvageValue(item)), color = AppColors.Danger, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.selectItem(null) }) { Text(stringResource(R.string.cw_ok)) } },
    )
}
