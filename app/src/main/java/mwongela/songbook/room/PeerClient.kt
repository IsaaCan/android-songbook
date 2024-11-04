package mwongela.songbook.room

data class PeerClient(
    val username: String,
    val stream: PeerStream?,
    var status: PeerStatus,
)