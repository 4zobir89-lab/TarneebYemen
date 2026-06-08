package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.Card
import com.example.game.GamePhase
import com.example.game.GameState
import com.example.game.GameViewModel
import com.example.game.Suit

@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0F3215) // Deep Casino Green
    ) {
        val backgroundBrush = Brush.radialGradient(
            colors = listOf(Color(0xFF1B5E20), Color(0xFF0F3215)),
            radius = 1500f
        )

        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
            if (state.phase == GamePhase.IDLE) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { viewModel.startGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text("ابدأ اللعبة", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                return@Surface
            }

            // Top Status Info
            TopInfoBar(state)

            // Players representation around the table
            PlayerAvatar(state, 1, Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
            PlayerAvatar(state, 2, Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 64.dp))
            PlayerAvatar(state, 3, Modifier.align(Alignment.TopStart).padding(top = 80.dp, start = 64.dp))
            PlayerAvatar(state, 4, Modifier.align(Alignment.CenterStart).padding(start = 16.dp))

            // Trick Area / Field Area
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                if (state.phase == GamePhase.FIELD_DECISION && state.field.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        state.field.forEachIndexed { index, card ->
                            CardView(card = card, modifier = Modifier.padding(horizontal = 4.dp).offset(y = if (index % 2 == 0) (-10).dp else 10.dp))
                        }
                    }
                } else if (state.trickCards.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        state.trickCards.entries.forEach { (playerId, card) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(" ${playerId + 1}", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                CardView(card = card)
                            }
                        }
                    }
                }
            }

            // Human Player Bottom Area
            HumanPlayerArea(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            )
            
            // Dialogs for interaction
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
}

@Composable
fun TopInfoBar(state: GameState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA000000)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.statusMessage,
                color = Color(0xFFFFD700), // Gold
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Text("المزايدة: ${state.highestBid}", color = Color.White)
                if (state.trumpSuit != null) {
                    Text("الحكم: ${state.trumpSuit!!.symbol} ${state.trumpSuit!!.arabicName}", color = Color(state.trumpSuit!!.color))
                }
                Text("المارات: ${state.roundTrickCount}/8", color = Color.White)
            }
        }
    }
}

@Composable
fun BiddingDialog(state: GameState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = { }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("دورك في المزايدة", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                val nextBid = kotlin.math.max(105, state.highestBid + 5)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.humanPass() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("أنسحب", fontSize = 16.sp)
                    }
                    Button(
                        onClick = { viewModel.humanBid(nextBid) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("مزايدة $nextBid", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FieldDecisionDialog(state: GameState, viewModel: GameViewModel) {
    Dialog(onDismissRequest = { }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("قرار الميدان", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (state.highestBid >= 140) {
                        Button(
                            onClick = { viewModel.humanRejectField() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("رفض (كرت أسود)", fontSize = 16.sp)
                        }
                    }
                    Button(
                        onClick = { viewModel.humanAcceptField() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                    ) {
                        Text("قبول الميدان", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscardingDialog(state: GameState, viewModel: GameViewModel) {
    val player = state.players.first { it.id == 0 }
    var selectedCards by remember { mutableStateOf(setOf<Card>()) }
    var selectedTrump by remember { mutableStateOf<Suit?>(null) }
    
    Dialog(onDismissRequest = { }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("توزيع 4 أوراق واختيار الحكم", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Suit.entries.forEach { suit ->
                        OutlinedButton(
                            onClick = { selectedTrump = suit },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedTrump == suit) Color(0xFFE3F2FD) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp, if (selectedTrump == suit) Color(0xFF1976D2) else Color.LightGray
                            )
                        ) {
                            Text("${suit.symbol} ${suit.arabicName}", color = Color(suit.color), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("اختر ${4 - selectedCards.size} أوراق للتخلص منها", color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Scrollable hand for discard selection
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    player.hand.forEach { card ->
                        val isSelected = selectedCards.contains(card)
                        Box(
                            modifier = Modifier
                                .clickable {
                                    if (isSelected) selectedCards = selectedCards - card
                                    else if (selectedCards.size < 4) selectedCards = selectedCards + card
                                }
                        ) {
                            CardView(
                                card = card,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.Red else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .alpha(if (isSelected) 0.5f else 1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.humanDiscardAndSelectTrump(selectedCards.toList(), selectedTrump!!) },
                    enabled = selectedCards.size == 4 && selectedTrump != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد", fontSize = 18.sp)
                }
            }
        }
    }
}



@Composable
fun PlayerAvatar(state: GameState, playerId: Int, modifier: Modifier = Modifier) {
    val player = state.players.find { it.id == playerId } ?: return
    val isTurn = state.currentTurnPlayerId == playerId

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.hakemId == playerId) {
            Icon(Icons.Default.Star, contentDescription = "Hakem", tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(if (isTurn) Color(0xFFFFD700).copy(alpha = 0.8f) else Color.DarkGray, CircleShape)
                .border(2.dp, if (isTurn) Color.White else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Player", tint = Color.LightGray, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(color = Color(0xAA000000), shape = RoundedCornerShape(4.dp)) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🟥 ${player.redCards} ", color = Color(0xFFFF5252), fontSize = 10.sp)
                    Text("⬛ ${player.blackCards} ", color = Color.Gray, fontSize = 10.sp)
                }
                Text("${player.hand.size} أوراق", color = Color.LightGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun HumanPlayerArea(state: GameState, viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val player = state.players.find { it.id == 0 } ?: return

    Column(
        modifier = modifier
            .background(Color(0xBB000000), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row {
                Text("نقاط المارات: ${player.gatheredCards.sumOf { it.points }}  |  ", color = Color.White, fontSize = 14.sp)
                Text("🟥 ${player.redCards}", color = Color(0xFFFF5252), fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Text("⬛ ${player.blackCards}", color = Color.LightGray, fontSize = 14.sp)
                if (state.hakemId == 0) {
                    Text("  |  👑 حاكم", color = Color(0xFFFFD700), fontSize = 14.sp)
                }
            }
        }

        // Draw human hand for playing or just viewing (Hide during discarding dialog to avoid double render)
        if (state.phase != GamePhase.DISCARDING) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-16).dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                player.hand.forEach { card ->
                    val isValidMove = state.phase == GamePhase.PLAYING && state.currentTurnPlayerId == 0 &&
                        (state.trickSuit == null || card.suit == state.trickSuit || player.hand.none { it.suit == state.trickSuit })
                    
                    val yOffset by animateDpAsState(
                        targetValue = if (isValidMove) (-10).dp else 0.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )

                    CardView(
                        card = card,
                        modifier = Modifier
                            .offset(y = yOffset)
                            .clickable(enabled = isValidMove) {
                                viewModel.humanPlayCard(card)
                            }
                    )
                }
            }
        } else {
           Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun CardView(card: Card, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .width(70.dp)
            .height(100.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            // Top Left
            Column(modifier = Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = card.rank.displayName,
                    color = Color(card.suit.color),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.suit.symbol,
                    color = Color(card.suit.color),
                    fontSize = 12.sp
                )
            }
            // Center
            Text(
                text = card.suit.symbol,
                color = Color(card.suit.color),
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            // Bottom Right (Inverted)
            Column(modifier = Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = card.suit.symbol,
                    color = Color(card.suit.color),
                    fontSize = 12.sp,
                    modifier = Modifier.graphicsLayer(rotationZ = 180f)
                )
                Text(
                    text = card.rank.displayName,
                    color = Color(card.suit.color),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer(rotationZ = 180f)
                )
            }
        }
    }
}


