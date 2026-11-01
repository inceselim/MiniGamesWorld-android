package com.eomer91.minigamesworld.ui.games.sudoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val CellColor = Color(0xFF0F172A)
private val PeerColor = Color(0xFF1E293B)
private val SameDigitColor = Color(0x5938BDF8)
private val SelectedColor = Color(0x9938BDF8)
private val ThinLine = Color(0xFF334155)
private val ThickLine = Color(0xFF94A3B8)
private val SolvedColor = Color(0xFF22C55E)

@Composable
fun SudokuScreen(viewModel: SudokuViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val newGameWithAd = { showInterstitial { viewModel.backToDifficultySelection() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Sure sadece ekran on plandayken ilerler (arka plan / reklam sirasinda durur).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val playing = state.phase == SudokuPhase.PLAYING
    LaunchedEffect(playing) {
        if (playing) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(1_000)
                    viewModel.tick()
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        when (state.phase) {
            SudokuPhase.LOADING -> Unit
            SudokuPhase.CHOOSING, SudokuPhase.GENERATING -> DifficultyPicker(
                generating = state.phase == SudokuPhase.GENERATING,
                onPick = viewModel::startNewGame,
            )
            else -> GameContent(
                state = state,
                viewModel = viewModel,
                onNewGame = newGameWithAd,
                onHint = { showRewarded({ viewModel.useHint() }, adNotReady) },
                onContinueAfterLoss = { showRewarded({ viewModel.continueAfterLoss() }, adNotReady) },
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------- Zorluk secimi ----------

@Composable
private fun DifficultyPicker(generating: Boolean, onPick: (SudokuDifficulty) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.sudoku_choose),
            color = AppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )

        if (generating) {
            CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.padding(top = 40.dp))
            Text(stringResource(R.string.sudoku_generating), color = AppColors.TextSecondary)
            return@Column
        }

        val accents = listOf(SolvedColor, AppColors.Primary, AppColors.Danger)
        SudokuDifficulty.entries.forEachIndexed { index, difficulty ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.SurfaceHigh)
                    .border(2.dp, accents[index].copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onPick(difficulty) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    stringResource(difficulty.labelRes),
                    color = accents[index],
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(R.string.sudoku_difficulty_info, difficulty.clues, difficulty.baseScore),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ---------- Oyun ----------

@Composable
private fun GameContent(
    state: SudokuUiState,
    viewModel: SudokuViewModel,
    onNewGame: () -> Unit,
    onHint: () -> Unit,
    onContinueAfterLoss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TopBar(state, onNewGame)

        SudokuGrid(state = state, onCellClick = viewModel::select, modifier = Modifier.padding(top = 14.dp))

        when (state.phase) {
            SudokuPhase.WON -> GameOverPanel(
                score = state.finalScore,
                isNewRecord = state.isNewRecord,
                onRestart = onNewGame,
                title = stringResource(R.string.sudoku_solved),
                titleColor = SolvedColor,
                detail = stringResource(R.string.sudoku_result, formatTime(state.elapsedSeconds), state.mistakes, state.finalScore),
                goldEarned = state.goldEarned,
                modifier = Modifier.padding(top = 16.dp),
            )
            SudokuPhase.LOST -> LostPanel(onContinue = onContinueAfterLoss, onNewGame = onNewGame)
            else -> {
                ToolRow(
                    notesMode = state.notesMode,
                    onErase = viewModel::erase,
                    onToggleNotes = viewModel::toggleNotesMode,
                    onHint = onHint,
                    modifier = Modifier.padding(top = 14.dp),
                )
                NumberPad(state = state, onDigit = viewModel::input, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun TopBar(state: SudokuUiState, onNewGame: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(state.difficulty.labelRes), color = AppColors.Primary, fontWeight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.sudoku_mistakes, state.mistakes, SudokuViewModel.MAX_MISTAKES),
                    color = if (state.mistakes > 0) AppColors.Danger else AppColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(14.dp))
                Text(formatTime(state.elapsedSeconds), color = AppColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = onNewGame,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
            modifier = Modifier.height(42.dp),
        ) {
            Text(stringResource(R.string.sudoku_new), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SudokuGrid(state: SudokuUiState, onCellClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selected = state.selected
    val selectedValue = selected?.let { state.valueAt(it) } ?: 0
    val peers = remember(selected) { selected?.let { SudokuRules.peers(it).toSet() } ?: emptySet() }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(CellColor),
    ) {
        val cell: Dp = maxWidth / SudokuRules.SIZE

        Column {
            for (r in 0 until SudokuRules.SIZE) {
                Row {
                    for (c in 0 until SudokuRules.SIZE) {
                        val i = r * SudokuRules.SIZE + c
                        val value = state.valueAt(i)
                        val background = when {
                            i == selected -> SelectedColor
                            selectedValue != 0 && value == selectedValue -> SameDigitColor
                            i in peers -> PeerColor
                            else -> CellColor
                        }
                        SudokuCell(
                            state = state,
                            index = i,
                            size = cell,
                            background = background,
                            onClick = { onCellClick(i) },
                        )
                    }
                }
            }
        }

        // Izgara cizgileri: ince her hucrede, kalin 3x3 kutularda.
        Canvas(Modifier.matchParentSize()) {
            val step = size.width / SudokuRules.SIZE
            for (k in 1 until SudokuRules.SIZE) {
                val thick = k % 3 == 0
                val stroke = if (thick) 2.5.dp.toPx() else 1.dp.toPx()
                val color = if (thick) ThickLine else ThinLine
                drawLine(color, Offset(k * step, 0f), Offset(k * step, size.height), stroke)
                drawLine(color, Offset(0f, k * step), Offset(size.width, k * step), stroke)
            }
        }
    }
}

@Composable
private fun SudokuCell(state: SudokuUiState, index: Int, size: Dp, background: Color, onClick: () -> Unit) {
    val density = LocalDensity.current
    val value = state.valueAt(index)

    Box(
        Modifier
            .size(size)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            val color = when {
                state.isGiven(index) -> AppColors.TextPrimary
                state.isWrong(index) -> AppColors.Danger
                else -> AppColors.Primary
            }
            Text(
                value.toString(),
                color = color,
                fontSize = with(density) { (size * 0.56f).toSp() },
                fontWeight = if (state.isGiven(index)) FontWeight.Black else FontWeight.Bold,
            )
        } else if (state.notes[index].isNotEmpty()) {
            val noteSize = with(density) { (size * 0.24f).toSp() }
            Column(Modifier.fillMaxSize().padding(1.dp)) {
                for (r in 0 until 3) {
                    Row(Modifier.weight(1f)) {
                        for (c in 0 until 3) {
                            val digit = r * 3 + c + 1
                            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (digit in state.notes[index]) {
                                    Text(digit.toString(), color = AppColors.TextSecondary, fontSize = noteSize)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    notesMode: Boolean,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolButton(stringResource(R.string.sudoku_erase), Icons.AutoMirrored.Rounded.Backspace, active = false, onClick = onErase, modifier = Modifier.weight(1f))
        ToolButton(
            stringResource(if (notesMode) R.string.sudoku_notes_on else R.string.sudoku_notes_off),
            Icons.Rounded.Edit,
            active = notesMode,
            onClick = onToggleNotes,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            stringResource(R.string.sudoku_hint),
            Icons.Rounded.Lightbulb,
            active = false,
            onClick = onHint,
            modifier = Modifier.weight(1f),
            adBadge = true,
        )
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    adBadge: Boolean = false,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) AppColors.Primary.copy(alpha = 0.25f) else AppColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (active) AppColors.Primary else AppColors.TextPrimary, modifier = Modifier.size(20.dp))
            if (adBadge) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = AppColors.Gold, modifier = Modifier.size(14.dp))
            }
        }
        Text(label, color = if (active) AppColors.Primary else AppColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun NumberPad(state: SudokuUiState, onDigit: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (digit in 1..9) {
            val done = state.placedCount(digit) >= SudokuRules.SIZE
            Box(
                Modifier
                    .weight(1f)
                    .height(54.dp)
                    .alpha(if (done) 0.25f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceHigh)
                    .clickable(enabled = !done) { onDigit(digit) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    digit.toString(),
                    color = if (state.notesMode) AppColors.TextSecondary else AppColors.Primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LostPanel(onContinue: () -> Unit, onNewGame: () -> Unit) {
    Column(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.SurfaceHigh)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.sudoku_lost_title), color = AppColors.Danger, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(
            stringResource(R.string.sudoku_lost_body),
            color = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(48.dp),
        ) {
            Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sudoku_continue), fontWeight = FontWeight.Black)
        }
        OutlinedButton(
            onClick = onNewGame,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(46.dp),
        ) {
            Text(stringResource(R.string.sudoku_new_puzzle), fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
