package com.eomer91.minigamesworld.ui.games.blockpuzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockPuzzleUiState(
    val board: BlockBoard,
    /** Sabit 3 yuva; kullanilan parca null olur. */
    val pieces: List<BlockPiece?>,
    /** Her yuvadaki parca tahtada bir yere sigiyor mu (sigmayanlar soluk cizilir). */
    val placeable: List<Boolean>,
    val score: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val gameOver: Boolean = false,
    val goldEarned: Int = 0,
    /** Son hamle geri alinabilir mi (odullu reklamla, tek seviye). */
    val canUndo: Boolean = false,
    /** Kayitli oyun yuklenirken true; bu sirada tahta cizilmez. */
    val isLoading: Boolean = true,
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class BlockPuzzleViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = BlockPuzzleEngine()
    val boardSize: Int get() = engine.boardSize

    private val goldTracker = GoldTracker()

    /** Son yerlestirmeden onceki durum; "Geri al" bunu geri yukler. */
    private var undoSnapshot: BlockPuzzleUiState? = null

    private fun awardGold(score: Int): Int {
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        return goldTracker.awarded
    }

    private val _state = MutableStateFlow(newState())
    val state: StateFlow<BlockPuzzleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { restoreSavedGame() }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun canPlace(shape: Shape, row: Int, col: Int): Boolean =
        engine.canPlace(_state.value.board, shape, row, col)

    /** Parcayi yerlestirir. Gecersizse false doner ve hicbir sey degismez. */
    fun place(slot: Int, row: Int, col: Int): Boolean {
        val s = _state.value
        if (s.gameOver) return false
        val piece = s.pieces.getOrNull(slot) ?: return false
        if (!engine.canPlace(s.board, piece.shape, row, col)) return false

        val cleared = engine.clearLines(engine.place(s.board, piece, row, col))
        val remaining = s.pieces.mapIndexed { i, p -> if (i == slot) null else p }
        val pieces = if (remaining.all { it == null }) engine.randomPieces() else remaining
        val score = s.score + engine.scoreFor(cleared.linesCleared)
        val gameOver = engine.isGameOver(cleared.board, pieces)

        undoSnapshot = s
        _state.value = s.copy(
            board = cleared.board,
            pieces = pieces,
            placeable = placeableFor(cleared.board, pieces),
            score = score,
            gameOver = gameOver,
            goldEarned = awardGold(score),
            canUndo = true,
        )

        when {
            gameOver -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
            }
            cleared.linesCleared >= 2 -> {
                feedback.play(Sfx.COMBO)
                feedback.vibrate(Haptic.HEAVY)
            }
            cleared.linesCleared == 1 -> {
                feedback.play(Sfx.CLEAR, volume = 0.8f)
                feedback.vibrate(Haptic.LIGHT)
            }
            else -> {
                feedback.play(Sfx.BLOCK)
                feedback.vibrate(Haptic.LIGHT)
            }
        }

        persist()

        // RN surumu rekoru son hamlenin puani eklenmeden kaydediyordu; burada guncel skor kaydedilir.
        if (score > s.best) viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        return true
    }

    /**
     * Odullu reklam sonrasi: son hamleyi geri alir (tahta, parcalar, skor).
     * Kazanilan altin ve rekor geri alinmaz; GoldTracker en yuksek skoru hatirladigi icin
     * ayni puanlar tekrar kazanilinca ikinci kez altin verilmez.
     */
    fun undo() {
        val snapshot = undoSnapshot ?: return
        undoSnapshot = null
        _state.update { current ->
            snapshot.copy(
                best = current.best,
                bestAtStart = current.bestAtStart,
                goldEarned = current.goldEarned,
                canUndo = false,
            )
        }
        persist()
        feedback.play(Sfx.SWIPE)
    }

    /** Odullu reklam sonrasi: eldeki 3 parcayi yenileriyle degistirir. Oyun bittiyse devam ettirebilir. */
    fun rerollPieces() {
        undoSnapshot = null
        _state.update { current ->
            val pieces = engine.randomPieces()
            current.copy(
                pieces = pieces,
                placeable = placeableFor(current.board, pieces),
                gameOver = engine.isGameOver(current.board, pieces),
                canUndo = false,
            )
        }
        persist()
        feedback.play(Sfx.DROP)
    }

    fun restart() {
        undoSnapshot = null
        goldTracker.reset()
        _state.update { newState().copy(best = it.best, bestAtStart = it.best, isLoading = false) }
        persist()
    }

    /** Oyundan cikilip geri gelindiginde kaldigi yerden devam: kayitli oyunu yukler. */
    private suspend fun restoreSavedGame() {
        val save = prefs.savedGame(GAME_ID).first()?.let(BlockPuzzleSave::fromJson)
        val valid = save != null &&
            save.board.size == engine.boardSize &&
            save.board.all { it.size == engine.boardSize } &&
            save.pieces.size == BlockPuzzleEngine.PIECES_PER_ROUND

        if (save == null || !valid) {
            _state.update { it.copy(isLoading = false) }
            return
        }

        val pieces = save.piecesAsDomain().let { if (it.all { p -> p == null }) engine.randomPieces() else it }
        goldTracker.restore(save.goldAwarded)
        _state.update {
            it.copy(
                board = save.board,
                pieces = pieces,
                placeable = placeableFor(save.board, pieces),
                score = save.score,
                gameOver = engine.isGameOver(save.board, pieces),
                goldEarned = save.goldAwarded,
                canUndo = false,
                isLoading = false,
            )
        }
    }

    /** Her hamleden sonra oyunu kaydeder; oyun bittiyse kaydi siler (bir dahaki giris yeni oyun). */
    private fun persist() {
        val s = _state.value
        if (s.isLoading) return
        val json = if (s.gameOver) null else BlockPuzzleSave.from(s, goldTracker.awarded).toJson()
        viewModelScope.launch {
            if (json == null) prefs.clearSavedGame(GAME_ID) else prefs.saveGame(GAME_ID, json)
        }
    }

    private fun newState(): BlockPuzzleUiState {
        val board = engine.emptyBoard()
        val pieces = engine.randomPieces()
        return BlockPuzzleUiState(board = board, pieces = pieces, placeable = placeableFor(board, pieces))
    }

    private fun placeableFor(board: BlockBoard, pieces: List<BlockPiece?>): List<Boolean> =
        pieces.map { it != null && engine.canPlaceAnywhere(board, it.shape) }

    companion object {
        /** RN surumundeki rekor bu id'ye aktarilir (LegacyDataMigrator). */
        const val GAME_ID = "block-puzzle"
    }
}
