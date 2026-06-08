package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke

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

        if (state.phase == GamePhase.IDLE) {
            StartScreen(onClick = { viewModel.startGame() })
            return
        }

        TopInfoBar(state)

        TableCenterArea(state, viewModel, Modifier.align(Alignment.Center))

        PlayerAvatar(state, 1, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        PlayerAvatar(state, 2, Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 72.dp))
        PlayerAvatar(state, 3, Modifier.align(Alignment.TopStart).padding(top = 100.dp, start = 72.dp))
        PlayerAvatar(state, 4, Modifier.align(Alignment.CenterStart).padding(start = 12.dp))

        HumanPlayerArea(state, viewModel, Modifier.align(Alignment.BottomCenter).fillMaxWidth())

        if (state.currentTurnPlayerId == 0) {
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

// ─── Start Screen ─────────────────────────────────────────────

@Composable
fun StartScreen(onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.6f))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1F0E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
}

// ─── Top Info Bar ─────────────────────────────────────────────

@Composable
fun TopInfoBar(state: GameState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .statusBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1F0E)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = state.statusMessage,
                color = StatusGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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

// ─── Table Center Area ────────────────────────────────────────

@Composable
fun TableCenterArea(state: GameState, viewModel: GameViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.phase == GamePhase.FIELD_DECISION && state.field.isNotEmpty()) {
            Text("الميدان", style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                modifier = Modifier.align(Alignment.TopCenter).padding(bottom = 4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                state.field.forEachIndexed { index, card ->
                    CardView(card = card, modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .offset(y = if (index % 2 == 0) (-12).dp else 12.dp)
                    )
                }
            }
        } else if (state.trickCards.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                state.trickCards.entries.forEachIndexed { index, (playerId, card) ->
                    val isWinner = playerId == state.currentTurnPlayerId && state.phase == GamePhase.PLAYING
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    PlayerColors[playerId % PlayerColors.size].copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(2.dp)
                        ) {
                            CardView(card = card, modifier = Modifier.graphicsLayer {
                                translationY = if (index % 2 == 0) -8f else 8f
                                if (isWinner) {
                                    scaleX = 1.1f; scaleY = 1.1f
                                }
                            })
                        }
                        Text(state.players[playerId].name, color = PlayerColors[playerId % PlayerColors.size],
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (state.phase == GamePhase.PLAYING && state.trickCards.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.trumpSuit != null) {
                    Text(state.trumpSuit!!.symbol, fontSize = 48.sp,
                        color = Color(state.trumpSuit!!.color).copy(alpha = 0.15f))
                }
                Text("اختر ورقة للعب", color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 13.sp)
            }
        }
    }
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
                    .border(3.dp, if (isTurn) Color.White else Color.Transparent, CircleShape),
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
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5252), CircleShape))
                    Spacer(Modifier.width(2.dp))
                    Text("${player.redCards}", color = Color(0xFFFF5252), fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF616161), CircleShape))
                    Spacer(Modifier.width(2.dp))
                    Text("${player.blackCards}", color = Color.Gray, fontSize = 10.sp)
                }
                Text("${player.hand.size}", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    if (state.currentTurnPlayerId == 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(8.dp).background(YemenGold, CircleShape))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.hakemId == 0) {
                        Icon(Icons.Default.Star, "حاكم", tint = YemenGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("🟥 ${player.redCards}", color = Color(0xFFFF5252), fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("⬛ ${player.blackCards}", color = Color.Gray, fontSize = 13.sp)
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
                            targetValue = if (isValidMove) (-12).dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )

                        val cardRotation = (player.hand.size / 2f - index) * 1.5f

                        CardView(
                            card = card,
                            modifier = Modifier
                                .offset(y = yOffset)
                                .graphicsLayer {
                                    rotationZ = cardRotation
                                    val pivot = Offset(size.width / 2f, size.height)
                                    transformOrigin = TransformOrigin(pivot.x / size.width, 1f)
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

// ─── Card View ────────────────────────────────────────────────

@Composable
fun CardView(card: Card, modifier: Modifier = Modifier, facedown: Boolean = false) {
    val enterTransition = remember { MutableTransitionState(false).apply { targetState = true } }
    var animProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        for (i in 0..10) {
            animProgress = i / 10f
            delay(15)
        }
        animProgress = 1f
    }

    val scale = animateFloatAsState(
        targetValue = if (facedown) 1f else (0.9f + animProgress * 0.1f),
        animationSpec = spring(dampingRatio = 0.6f)
    )

    Surface(
        modifier = modifier
            .width(64.dp)
            .height(92.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = animProgress)
            .shadow(if (facedown) 2.dp else 6.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = if (facedown) CardBackColor else CardWhite
    ) {
        if (facedown) {
            CardBack()
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                Column(modifier = Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.rank.displayName, color = Color(card.suit.color), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(card.suit.symbol, color = Color(card.suit.color), fontSize = 11.sp)
                }
                Text(card.suit.symbol, color = Color(card.suit.color), fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
                Text("${card.points}", color = TextSecondary, fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.Center).offset(y = 18.dp))
                Column(modifier = Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.suit.symbol, color = Color(card.suit.color), fontSize = 11.sp,
                        modifier = Modifier.graphicsLayer(rotationZ = 180f))
                    Text(card.rank.displayName, color = Color(card.suit.color), fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.graphicsLayer(rotationZ = 180f))
                }
            }
        }
    }
}

@Composable
fun CardBack() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRoundRect(CardBackColor, CornerRadius(10f, 10f))
        drawRoundRect(Color(0xFF8B0000), CornerRadius(10f, 10f), topLeft = Offset(4f, 4f),
            size = Size(w - 8f, h - 8f), style = Stroke(width = 2f))
        val cx = w / 2f; val cy = h / 2f
        drawCircle(Color(0x44FFD700), w * 0.25f, Offset(cx, cy))
        drawCircle(Color(0x44FFD700), w * 0.1f, Offset(cx, cy))
        drawLine(Color(0x33FFD700), Offset(cx - 20f, cy), Offset(cx + 20f, cy), strokeWidth = 1.5f)
        drawLine(Color(0x33FFD700), Offset(cx, cy - 20f), Offset(cx, cy + 20f), strokeWidth = 1.5f)
    }
}

