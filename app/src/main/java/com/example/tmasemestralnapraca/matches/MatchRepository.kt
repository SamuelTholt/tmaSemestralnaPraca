package com.example.tmasemestralnapraca.matches

import com.example.tmasemestralnapraca.matches.matchEvent.EventWithPlayer
import com.example.tmasemestralnapraca.matches.matchEvent.MatchEvent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchRepository {
    private val db = FirebaseFirestore.getInstance()
    private val matchesCollection = db.collection("matches")
    private val playersCollection = db.collection("players")
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
            val eventsSnapshot = eventsCollection.whereEqualTo("matchId", matchId).get().await()
            for (document in eventsSnapshot.documents) {
                document.reference.delete().await()
            }

            matchesCollection.document(matchId).delete().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    suspend fun deleteMatchEventById(id: String) {
        eventsCollection.document(id).delete().await()
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

        // Vrátiť MatchDetail
        return MatchDetail(
            match = match,
            events = eventsWithPlayers
        )
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



    fun getFilteredAndSortedMatches(played: Boolean?, sortDirection: Query.Direction?): Flow<List<MatchModel>> = callbackFlow {
        val baseQuery = matchesCollection

        var query = baseQuery as Query

        if (played != null) {
            query = query.whereEqualTo("played", played)
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

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val sortedMatches = matches.sortedWith { match1, match2 ->
                    try {
                        val date1 = dateFormat.parse(match1.date) ?: Date(0)
                        val date2 = dateFormat.parse(match2.date) ?: Date(0)

                        if (sortDirection == Query.Direction.DESCENDING) {
                            date2.compareTo(date1)
                        } else {
                            date1.compareTo(date2)
                        }
                    } catch (e: Exception) {
                        if (sortDirection == Query.Direction.DESCENDING) {
                            match2.date.compareTo(match1.date)
                        } else {
                            match1.date.compareTo(match2.date)
                        }
                    }
                }

                trySend(sortedMatches)
            }
        }

        awaitClose { subscription.remove() }
    }

}
