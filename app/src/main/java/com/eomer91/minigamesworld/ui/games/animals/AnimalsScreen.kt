package com.eomer91.minigamesworld.ui.games.animals

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.drawVectorFit
import com.eomer91.minigamesworld.ui.theme.AppColors
import java.util.Locale

/** Telefonun konusma motoru; dil uygulamanin diline gore. */
private class Speaker(context: android.content.Context, private val locale: Locale) {
    private var ready = false
    private var pending: String? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            pending?.let { speak(it) }
            pending = null
        }
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        runCatching {
            val r = tts.setLanguage(locale)
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.ENGLISH)
            tts.setPitch(1.15f)
            tts.setSpeechRate(0.9f)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "animals")
        }
    }

    fun shutdown() = runCatching { tts.stop(); tts.shutdown() }
}

@Composable
private fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val speaker = remember(locale) { Speaker(context, locale) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}

@Composable
fun AnimalsScreen(viewModel: AnimalsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()
    val showInterstitial = rememberInterstitialLauncher()
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFF8EC)),
    ) {
        when (state.mode) {
            AnimalsMode.MENU -> Menu(onExplore = viewModel::explore, onQuiz = { showInterstitial { viewModel.startQuiz() } })
            AnimalsMode.EXPLORE -> Explore(state, viewModel, speaker)
            AnimalsMode.QUIZ -> Quiz(state, viewModel, speaker)
            AnimalsMode.QUIZ_DONE -> QuizDone(state, onAgain = { showInterstitial { viewModel.startQuiz() } }, onMenu = viewModel::menu)
        }
    }
}

@Composable
private fun AnimalImage(a: Animal, modifier: Modifier = Modifier, padding: Float = 0.06f) {
    Canvas(modifier) {
        drawVectorFit(AnimalArt.parts[a.id] ?: emptyList(), Rect(0f, 0f, size.width, size.height), padding) { null }
    }
}

@Composable
private fun Menu(onExplore: () -> Unit, onQuiz: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Animal.DOG, Animal.CAT, Animal.DUCK, Animal.LION).forEach { AnimalImage(it, Modifier.size(70.dp)) }
        }
        Text(stringResource(R.string.game_animals_name), color = Color(0xFF7A4A2A), fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
        BigButton("👆 " + stringResource(R.string.an_explore), stringResource(R.string.an_explore_desc), Color(0xFF4CC9F0), onExplore)
        BigButton("❓ " + stringResource(R.string.an_quiz), stringResource(R.string.an_quiz_desc), Color(0xFFFF8FAB), onQuiz)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Animal.COW, Animal.FROG, Animal.ELEPHANT, Animal.MONKEY).forEach { AnimalImage(it, Modifier.size(70.dp)) }
        }
    }
}

@Composable
private fun BigButton(title: String, sub: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(sub, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================ Kesfet

@Composable
private fun Explore(state: AnimalsUiState, viewModel: AnimalsViewModel, speaker: Speaker) {
    val shown = state.shown
    val text = shown?.let { stringResource(it.nameRes) + ". " + stringResource(it.soundRes) }
    LaunchedEffect(state.tapCount) { text?.let(speaker::speak) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A4A2A), modifier = Modifier.clickable { viewModel.menu() }.padding(8.dp))
                Text(stringResource(R.string.an_tap_hint), color = Color(0xFF7A4A2A), fontWeight = FontWeight.Bold)
            }
            Animal.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { a ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(AnimalArt.background[a.id] ?: 0xFFFFFFFF))
                                .clickable { viewModel.show(a) }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AnimalImage(a, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(stringResource(a.nameRes), color = Color(0xFF5A3A2A), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        if (shown != null) {
            val bounce = remember { Animatable(1f) }
            LaunchedEffect(state.tapCount) {
                bounce.snapTo(0.75f)
                bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { viewModel.show(null) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(AnimalArt.background[shown.id] ?: 0xFFFFFFFF))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimalImage(shown, Modifier.size(220.dp).scale(bounce.value).clickable { viewModel.show(shown) })
                    Text(stringResource(shown.nameRes), color = Color(0xFF5A3A2A), fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(
                        "🔊 " + stringResource(shown.soundRes),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFF8FAB))
                            .clickable { viewModel.show(shown) }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

// ============================================================ Bil bakalim

@Composable
private fun Quiz(state: AnimalsUiState, viewModel: AnimalsViewModel, speaker: Speaker) {
    val q = state.question ?: return
    val question = when (q.kind) {
        QuestionKind.NAME -> stringResource(R.string.an_q_which, stringResource(q.answer.nameRes))
        QuestionKind.SOUND -> stringResource(R.string.an_q_sound, stringResource(q.answer.soundRes))
    }
    val praise = stringResource(R.string.an_correct, stringResource(q.answer.nameRes))
    val tryAgain = stringResource(R.string.an_try_again)
    LaunchedEffect(state.index) { speaker.speak(question) }
    LaunchedEffect(state.solved) { if (state.solved) speaker.speak(praise) }
    LaunchedEffect(state.wrong.size) { if (state.wrong.isNotEmpty()) speaker.speak(tryAgain) }

    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A4A2A), modifier = Modifier.clickable { viewModel.menu() }.padding(8.dp))
            Spacer(Modifier.weight(1f))
            Text("${state.index + 1} / ${state.questions.size}", color = Color(0xFF7A4A2A), fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("⭐ ${state.correct}", color = Color(0xFFB45309), fontWeight = FontWeight.Black)
        }
        Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable { speaker.speak(question) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔊", fontSize = 26.sp)
            Text(question, color = Color(0xFF5A3A2A), fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        q.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    val wrong = a in state.wrong
                    val right = state.solved && a == q.answer
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(AnimalArt.background[a.id] ?: 0xFFFFFFFF).copy(alpha = if (wrong) 0.35f else 1f))
                            .border(
                                if (right || wrong) 4.dp else 0.dp,
                                if (right) Color(0xFF22C55E) else if (wrong) Color(0xFFEF4444) else Color.Transparent,
                                RoundedCornerShape(22.dp),
                            )
                            .clickable { viewModel.answer(a) },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimalImage(a, Modifier.fillMaxSize().padding(8.dp).scale(if (right) 1.08f else 1f))
                        if (wrong) Text("✕", color = Color(0xFFEF4444), fontSize = 40.sp, fontWeight = FontWeight.Black)
                        if (right) Text("✓", color = Color(0xFF22C55E), fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizDone(state: AnimalsUiState, onAgain: () -> Unit, onMenu: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("★".repeat(state.stars) + "☆".repeat(3 - state.stars), color = Color(0xFFF59E0B), fontSize = 54.sp)
        Text(stringResource(R.string.an_result, state.correct, state.questions.size), color = Color(0xFF5A3A2A), fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Animal.PIG, Animal.CHICKEN, Animal.SHEEP).forEach { AnimalImage(it, Modifier.size(80.dp).clip(CircleShape)) }
        }
        Button(
            onClick = onAgain,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8FAB), contentColor = Color.White),
            modifier = Modifier.padding(top = 20.dp).height(52.dp),
        ) { Text(stringResource(R.string.game_play_again), fontWeight = FontWeight.Black, fontSize = 18.sp) }
        Text(stringResource(R.string.an_menu), color = AppColors.Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp).clickable(onClick = onMenu).padding(8.dp))
    }
}
