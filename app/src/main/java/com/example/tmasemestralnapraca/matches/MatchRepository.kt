package com.example.tmasemestralnapraca.matches

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
    private val lineupCollection = db.collection("lineup")
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

}
