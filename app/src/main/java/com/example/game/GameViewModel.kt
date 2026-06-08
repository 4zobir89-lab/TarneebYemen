package com.example.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var dealerId = 0
    private var kiayalId = 1
    
    init {
        val players = listOf(
            Player(0, "أنت", true),
            Player(1, "اللاعب 2", false),
            Player(2, "اللاعب 3", false),
            Player(3, "اللاعب 4", false),
            Player(4, "اللاعب 5", false)
        )
        _state.update { it.copy(players = players, phase = GamePhase.IDLE) }
    }

    fun startGame() {
        dealerId = 0
        kiayalId = 1
        startNewRound()
    }

    private fun startNewRound() {
        val deck = mutableListOf<Card>()
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                deck.add(Card(suit, rank))
            }
        }
        deck.shuffle()

        val field = deck.take(5)
        deck.subList(0, 5).clear()

        val currentPlayers = _state.value.players.map {
            val hand = deck.take(7)
            deck.subList(0, 7).clear()
            it.copy(hand = hand.sortedWith(compareBy({ c -> c.suit }, { c -> c.rank.ordinal })).reversed(), gatheredCards = emptyList())
        }

        _state.update { it.copy(
            phase = GamePhase.BIDDING,
            players = currentPlayers,
            field = field,
            currentBidderIndex = kiayalId, // Bidding starts with person right of dealer
            passedPlayers = emptySet(),
            highestBid = 100, // Must bid 105 or higher
            highestBidderId = null,
            hakemId = null,
            trumpSuit = null,
            trickCards = emptyMap(),
            roundTrickCount = 0,
            statusMessage = "بدأت المزايدة",
            trickStarterId = kiayalId,
            currentTurnPlayerId = kiayalId
        )}
        
        handleAITurn()
    }

    fun humanBid(amount: Int) {
        if (_state.value.phase != GamePhase.BIDDING || _state.value.currentTurnPlayerId != 0) return
        placeBid(0, amount)
    }

    fun humanPass() {
        if (_state.value.phase != GamePhase.BIDDING || _state.value.currentTurnPlayerId != 0) return
        passBid(0)
    }

    private fun placeBid(playerId: Int, amount: Int) {
        _state.update { it.copy(highestBid = amount, highestBidderId = playerId) }
        nextBidder()
    }

    private fun passBid(playerId: Int) {
        _state.update { it.copy(passedPlayers = it.passedPlayers + playerId) }
        nextBidder()
    }

    private fun nextBidder() {
        val st = _state.value
        if (st.passedPlayers.size == 4 && st.highestBidderId != null) {
            // Bidding over
            endBidding(st.highestBidderId)
            return
        }
        if (st.passedPlayers.size == 5) {
            // Everyone passed, redeal
            _state.update { it.copy(statusMessage = "الجميع انسحب من المزايدة، يتم إعادة التوزيع...") }
            viewModelScope.launch { delay(2000); kiayalId = (kiayalId + 1) % 5; startNewRound() }
            return
        }

        var nextIndex = (st.currentBidderIndex + 1) % 5
        while (st.passedPlayers.contains(nextIndex)) {
            nextIndex = (nextIndex + 1) % 5
        }
        
        _state.update { it.copy(currentBidderIndex = nextIndex, currentTurnPlayerId = nextIndex) }
        handleAITurn()
    }

    private fun endBidding(hakemId: Int) {
        val bid = _state.value.highestBid
        _state.update { it.copy(hakemId = hakemId, phase = GamePhase.FIELD_DECISION, currentTurnPlayerId = hakemId) }
        
        if (bid <= 135) {
            _state.update { it.copy(statusMessage = "الحاكم اللاعب ${hakemId+1} والمزايدة $bid (يأخذ الميدان مباشرة)") }
            viewModelScope.launch { delay(1500); acceptField(hakemId) }
        } else {
            _state.update { it.copy(statusMessage = "الحاكم اللاعب ${hakemId+1} والمزايدة $bid (كشف الميدان)") }
            if (hakemId != 0) {
                // AI deciding on revealed field
                viewModelScope.launch { delay(2000); acceptField(hakemId) /* simplified: AI always accepts for now */ }
            }
        }
    }
    
    fun humanAcceptField() {
        val st = _state.value
        if (st.phase == GamePhase.FIELD_DECISION && st.currentTurnPlayerId == 0) {
            acceptField(0)
        }
    }
    
    fun humanRejectField() {
        val st = _state.value
        if (st.phase == GamePhase.FIELD_DECISION && st.currentTurnPlayerId == 0) {
            rejectField(0)
        }
    }

    private fun acceptField(hakemId: Int) {
        val st = _state.value
        val hakem = st.players.find { it.id == hakemId }!!
        val newHand = (hakem.hand + st.field).sortedWith(compareBy({ c -> c.suit }, { c -> c.rank.ordinal })).reversed()
        _state.update { state -> 
            val newPlayers = state.players.map { if (it.id == hakemId) it.copy(hand = newHand) else it }
            state.copy(phase = GamePhase.DISCARDING, players = newPlayers, field = emptyList(), statusMessage = "الحاكم يوزع 4 أوراق")
        }
        if (hakemId != 0) {
            viewModelScope.launch { delay(2000); aiDiscard(hakemId) }
        }
    }
    
    private fun rejectField(hakemId: Int) {
        _state.update { state -> 
            val newPlayers = state.players.map { if (it.id == hakemId) it.copy(blackCards = it.blackCards + 1) else it }
            state.copy(players = newPlayers, statusMessage = "رفض الحاكم الميدان. اكتسب كرت أسود.", phase = GamePhase.ROUND_END)
        }
        viewModelScope.launch { delay(3000); checkDahamAndNextRound() }
    }

    // For human discard & select trump
    fun humanDiscardAndSelectTrump(discards: List<Card>, trump: Suit) {
        val st = _state.value
        if (st.phase != GamePhase.DISCARDING || st.currentTurnPlayerId != 0 || discards.size != 4) return
        executeDiscardAndSetTrump(0, discards, trump)
    }

    private fun aiDiscard(hakemId: Int) {
        val st = _state.value
        val hand = st.players.find { it.id == hakemId }!!.hand.toMutableList()
        // Simple AI: sort by points and keep high, discard 4 lowest points that are not Ace/King/10/2.
        hand.sortBy { it.points }
        val discards = hand.take(4)
        
        // Find best suit for trump based on count and high cards
        val remaining = hand.drop(4)
        val bestSuit = remaining.groupBy { it.suit }.maxByOrNull { it.value.size }?.key ?: Suit.CLUBS
        
        executeDiscardAndSetTrump(hakemId, discards, bestSuit)
    }

    private fun executeDiscardAndSetTrump(hakemId: Int, discards: List<Card>, trump: Suit) {
        val st = _state.value
        val newPlayers = st.players.map { p -> 
            if (p.id == hakemId) {
                p.copy(hand = p.hand.filter { it !in discards })
            } else {
                // Give one discarded card to each other player
                val cardToGive = discards[if (p.id < hakemId) p.id else p.id - 1]
                p.copy(hand = (p.hand + cardToGive).sortedWith(compareBy({ c -> c.suit }, { c -> c.rank.ordinal })).reversed())
            }
        }
        _state.update { it.copy(
            players = newPlayers, 
            trumpSuit = trump, 
            phase = GamePhase.PLAYING,
            currentTurnPlayerId = kiayalId,
            trickStarterId = kiayalId,
            statusMessage = "الحكم هو ${trump.arabicName}. يبدأ اللعب!"
        )}
        handleAITurn()
    }

    fun humanPlayCard(card: Card) {
        val st = _state.value
        if (st.phase != GamePhase.PLAYING || st.currentTurnPlayerId != 0) return
        
        // Validate move
        val hand = st.players.find { it.id == 0 }!!.hand
        val trickSuit = st.trickSuit
        if (trickSuit != null && card.suit != trickSuit) {
            val hasSuit = hand.any { it.suit == trickSuit }
            if (hasSuit) return // Invalid move, must follow suit
        }
        
        playCard(0, card)
    }

    private fun playCard(playerId: Int, card: Card) {
        val st = _state.value
        
        val newPlayers = st.players.map { 
            if (it.id == playerId) it.copy(hand = it.hand - card) else it 
        }
        
        val newTrickCards = st.trickCards + (playerId to card)
        val newTrickSuit = st.trickSuit ?: card.suit
        
        _state.update { it.copy(
            players = newPlayers,
            trickCards = newTrickCards,
            trickSuit = newTrickSuit
        )}

        if (newTrickCards.size == 5) {
            // End of trick
            viewModelScope.launch {
                delay(1500)
                resolveTrick()
            }
        } else {
            _state.update { it.copy(currentTurnPlayerId = (playerId + 1) % 5) }
            handleAITurn()
        }
    }

    private fun resolveTrick() {
        val st = _state.value
        val trickSuit = st.trickSuit!!
        val trump = st.trumpSuit!!
        
        var winnerId = st.trickStarterId
        var highestCard = st.trickCards[winnerId]!!
        
        for ((pId, card) in st.trickCards) {
            if (pId == winnerId) continue
            // Compare
            if (card.suit == trump && highestCard.suit != trump) {
                winnerId = pId
                highestCard = card
            } else if (card.suit == highestCard.suit) {
                if (card.rank.ordinal > highestCard.rank.ordinal) {
                    winnerId = pId
                    highestCard = card
                }
            }
        }
        
        val cardsGathered = st.trickCards.values.toList()

        _state.update { state -> 
            val newPlayers = state.players.map { p ->
                if (p.id == winnerId) p.copy(gatheredCards = p.gatheredCards + cardsGathered) else p
            }
            state.copy(
                players = newPlayers,
                trickCards = emptyMap(),
                trickSuit = null,
                currentTurnPlayerId = winnerId,
                trickStarterId = winnerId,
                roundTrickCount = state.roundTrickCount + 1,
                statusMessage = "اللاعب ${winnerId + 1} فاز بالمار"
            )
        }
        
        if (_state.value.roundTrickCount == 8) {
            endRound()
        } else {
            handleAITurn()
        }
    }

    private fun endRound() {
        val st = _state.value
        val hakemId = st.hakemId!!
        val hakem = st.players.find { it.id == hakemId }!!
        
        val totalPoints = hakem.gatheredCards.sumOf { it.points }
        val bid = st.highestBid
        
        val won = totalPoints >= bid
        
        val newPlayers = st.players.map { p ->
            if (p.id == hakemId) {
                if (won) p.copy(redCards = p.redCards + 1)
                else p.copy(blackCards = p.blackCards + 1)
            } else p
        }
        
        _state.update { it.copy(
            phase = GamePhase.ROUND_END,
            players = newPlayers,
            statusMessage = "انتهت الجولة! الحاكم جمع $totalPoints نقطة." + if (won) " (فاز واكتسب كرت أحمر)" else " (خسر واكتسب كرت أسود)"
        )}

        viewModelScope.launch {
            delay(4000)
            checkDahamAndNextRound()
        }
    }

    private fun checkDahamAndNextRound() {
        val st = _state.value
        var newPlayers = st.players
        
        var dahamer = newPlayers.find { it.redCards >= 3 }
        if (dahamer != null) {
            val hasBlacks = newPlayers.any { it.blackCards > 0 }
            if (hasBlacks) {
                 _state.update { it.copy(statusMessage = "اللاعب ${dahamer.id+1} يدحم أصحاب الكروت السوداء!") }
            } else {
                 _state.update { it.copy(statusMessage = "اللاعب ${dahamer.id+1} يدحم الجميع!") }
            }
            // Reset all
            newPlayers = newPlayers.map { it.copy(redCards = 0, blackCards = 0) }
            viewModelScope.launch { delay(4000); finalizeDaham(newPlayers) }
            return
        }
        
        val blackDahamer = newPlayers.find { it.blackCards >= 3 }
        if (blackDahamer != null) {
            _state.update { it.copy(statusMessage = "اللاعب ${blackDahamer.id+1} جمع 3 كروت سوداء! يدحم وحده.") }
            newPlayers = newPlayers.map { if (it.id == blackDahamer.id) it.copy(blackCards = 0) else it }
            viewModelScope.launch { delay(4000); finalizeDaham(newPlayers) }
            return
        }
        
        _state.update { it.copy(players = newPlayers) }
        kiayalId = (kiayalId + 1) % 5
        startNewRound()
    }
    
    private fun finalizeDaham(newPlayers: List<Player>) {
        _state.update { it.copy(players = newPlayers) }
        kiayalId = (kiayalId + 1) % 5
        startNewRound()
    }

    private fun handleAITurn() {
        val st = _state.value
        if (st.currentTurnPlayerId == 0 || st.phase == GamePhase.IDLE || st.phase == GamePhase.ROUND_END) return
        
        viewModelScope.launch {
            delay(1000)
            val currentState = _state.value // re-fetch state after delay
            if (currentState.currentTurnPlayerId == 0) return@launch

            val aiPlayer = currentState.players.find { it.id == currentState.currentTurnPlayerId }!!
            
            when (currentState.phase) {
                GamePhase.BIDDING -> {
                    // Simple AI bid
                    val handValue = aiPlayer.hand.sumOf { it.points }
                    if (handValue > 50 && currentState.highestBid < 140) {
                        placeBid(aiPlayer.id, kotlin.math.max(105, currentState.highestBid + 5))
                    } else if (currentState.highestBid == 100 && handValue > 40) {
                        placeBid(aiPlayer.id, 105)
                    } else {
                        passBid(aiPlayer.id)
                    }
                }
                GamePhase.PLAYING -> {
                    val hand = aiPlayer.hand
                    val trickSuit = currentState.trickSuit
                    val validCards = if (trickSuit != null && hand.any { it.suit == trickSuit }) {
                        hand.filter { it.suit == trickSuit }
                    } else {
                        hand
                    }
                    val cardToPlay = validCards.random() // Very basic random playing for AI
                    playCard(aiPlayer.id, cardToPlay)
                }
                else -> {}
            }
        }
    }
}