// ─── Bidding Dialog ───────────────────────────────────────────

@Composable
fun BiddingDialog(state: GameState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯 دورك في المزايدة", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = YemenGold)
                Spacer(Modifier.height(8.dp))
                Text("أعلى مزايدة: ${state.highestBid}", fontSize = 16.sp, color = TextSecondary)
                Spacer(Modifier.height(20.dp))

                val nextBid = kotlin.math.max(105, state.highestBid + 5)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.humanPass() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, Color(0xFF666666)),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("انسحب", fontSize = 16.sp, color = Color(0xFF999999))
                    }
                    Button(
                        onClick = { viewModel.humanBid(nextBid) },
                        colors = ButtonDefaults.buttonColors(containerColor = YemenRed),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
                        modifier = Modifier.height(52.dp)
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
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📋 قرار الميدان", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = YemenGold)
                Spacer(Modifier.height(8.dp))
                Text("أوراق الميدان:", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.field.forEach { card ->
                        CardView(card = card)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.humanAcceptField() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, Color(0xFF388E3C)),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("قبول ✅", fontSize = 16.sp, color = Color(0xFF4CAF50))
                    }
                    if (state.highestBid >= 140) {
                        Button(
                            onClick = { viewModel.humanRejectField() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("رفض ❌", fontSize = 16.sp)
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
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎴 توزيع 4 أوراق واختيار الحكم", fontSize = 20.sp,
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
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
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
                    Text("تأكيد ✅", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
