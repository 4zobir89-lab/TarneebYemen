package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.game.*
import com.example.ui.theme.*

@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(TableGreen)) {
        Box(modifier = Modifier.fillMaxSize().background(createFeltBrush()))
        FeltOverlay()

        if (state.phase == GamePhase.IDLE) {
            StartScreen(onClick = { viewModel.startGame() })
            return
        }

        TopInfoBar(state, onBack = { viewModel.resetGame() })

        TableCenterArea(state, viewModel, Modifier.align(Alignment.Center))

        PlayerAvatar(state, 1, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        PlayerAvatar(state, 2, Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 72.dp))
        PlayerAvatar(state, 3, Modifier.align(Alignment.TopStart).padding(top = 100.dp, start = 72.dp))
        PlayerAvatar(state, 4, Modifier.align(Alignment.CenterStart).padding(start = 12.dp))

        HumanPlayerArea(state, viewModel, Modifier.align(Alignment.BottomCenter).fillMaxWidth())

        if (state.phase == GamePhase.GAME_OVER) {
            GameOverDialog(state, viewModel)
        } else if (state.phase == GamePhase.ROUND_END) {
            RoundEndOverlay(state)
        } else if (state.currentTurnPlayerId == 0) {
            when (state.phase) {
                GamePhase.BIDDING -> BiddingDialog(state, viewModel)
                GamePhase.FIELD_DECISION -> FieldDecisionDialog(state, viewModel)
                GamePhase.DISCARDING -> DiscardingDialog(state, viewModel)
                else -> {}
            }
        }
    }
}

// ─── Background ───────────────────────────────────────────────

private fun createFeltBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(TableGreenDark, TableGreen, TableGreenMid),
        startY = 0f, endY = 2000f
    )
}

@Composable
fun FeltOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
        val spacing = 80f
        val diamondSize = 24f
        var row = 0
        var y = 0f
        while (y < size.height) {
            val xOffset = if (row % 2 == 0) 0f else spacing / 2f
            var x = xOffset
            while (x < size.width) {
                val path = Path().apply {
                    moveTo(x, y - diamondSize / 2f)
                    lineTo(x + diamondSize / 2f, y)
                    lineTo(x, y + diamondSize / 2f)
                    lineTo(x - diamondSize / 2f, y)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(1.2f))
                x += spacing
            }
            row++
            y += spacing * 0.7f
        }
    }
}

// ─── Start Screen ─────────────────────────────────────────────

