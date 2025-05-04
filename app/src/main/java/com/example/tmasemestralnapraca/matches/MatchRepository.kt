package com.example.tmasemestralnapraca.matches

import android.util.Log
import com.example.tmasemestralnapraca.matches.matchEvent.EventType
import com.example.tmasemestralnapraca.matches.matchEvent.EventWithPlayer
import com.example.tmasemestralnapraca.matches.matchEvent.MatchEvent
import com.example.tmasemestralnapraca.matches.matchLineup.LineupPlayer
import com.example.tmasemestralnapraca.matches.matchLineup.PlayerWithStats
import com.example.tmasemestralnapraca.player.PlayerModel
import com.example.tmasemestralnapraca.teams.TeamRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MatchRepository {
    private val db = FirebaseFirestore.getInstance()
    private val matchesCollection = db.collection("matches")
    private val playersCollection = db.collection("players")
    private val lineupCollection = db.collection("lineups")
    private val eventsCollection = db.collection("events")


    suspend fun saveMatch(match: MatchModel) {
        val newMatch = match.copy()
        if (newMatch.id == null) {
            val documentRef = matchesCollection.document()
            newMatch.id = documentRef.id
            documentRef.set(newMatch).await()
        } else {
            matchesCollection.document(newMatch.id!!).set(newMatch).await()
        }
    }

    suspend fun updateMatch(match: MatchModel) {
        match.id?.let { id ->
            matchesCollection.document(id).set(match).await()
        }
    }


    suspend fun deleteMatchById(matchId: String) {
        try {
            val lineupSnapshot = lineupCollection.whereEqualTo("matchId", matchId).get().await()
            for (document in lineupSnapshot.documents) {
                document.reference.delete().await()
            }

            val eventsSnapshot = eventsCollection.whereEqualTo("matchId", matchId).get().await()
            for (document in eventsSnapshot.documents) {
                document.reference.delete().await()
            }

            matchesCollection.document(matchId).delete().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    suspend fun getMatchDetailById(matchId: String): MatchDetail {
        // Získať zápas
        val matchDoc = matchesCollection.document(matchId).get().await()
        val match = matchDoc.toObject(MatchModel::class.java) ?: MatchModel()

        // Získať udalosti
        val eventsSnapshot = eventsCollection.whereEqualTo("matchId", matchId).get().await()
        val events = eventsSnapshot.toObjects(MatchEvent::class.java)

        // Získať všetkých hráčov pre efektívnosť (aby sme sa vyhli opakovaným dopytom)
        val allPlayers = playersCollection.get().await().documents.mapNotNull {
            it.toObject(PlayerModel::class.java)?.apply { id = it.id }
        }

        // Spracovať udalosti s informáciami o hráčoch
        val eventsWithPlayers = events.mapNotNull { event ->
            val player = allPlayers.find { it.id == event.playerId } ?: return@mapNotNull null
            val assistPlayer = event.playerAssistId?.let { assistId ->
                allPlayers.find { it.id == assistId }
            }

            EventWithPlayer(
                event = event,
                player = player,
                assistPlayer = assistPlayer
            )
        }.sortedBy { it.event.minute }

        // Vrátiť MatchDetail bez zostavy
        return MatchDetail(
            match = match,
            events = eventsWithPlayers
        )
    }

    // Pridať hráča do zostavy
    suspend fun addPlayerToLineup(lineupPlayer: LineupPlayer) {
        val newLineupPlayer = lineupPlayer.copy()
        if (newLineupPlayer.id == null) {
            val documentRef = lineupCollection.document()
            newLineupPlayer.id = documentRef.id
            documentRef.set(newLineupPlayer).await()
        } else {
            lineupCollection.document(newLineupPlayer.id!!).set(newLineupPlayer).await()
        }
    }

    // Pridať udalosť do zápasu
    suspend fun addMatchEvent(event: MatchEvent) {
        val newEvent = event.copy()
        if (newEvent.id == null) {
            val documentRef = eventsCollection.document()
            newEvent.id = documentRef.id
            documentRef.set(newEvent).await()
        } else {
            eventsCollection.document(newEvent.id!!).set(newEvent).await()
        }
    }

    // Vymazať hráča zo zostavy
    suspend fun removePlayerFromLineup(lineupId: String) = withContext(Dispatchers.IO) {
        lineupCollection.document(lineupId).delete().await()
    }

    // Získať zoznam všetkých hráčov pre výber do zostavy
    suspend fun getAllPlayers(): List<PlayerModel> = withContext(Dispatchers.IO) {
        val snapshot = playersCollection.get().await()
        return@withContext snapshot.documents.mapNotNull {
            it.toObject(PlayerModel::class.java)?.apply { id = it.id }
        }
    }

    // Získať všetky zápasy
    suspend fun getAllMatches(): List<MatchModel> = withContext(Dispatchers.IO) {
        val snapshot = matchesCollection.get().await()
        val matches = snapshot.documents.mapNotNull {
            it.toObject(MatchModel::class.java)?.apply { id = it.id }
        }

        return@withContext matches
    }
    //
    suspend fun getMatchWithTeamDetails(matchId: String): MatchModel? = withContext(Dispatchers.IO) {
        val matchDoc = matchesCollection.document(matchId).get().await()
        val match = matchDoc.toObject(MatchModel::class.java)?.apply { id = matchDoc.id } ?: return@withContext null

        if (match.opponentTeamId.isNotEmpty()) {
            val teamRepository = TeamRepository()
            val team = teamRepository.getTeamById(match.opponentTeamId)

            team?.let {
                match.apply {
                    val updatedMatch = this.copy(
                        opponentName = it.teamName,
                        opponentLogo = it.teamImageLogoPath ?: ""
                    )
                    return@withContext updatedMatch.apply { id = matchId }
                }
            }
        }

        return@withContext match
    }

    suspend fun updateMatchDetail(matchDetail: MatchDetail): Boolean {
        return try {
            // Najprv aktualizujeme základné dáta zápasu
            val matchRef = db.collection("matches").document(matchDetail.match.id.toString())
            matchRef.set(matchDetail.match)

            // Aktualizujeme zoznam eventov
            val eventsCollection = matchRef.collection("events")
            // Najprv vymažeme všetky existujúce eventy
            eventsCollection.get().await().documents.forEach { doc ->
                doc.reference.delete().await()
            }
            // Potom pridáme aktuálne eventy
            matchDetail.events.forEach { eventWithPlayer ->
                val event = eventWithPlayer.event
                val eventMap = mapOf(
                    "id" to event.id,
                    "matchId" to event.matchId,
                    "playerId" to event.playerId,
                    "eventType" to event.eventType.name,
                    "minute" to event.minute,
                    "assistPlayerId" to event.playerAssistId
                )
                eventsCollection.document(event.id.toString()).set(eventMap).await()
            }

            true
        } catch (e: Exception) {
            Log.e("MatchRepository", "Error updating match detail", e)
            false
        }
    }

    fun getMatches(): Flow<List<MatchModel>> = callbackFlow {
        val subscription = matchesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val matches = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MatchModel::class.java)?.copy(id = doc.id)
                }
                trySend(matches)
            }
        }

        awaitClose { subscription.remove() }
    }

    fun getFilteredAndSortedMatches(played: Boolean?, sortDirection: Query.Direction?): Flow<List<MatchModel>> = callbackFlow {
        val baseQuery = matchesCollection

        var query = baseQuery as Query

        if (played != null) {
            query = query.whereEqualTo("played", played)
        }

        if (sortDirection != null) {
            query = query.orderBy("date", sortDirection)
        }

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val matches = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MatchModel::class.java)?.copy(id = doc.id)
                }
                trySend(matches)
            }
        }

        awaitClose { subscription.remove() }
    }

    suspend fun updatePlayerInLineup(lineupPlayer: LineupPlayer): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val matchLineupCollection = db.collection("match_lineups")

            val querySnapshot = matchLineupCollection
                .whereEqualTo("matchId", lineupPlayer.matchId)
                .whereEqualTo("playerId", lineupPlayer.playerId)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val documentId = querySnapshot.documents[0].id

                matchLineupCollection.document(documentId)
                    .update(
                        mapOf(
                            "minutesIn" to lineupPlayer.minutesIn,
                            "minutesOut" to lineupPlayer.minutesOut
                        )
                    )
                    .await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MatchRepository", "Error updating player in lineup: ${e.message}")
            false
        }
    }

}
