package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    val soundManager = SoundManager(application)

    private var kiayalId = 1

    init {
        val players = listOf(
            Player(0, "أنت", true),
            Player(1, "شاهر", false),
            Player(2, "ماجد", false),
            Player(3, "فارس", false),
            Player(4, "ناصر", false)
        )
        _state.update { it.copy(players = players, phase = GamePhase.IDLE) }
    }

    fun resetGame() {
        _state.value = GameState()
    }

    fun startGame() {
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
            currentBidderIndex = kiayalId,
            passedPlayers = emptySet(),
            highestBid = 100,
            highestBidderId = null,
            hakemId = null,
            trumpSuit = null,
            trickCards = emptyMap(),
            roundTrickCount = 0,
            statusMessage = "بدأت المزايدة",
            trickStarterId = kiayalId,
            currentTurnPlayerId = kiayalId
        )}

        soundManager.playCardDeal()
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
        soundManager.playBid()
        _state.update { it.copy(highestBid = amount, highestBidderId = playerId) }
        nextBidder()
    }

    private fun passBid(playerId: Int) {
        _state.update { it.copy(passedPlayers = it.passedPlayers + playerId) }
        nextBidder()
    }

    private fun nextBidder() {
        val st = _state.value
        val highestBidderPassed = st.highestBidderId != null && st.passedPlayers.contains(st.highestBidderId)

        if (st.passedPlayers.size >= 4 && !highestBidderPassed && st.highestBidderId != null) {
            endBidding(st.highestBidderId)
            return
        }
        if (st.passedPlayers.size == 5 || highestBidderPassed) {
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
            _state.update { it.copy(statusMessage = "الحاكم ${_state.value.players[hakemId].name} والمزايدة $bid (يأخذ الميدان مباشرة)") }
            viewModelScope.launch { delay(1500); acceptField(hakemId) }
        } else {
            _state.update { it.copy(statusMessage = "الحاكم ${_state.value.players[hakemId].name} والمزايدة $bid (كشف الميدان)") }
            if (hakemId != 0) {
                viewModelScope.launch { delay(2000); acceptField(hakemId) }
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
        soundManager.playLose()
        _state.update { state ->
            val newPlayers = state.players.map { if (it.id == hakemId) it.copy(blackCards = it.blackCards + 1) else it }
            state.copy(players = newPlayers, statusMessage = "رفض الحاكم الميدان. اكتسب كرت أسود.", phase = GamePhase.ROUND_END)
        }
        viewModelScope.launch { delay(3000); checkDahamAndNextRound() }
    }

    fun humanDiscardAndSelectTrump(discards: List<Card>, trump: Suit) {
        val st = _state.value
        if (st.phase != GamePhase.DISCARDING || st.currentTurnPlayerId != 0 || discards.size != 4) return
        executeDiscardAndSetTrump(0, discards, trump)
    }

    private fun aiDiscard(hakemId: Int) {
        val st = _state.value
        val hand = st.players.find { it.id == hakemId }!!.hand.toMutableList()
        hand.sortBy { it.points }
        val discards = hand.take(4)

        val remaining = hand.drop(4)
        val bestSuit = evaluateBestTrumpSuit(remaining)
        executeDiscardAndSetTrump(hakemId, discards, bestSuit)
    }

    private fun evaluateBestTrumpSuit(hand: List<Card>): Suit {
        return hand.groupBy { it.suit }.maxByOrNull { entry ->
            val count = entry.value.size
            val highCards = entry.value.count { it.rank in listOf(Rank.ACE, Rank.KING, Rank.TEN, Rank.TWO) }
            count * 3 + highCards * 2
        }?.key ?: Suit.CLUBS
    }

    private fun executeDiscardAndSetTrump(hakemId: Int, discards: List<Card>, trump: Suit) {
        soundManager.playTrumpReveal()
        val st = _state.value
        val newPlayers = st.players.map { p ->
            if (p.id == hakemId) {
                p.copy(hand = p.hand.filter { it !in discards })
            } else {
                val cardIndex = if (p.id < hakemId) p.id else p.id - 1
                val cardToGive = discards.getOrNull(cardIndex)
                if (cardToGive != null) {
                    p.copy(hand = (p.hand + cardToGive).sortedWith(compareBy({ c -> c.suit }, { c -> c.rank.ordinal })).reversed())
                } else p
            }
        }
        _state.update { it.copy(
            players = newPlayers,
            trumpSuit = trump,
            phase = GamePhase.PLAYING,
            currentTurnPlayerId = hakemId,
            trickStarterId = hakemId,
            statusMessage = "الحكم هو ${trump.arabicName} ${trump.symbol}. ${_state.value.players[hakemId].name} يبدأ!"
        )}
        handleAITurn()
    }

    fun humanPlayCard(card: Card) {
        val st = _state.value
        if (st.phase != GamePhase.PLAYING || st.currentTurnPlayerId != 0) return

        val hand = st.players.find { it.id == 0 }!!.hand
        val trickSuit = st.trickSuit
        if (trickSuit != null && card.suit != trickSuit) {
            val hasSuit = hand.any { it.suit == trickSuit }
            if (hasSuit) return
        }

        playCard(0, card)
    }

    private fun playCard(playerId: Int, card: Card) {
        soundManager.playCardSlap()
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

        val trickEntries = st.trickCards.entries.toList()
        var winnerId = trickEntries.first().key
        var highestCard = trickEntries.first().value

        for (entry in trickEntries) {
            val pId = entry.key
            val card = entry.value
            if (pId == winnerId) continue
            val beatsTrump = card.suit == trump && highestCard.suit != trump
            val beatsSameSuit = card.suit == highestCard.suit && card.rank.ordinal > highestCard.rank.ordinal
            if (beatsTrump || beatsSameSuit) {
                winnerId = pId
                highestCard = card
            }
        }

        soundManager.playWin()
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
                statusMessage = "${state.players[winnerId].name} فاز بالمار!"
            )
        }

        if (_state.value.roundTrickCount >= 8) {
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

        if (won) soundManager.playWin() else soundManager.playLose()

        val newPlayers = st.players.map { p ->
            if (p.id == hakemId) {
                if (won) p.copy(redCards = p.redCards + 1)
                else p.copy(blackCards = p.blackCards + 1)
            } else p
        }

        _state.update { it.copy(
            phase = GamePhase.ROUND_END,
            players = newPlayers,
            statusMessage = "انتهت الجولة! ${hakem.name} جمع $totalPoints نقطة من أصل $bid." +
                    if (won) " (فوز - كرت أحمر!)" else " (خسارة - كرت أسود!)"
        )}

        viewModelScope.launch {
            delay(4000)
            checkDahamAndNextRound()
        }
    }

    private fun checkDahamAndNextRound() {
        val st = _state.value
        var newPlayers = st.players

        val winner = newPlayers.find { it.redCards >= 7 }
        if (winner != null) {
            soundManager.playWin()
            _state.update { it.copy(
                phase = GamePhase.GAME_OVER,
                statusMessage = "${winner.name} فاز باللعبة! ${winner.redCards} كروت حمراء. تهانينا!"
            )}
            return
        }
        val loser = newPlayers.find { it.blackCards >= 7 }
        if (loser != null) {
            soundManager.playLose()
            _state.update { it.copy(
                phase = GamePhase.GAME_OVER,
                statusMessage = "${loser.name} خسر اللعبة! ${loser.blackCards} كروت سوداء."
            )}
            return
        }

        val dahamer = newPlayers.find { it.redCards >= 3 }
        if (dahamer != null) {
            val hasBlacks = newPlayers.any { it.blackCards > 0 }
            soundManager.playWin()
            _state.update { it.copy(
                statusMessage = if (hasBlacks)
                    "دحام! ${dahamer.name} يدحم أصحاب الكروت السوداء!"
                else
                    "دحام! ${dahamer.name} يدحم الجميع!"
            )}
            newPlayers = newPlayers.map { it.copy(redCards = 0, blackCards = 0) }
            viewModelScope.launch { delay(4000); finalizeDaham(newPlayers) }
            return
        }

        val blackDahamer = newPlayers.find { it.blackCards >= 3 }
        if (blackDahamer != null) {
            soundManager.playLose()
            _state.update { it.copy(statusMessage = "${blackDahamer.name} جمع 3 كروت سوداء! يدحم وحده.") }
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
            val currentState = _state.value
            if (currentState.currentTurnPlayerId == 0) return@launch

            val aiPlayer = currentState.players.find { it.id == currentState.currentTurnPlayerId }!!

            when (currentState.phase) {
                GamePhase.BIDDING -> aiBid(aiPlayer, currentState)
                GamePhase.PLAYING -> aiPlayCard(aiPlayer, currentState)
                else -> {}
            }
        }
    }

    private fun aiBid(aiPlayer: Player, st: GameState) {
        val handValue = evaluateHand(aiPlayer.hand, null)
        val likelyTrumpSuit = aiPlayer.hand.groupBy { it.suit }.maxByOrNull { it.value.size }?.key
        val trumpBonus = if (likelyTrumpSuit != null) {
            aiPlayer.hand.count { it.suit == likelyTrumpSuit } * 3
        } else 0
        val totalStrength = handValue + trumpBonus

        val isHighestBidder = st.highestBidderId == aiPlayer.id

        when {
            totalStrength > 70 && st.highestBid < 140 -> {
                placeBid(aiPlayer.id, kotlin.math.max(105, st.highestBid + 5))
            }
            totalStrength > 55 && st.highestBid < 125 -> {
                placeBid(aiPlayer.id, kotlin.math.max(105, st.highestBid + 5))
            }
            st.highestBid == 100 && totalStrength > 45 -> {
                placeBid(aiPlayer.id, 105)
            }
            isHighestBidder -> {
                nextBidder()
            }
            else -> {
                passBid(aiPlayer.id)
            }
        }
    }

    private fun evaluateHand(hand: List<Card>, trumpSuit: Suit?): Int {
        return hand.sumOf { card ->
            val base = card.points
            val trumpBonus = if (card.suit == trumpSuit) 5 else 0
            val rankBonus = when (card.rank) {
                Rank.ACE -> 3
                Rank.KING -> 2
                Rank.TEN, Rank.TWO -> 1
                else -> 0
            }
            base + trumpBonus + rankBonus
        }
    }

    private fun aiPlayCard(aiPlayer: Player, st: GameState) {
        val hand = aiPlayer.hand
        val trickSuit = st.trickSuit
        val trump = st.trumpSuit!!
        val isFirstPlayer = st.trickCards.isEmpty()

        val validCards = if (trickSuit != null && hand.any { it.suit == trickSuit }) {
            hand.filter { it.suit == trickSuit }
        } else {
            hand
        }

        val cardToPlay = if (isFirstPlayer) {
            aiSelectLeadCard(hand, trump)
        } else {
            aiSelectFollowCard(validCards, st.trickCards, trickSuit, trump)
        }

        playCard(aiPlayer.id, cardToPlay)
    }

    private fun aiSelectLeadCard(hand: List<Card>, trump: Suit): Card {
        val hasTrump = hand.any { it.suit == trump }
        if (hasTrump) {
            val highTrump = hand.filter { it.suit == trump }
                .maxByOrNull { it.rank.ordinal }
            if (highTrump != null && highTrump.rank.ordinal >= Rank.KING.ordinal) {
                return highTrump
            }
        }
        val nonTrump = hand.filter { it.suit != trump }
        if (nonTrump.isNotEmpty()) {
            val aceOrKing = nonTrump.firstOrNull { it.rank == Rank.ACE || it.rank == Rank.KING }
            if (aceOrKing != null) return aceOrKing
            return nonTrump.minByOrNull { it.points } ?: hand.first()
        }
        return hand.first()
    }

    private fun aiSelectFollowCard(
        validCards: List<Card>,
        trickCards: Map<Int, Card>,
        trickSuit: Suit?,
        trump: Suit
    ): Card {
        if (trickCards.isEmpty() || trickSuit == null) return validCards.first()

        val currentWinner = findCurrentWinner(trickCards, trickSuit, trump)

        val canWin = validCards.any { card ->
            val beatsWithTrump = card.suit == trump && (currentWinner.suit != trump || card.rank.ordinal > currentWinner.rank.ordinal)
            val beatsSuit = card.suit == trickSuit && card.rank.ordinal > currentWinner.rank.ordinal
            beatsWithTrump || beatsSuit
        }

        return if (canWin) {
            val winningCards = validCards.filter { card ->
                val beatsWithTrump = card.suit == trump && (currentWinner.suit != trump || card.rank.ordinal > currentWinner.rank.ordinal)
                val beatsSuit = card.suit == trickSuit && card.rank.ordinal > currentWinner.rank.ordinal
                beatsWithTrump || beatsSuit
            }
            winningCards.minByOrNull { it.points } ?: validCards.first()
        } else {
            validCards.minByOrNull { it.points } ?: validCards.first()
        }
    }

    private fun findCurrentWinner(trickCards: Map<Int, Card>, trickSuit: Suit, trump: Suit): Card {
        val entries = trickCards.entries.toList()
        if (entries.isEmpty()) return trickCards.values.first()
        var winner = entries.first().value
        for (entry in entries.drop(1)) {
            val card = entry.value
            val beatsTrump = card.suit == trump && winner.suit != trump
            val beatsSameSuit = card.suit == winner.suit && card.rank.ordinal > winner.rank.ordinal
            if (beatsTrump || beatsSameSuit) {
                winner = card
            }
        }
        return winner
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