@Composable
fun StartScreen(onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.6f))
        ) {
            Card(
                modifier = Modifier.padding(24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1F0E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("♠ ♥ ♣ ♦", fontSize = 28.sp, color = YemenGold)
                    Spacer(Modifier.height(16.dp))
                    Text("الترمب اليمني", style = MaterialTheme.typography.displayLarge, color = YemenGold)
                    Text("187", style = MaterialTheme.typography.headlineLarge, color = YemenSand)
                    Spacer(Modifier.height(8.dp))
                    Text("Tarneeb Yemeni", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = YemenRed),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 18.dp)
                    ) {
                        Text("ابدأ اللعبة", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(5) { id ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(PlayerColors[id], CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("5 لاعبين", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

// ─── Top Info Bar ─────────────────────────────────────────────

@Composable
fun TopInfoBar(state: GameState, onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .statusBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1F0E)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = YemenGold
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.statusMessage,
                    color = StatusGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            Spacer(Modifier.width(36.dp))
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            InfoChip("المزايدة", if (state.highestBid > 100) "${state.highestBid}" else "-", StatusGold)
            if (state.trumpSuit != null) {
                TrumpBadge(state.trumpSuit!!)
            }
            InfoChip("المارات", "${state.roundTrickCount}/8", StatusWhite)
            if (state.hakemId != null) {
                InfoChip("الحاكم", state.players[state.hakemId!!].name, YemenSand)
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun TrumpBadge(suit: Suit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("الحكم", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(suit.symbol, fontSize = 18.sp, color = Color(suit.color))
            Spacer(Modifier.width(2.dp))
            Text(suit.arabicName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(suit.color))
        }
    }
}

// ─── Trump Reveal Animation ───────────────────────────────────

@Composable
fun TrumpRevealAnimation(trumpSuit: Suit) {
    val flipProgress = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(0.85f) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(trumpSuit) {
        flipProgress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
        revealed = true
        glowAlpha.animateTo(0.7f, tween(300))
        glowAlpha.animateTo(0.15f, tween(500))
        glowAlpha.animateTo(0.5f, tween(400))
        while (true) {
            pulseScale.animateTo(1.05f, tween(600, easing = FastOutSlowInEasing))
            pulseScale.animateTo(0.85f, tween(600, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier.size(130.dp, 185.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = flipProgress.value * 180f
                    cameraDistance = 12f * density
                }
        ) {
            if (flipProgress.value < 0.45f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - (flipProgress.value / 0.45f).coerceIn(0f, 1f)
                        }
                ) {
                    CardBackLarge()
                }
            }
            if (flipProgress.value > 0.55f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                            alpha = ((flipProgress.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
                        }
                        .background(CardWhite, RoundedCornerShape(18.dp))
                        .border(2.dp, Color(trumpSuit.color).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(trumpSuit.symbol, fontSize = 52.sp, color = Color(trumpSuit.color))
                            Spacer(Modifier.height(6.dp))
                            Text(trumpSuit.arabicName, fontSize = 20.sp, color = Color(trumpSuit.color),
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text("الحكم", fontSize = 16.sp, color = YemenGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (revealed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = glowAlpha.value
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }
                    .background(Color(trumpSuit.color).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            )
        }
    }
}

@Composable
fun CardBackLarge() {
    val ctx = LocalContext.current
    Image(
        painter = painterResource(ctx.drawableId("card_back")),
        contentDescription = "ظهر الورقة",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

// ─── Table Center Area ────────────────────────────────────────

@Composable
fun TableCenterArea(state: GameState, viewModel: GameViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.phase == GamePhase.TRUMP_REVEAL && state.trumpSuit != null) {
            TrumpRevealAnimation(state.trumpSuit!!)
        } else if (state.phase == GamePhase.FIELD_DECISION && state.field.isNotEmpty()) {
            CenterLabel("الميدان")
            val fieldAnim = remember { Animatable(0f) }
            LaunchedEffect(state.field) {
                fieldAnim.snapTo(0f)
                fieldAnim.animateTo(1f, tween(600))
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = fieldAnim.value }
            ) {
                state.field.forEachIndexed { index, card ->
                    CardView(
                        card = card,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .offset(y = if (index % 2 == 0) (-12).dp else 12.dp)
                            .graphicsLayer {
                                translationX = (index - (state.field.size - 1) / 2f) * 8f * (1f - fieldAnim.value)
                                alpha = fieldAnim.value
                            }
                    )
                }
            }
        } else if (state.trickCards.isNotEmpty()) {
            CenterLabel("اللعب")
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                state.trickCards.entries.forEachIndexed { index, (playerId, card) ->
                    val isWinner = playerId == state.currentTurnPlayerId && state.phase == GamePhase.PLAYING
                    val winGlow = remember { Animatable(0f) }
                    LaunchedEffect(isWinner) {
                        if (isWinner) {
                            while (true) {
                                winGlow.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                                winGlow.animateTo(0.3f, tween(600, easing = FastOutSlowInEasing))
                            }
                        } else {
                            winGlow.snapTo(0f)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isWinner) YemenGold.copy(alpha = 0.25f + winGlow.value * 0.3f)
                                    else PlayerColors[playerId % PlayerColors.size].copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(2.dp)
                        ) {
                            CardView(
                                card = card,
                                isWinner = isWinner,
                                winGlow = winGlow.value,
                                modifier = Modifier.graphicsLayer {
                                    translationY = if (index % 2 == 0) -8f else 8f
                                    if (isWinner) {
                                        scaleX = 1.1f + winGlow.value * 0.05f
                                        scaleY = 1.1f + winGlow.value * 0.05f
                                        shadowElevation = 12f + winGlow.value * 6f
                                    }
                                }
                            )
                        }
                        Text(
                            state.players[playerId].name,
                            color = if (isWinner) YemenGold else PlayerColors[playerId % PlayerColors.size],
                            fontSize = 10.sp,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        } else if (state.phase == GamePhase.PLAYING) {
            if (state.trumpSuit != null) {
                val suitPulse = remember { Animatable(0.1f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        suitPulse.animateTo(0.2f, tween(2000, easing = FastOutSlowInEasing))
                        suitPulse.animateTo(0.1f, tween(2000, easing = FastOutSlowInEasing))
                    }
                }
                Text(state.trumpSuit!!.symbol, fontSize = 56.sp,
                    color = Color(state.trumpSuit!!.color)
                        .copy(alpha = suitPulse.value))
            }
            if (state.trickCards.isEmpty()) {
                Text("اختر ورقة للعب", color = TextSecondary.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        } else if (state.phase == GamePhase.BIDDING) {
            val biddersLeft = 5 - state.passedPlayers.size
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("المزايدة", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = YemenGold.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Text("المتبقي: $biddersLeft لاعبين",
                    color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CenterLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp))
}

// ─── Player Avatar ────────────────────────────────────────────

@Composable
fun PlayerAvatar(state: GameState, playerId: Int, modifier: Modifier = Modifier) {
    val player = state.players.find { it.id == playerId } ?: return
    val isTurn = state.currentTurnPlayerId == playerId
    val pulseAnim = remember { Animatable(1f) }

    LaunchedEffect(isTurn) {
        if (isTurn) {
            while (true) {
                pulseAnim.animateTo(1.15f, tween(500, easing = FastOutSlowInEasing))
                pulseAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
        } else {
            pulseAnim.snapTo(1f)
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(if (isTurn) 58.dp else 50.dp)
                    .graphicsLayer(scaleX = pulseAnim.value, scaleY = pulseAnim.value)
                    .background(
                        if (isTurn) YemenGold else PlayerColors[playerId % PlayerColors.size],
                        CircleShape
                    )
                    .border(
                        if (isTurn) 3.dp else 1.dp,
                        if (isTurn) Color.White else Color.White.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("${playerId + 1}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (state.hakemId == playerId) {
                Icon(Icons.Default.Star, "حاكم",
                    tint = YemenGold,
                    modifier = Modifier.size(20.dp).offset(x = 4.dp, y = (-4).dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            color = Color(0xDD0A1F0E),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = Color(0xFFFF5252), fontSize = 8.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("${player.redCards}", color = Color(0xFFFF5252), fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("●", color = Color.Gray, fontSize = 8.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("${player.blackCards}", color = Color.Gray, fontSize = 10.sp)
                }
                Text("${player.hand.size}", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Round End Overlay ────────────────────────────────────────

@Composable
fun RoundEndOverlay(state: GameState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.8f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1F0E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("انتهت الجولة", style = MaterialTheme.typography.headlineMedium, color = YemenGold)
                    Spacer(Modifier.height(12.dp))
                    Text(state.statusMessage, color = Color.White, fontSize = 14.sp,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text("النتيجة:", style = MaterialTheme.typography.titleLarge, color = YemenSand)
                    Spacer(Modifier.height(8.dp))
                    state.players.forEach { player ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(PlayerColors[player.id], CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(player.name, color = Color.White, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, modifier = Modifier.width(60.dp))
                            Text("● ${player.redCards}", color = Color(0xFFFF5252), fontSize = 13.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("● ${player.blackCards}", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Game Over Dialog ─────────────────────────────────────────

@Composable
fun GameOverDialog(state: GameState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("انتهت اللعبة!", style = MaterialTheme.typography.headlineLarge, color = YemenGold)
                Spacer(Modifier.height(8.dp))
                Text(state.statusMessage, color = Color.White, fontSize = 15.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                Text("النتيجة النهائية:", style = MaterialTheme.typography.titleMedium, color = YemenSand)
                Spacer(Modifier.height(12.dp))
                state.players.forEach { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp).fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(PlayerColors[player.id], CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(player.name, color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, modifier = Modifier.width(70.dp))
                        Text("● ${player.redCards}", color = Color(0xFFFF5252), fontSize = 14.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("● ${player.blackCards}", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = YemenRed),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("لعبة جديدة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Human Player Area ────────────────────────────────────────

@Composable
fun HumanPlayerArea(state: GameState, viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val player = state.players.find { it.id == 0 } ?: return

    Surface(
        modifier = modifier,
        color = Color(0xDD0A1F0E),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(PlayerColors[0], CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (state.currentTurnPlayerId == 0 && state.phase == GamePhase.PLAYING) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(YemenGold, CircleShape)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.hakemId == 0) {
                        Icon(Icons.Default.Star, "حاكم", tint = YemenGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("● ${player.redCards}", color = Color(0xFFFF5252), fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("● ${player.blackCards}", color = Color.Gray, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.phase != GamePhase.DISCARDING) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy((-20).dp, Alignment.CenterHorizontally)
                ) {
                    player.hand.forEachIndexed { index, card ->
                        val isValidMove = state.phase == GamePhase.PLAYING && state.currentTurnPlayerId == 0 &&
                            (state.trickSuit == null || card.suit == state.trickSuit ||
                             player.hand.none { it.suit == state.trickSuit })

                        val yOffset by animateDpAsState(
                            targetValue = if (isValidMove) (-16).dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )

                        val cardRotation = (player.hand.size / 2f - index) * 1.5f

                        CardView(
                            card = card,
                            showDealAnimation = state.roundNumber > 0,
                            dealIndex = index,
                            modifier = Modifier
                                .offset(y = yOffset)
                                .graphicsLayer {
                                    rotationZ = cardRotation
                                    val pivot = Offset(size.width / 2f, size.height)
                                    transformOrigin = TransformOrigin(pivot.x / size.width, 1f)
                                    if (isValidMove) {
                                        shadowElevation = 8f
                                    }
                                }
                                .clickable(enabled = isValidMove) {
                                    viewModel.humanPlayCard(card)
                                }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ─── Resource Helpers ────────────────────────────────────────

private fun Context.drawableId(name: String): Int {
    return resources.getIdentifier(name, "drawable", packageName)
}

private fun Card.imageName(): String {
    val suitName = when (suit) {
        Suit.HEARTS -> "hearts"
        Suit.DIAMONDS -> "diamonds"
        Suit.CLUBS -> "clubs"
        Suit.SPADES -> "spades"
    }
    val rankFile = when (rank) {
        Rank.ACE -> "a"
        Rank.TWO -> "2"
        Rank.SIX -> "6"
        Rank.SEVEN -> "7"
        Rank.EIGHT -> "8"
        Rank.NINE -> "9"
        Rank.TEN -> "10"
        Rank.JACK -> "j"
        Rank.QUEEN -> "q"
        Rank.KING -> "k"
    }
    return "card_${suitName}_${rankFile}"
}

// ─── Card View ────────────────────────────────────────────────

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    facedown: Boolean = false,
    showDealAnimation: Boolean = false,
    dealIndex: Int = 0,
    isWinner: Boolean = false,
    winGlow: Float = 0f
) {
    var dealProgress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(showDealAnimation, dealIndex) {
        if (showDealAnimation) {
            delay(dealIndex * 70L)
            for (i in 0..12) {
                dealProgress = i / 12f
                delay(16)
            }
            dealProgress = 1f
        }
    }

    val targetScale = 0.85f + dealProgress * 0.15f

    val ctx = LocalContext.current
    Image(
        painter = painterResource(
            if (facedown) ctx.drawableId("card_back") else ctx.drawableId(card.imageName())
        ),
        contentDescription = if (facedown) "ظهر الورقة" else "${card.rank.displayName} ${card.suit.arabicName}",
        modifier = modifier
            .width(64.dp)
            .height(92.dp)
            .graphicsLayer {
                scaleX = targetScale
                scaleY = targetScale
                alpha = dealProgress
                translationY = (1f - dealProgress) * (-80f)
                if (isWinner) {
                    shadowElevation = 12f + winGlow * 6f
                }
            }
            .shadow(
                if (facedown) 2.dp else (if (isWinner) (8.dp + 6.dp * winGlow) else 8.dp),
                RoundedCornerShape(10.dp)
            ),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
fun CardBack() {
    val ctx = LocalContext.current
    Image(
        painter = painterResource(ctx.drawableId("card_back")),
        contentDescription = "ظهر الورقة",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

// ─── Bidding Dialog ───────────────────────────────────────────

@Composable
fun BiddingDialog(state: GameState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text("دورك في المزايدة", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = YemenGold)
                Spacer(Modifier.height(8.dp))
                Text("أعلى مزايدة: ${state.highestBid}", fontSize = 16.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                val biddersLeft = 5 - state.passedPlayers.size
                Text("المتبقي: $biddersLeft لاعبين", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.7f))
                Spacer(Modifier.height(24.dp))

                val nextBid = kotlin.math.max(105, state.highestBid + 5)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.humanPass() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, Color(0xFF666666)),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("انسحب", fontSize = 16.sp, color = Color(0xFF999999))
                    }
                    Button(
                        onClick = { viewModel.humanBid(nextBid) },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenRed),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("مزايدة $nextBid", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Field Decision Dialog ────────────────────────────────────

@Composable
fun FieldDecisionDialog(state: GameState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📋", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text("قرار الميدان", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = YemenGold)
                Spacer(Modifier.height(8.dp))
                Text("المزايدة: ${state.highestBid}", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("أوراق الميدان:", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.field.forEach { card ->
                        CardView(card = card)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.humanAcceptField() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("قبول", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    if (state.highestBid >= 140) {
                        OutlinedButton(
                            onClick = { viewModel.humanRejectField() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(2.dp, Color(0xFFC62828)),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("رفض", fontSize = 16.sp, color = Color(0xFFE57373))
                        }
                    }
                }
            }
        }
    }
}

// ─── Discarding Dialog ────────────────────────────────────────

@Composable
fun DiscardingDialog(state: GameState, viewModel: GameViewModel) {
    val player = state.players.first { it.id == 0 }
    var selectedCards by remember { mutableStateOf(setOf<Card>()) }
    var selectedTrump by remember { mutableStateOf<Suit?>(null) }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎴", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text("توزيع الأوراق واختيار الحكم", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = YemenGold)
                Spacer(Modifier.height(16.dp))

                Text("اختر الحكم (الترمب):", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Suit.entries.forEach { suit ->
                        val isSelected = selectedTrump == suit
                        Surface(
                            modifier = Modifier
                                .clickable { selectedTrump = suit },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(suit.color).copy(alpha = 0.2f) else Color(0xFF2D2D44),
                            border = BorderStroke(2.dp, if (isSelected) Color(suit.color) else Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(suit.symbol, fontSize = 22.sp, color = Color(suit.color))
                                Text(suit.arabicName, fontSize = 11.sp, color = Color(suit.color))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("اختر ${4 - selectedCards.size} أوراق للتخلص منها:",
                    color = if (selectedCards.size < 4) YemenSand else Color(0xFF4CAF50),
                    fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    player.hand.forEach { card ->
                        val isSelected = selectedCards.contains(card)
                        Box(
                            modifier = Modifier.clickable {
                                if (isSelected) selectedCards = selectedCards - card
                                else if (selectedCards.size < 4) selectedCards = selectedCards + card
                            }
                        ) {
                            CardView(
                                card = card,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .then(
                                        if (isSelected) Modifier
                                            .border(3.dp, YemenRed, RoundedCornerShape(10.dp))
                                            .alpha(0.6f)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.humanDiscardAndSelectTrump(selectedCards.toList(), selectedTrump!!) },
                    enabled = selectedCards.size == 4 && selectedTrump != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YemenRed,
                        disabledContainerColor = Color(0xFF444444)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("تأكيد", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
