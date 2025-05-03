package com.example.tmasemestralnapraca.matches.matchEvent

import com.example.tmasemestralnapraca.player.PlayerModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MatchEventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")

    suspend fun saveMatchEvent(matchEvent: MatchEvent) {
        val newEvent = matchEvent.copy()
        if (newEvent.id == null) {
            val documentRef = eventsCollection.document()
            newEvent.id = documentRef.id
            documentRef.set(newEvent).await()
        } else {
            eventsCollection.document(newEvent.id!!).set(newEvent).await()
        }
    }

    suspend fun deleteMatchEvent(matchEvent: MatchEvent) {
        matchEvent.id?.let { id ->
            eventsCollection.document(id).set(matchEvent).await()
        }
    }

    suspend fun deleteEventById(id: String) {
        eventsCollection.document(id).delete().await()
    }

    suspend fun getEventById(id: String): MatchEvent? {
        return eventsCollection.document(id).get().await().toObject(MatchEvent::class.java)
    }

    suspend fun updateEvent(event: MatchEvent) {
        event.id?.let { id ->
            eventsCollection.document(id).set(event).await()
        }
    }

    fun getAllEventsToMatchByMatchId(matchId: String): Flow<List<EventWithPlayer>> = callbackFlow {
        val subscription = eventsCollection
            .whereEqualTo("matchId", matchId)
            .orderBy("minute", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val db = FirebaseFirestore.getInstance()
                    val events = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(MatchEvent::class.java)?.copy(id = doc.id)
                    }

                    // Získaj hráčov pre každý event (asynchrónne, ale jednoduché spojenie)
                    val jobs = events.map { event ->
                        val deferred = CompletableDeferred<EventWithPlayer>()
                        db.collection("players").document(event.playerId ?: "")
                            .get()
                            .addOnSuccessListener { doc ->
                                val player = doc.toObject(PlayerModel::class.java)
                                deferred.complete(EventWithPlayer(event, player))
                            }
                            .addOnFailureListener {
                                deferred.complete(EventWithPlayer(event, null))
                            }
                        deferred
                    }

                    lifecycleScope.launch {
                        val result = jobs.awaitAll()
                        trySend(result).isSuccess
                    }
                }
            }

        awaitClose { subscription.remove() }
    }

}