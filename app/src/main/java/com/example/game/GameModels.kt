package com.example.game

enum class Suit(val arabicName: String, val color: Long, val symbol: String) {
    CLUBS("كراول", 0xFF000000, "♣"),      // Black
    SPADES("اسبيك", 0xFF000000, "♠"),     // Black
    DIAMONDS("ديمن", 0xFFD32F2F, "♦"),    // Red
    HEARTS("هتين", 0xFFD32F2F, "♥")       // Red
}

enum class Rank(val displayName: String, val pointValue: Int) {
    SIX("6", 0),
    SEVEN("7", 0),
    EIGHT("8", 0),
    NINE("9", 0),
    TEN("10", 10),
    JACK("J", 4),
    QUEEN("Q", 4),
    KING("K", 4),
    ACE("A", 11),
    TWO("2", 10)
}

data class Card(val suit: Suit, val rank: Rank) : Comparable<Card> {
    val points: Int
        get() = if (suit == Suit.CLUBS && rank == Rank.TWO) 25 else rank.pointValue

    override fun compareTo(other: Card): Int {
        return this.rank.ordinal.compareTo(other.rank.ordinal)
    }
}

enum class GamePhase {
    IDLE,
    DEALING,
    BIDDING,
    FIELD_DECISION,
    DISCARDING,
    TRUMP_SELECTION,
    PLAYING,
    ROUND_END,
    GAME_OVER
}

data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val hand: List<Card> = emptyList(),
    val gatheredCards: List<Card> = emptyList(),
    val redCards: Int = 0,
    val blackCards: Int = 0
)

data class GameState(
    val phase: GamePhase = GamePhase.IDLE,
    val players: List<Player> = List(5) { Player(it, "اللاعب ${it + 1}", it == 0) },
    val field: List<Card> = emptyList(),
    val currentTurnPlayerId: Int = 0,
    
    // Bidding
    val highestBid: Int = 0,
    val highestBidderId: Int? = null,
    val currentBidderIndex: Int = 0,
    val passedPlayers: Set<Int> = emptySet(),
    
    // Trump
    val trumpSuit: Suit? = null,
    val hakemId: Int? = null,
    
    // Playing
    val trickCards: Map<Int, Card> = emptyMap(), // Player ID -> Card
    val trickStarterId: Int = 0,
    val trickSuit: Suit? = null,
    val roundTrickCount: Int = 0,
    
    // Status message for UI
    val statusMessage: String = "مرحباً بك في ترمب 187"
)
