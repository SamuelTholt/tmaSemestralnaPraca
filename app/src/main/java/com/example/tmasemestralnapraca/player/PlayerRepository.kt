package com.example.tmasemestralnapraca.player

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlayerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val playersCollection = firestore.collection("players")

    suspend fun savePlayer(player: PlayerModel) {
        val newPlayer = player.copy()
        if (newPlayer.id == null) {
            val documentRef = playersCollection.document()
            newPlayer.id = documentRef.id
            documentRef.set(newPlayer).await()
        } else {
            playersCollection.document(newPlayer.id!!).set(newPlayer).await()
        }
    }

    suspend fun updatePlayer(player: PlayerModel) {
        player.id?.let { id ->
            playersCollection.document(id).set(player).await()
        }
    }

    suspend fun deletePlayer(player: PlayerModel) {
        player.id?.let { id ->
            playersCollection.document(id).delete().await()
        }
    }

    suspend fun deletePlayerById(id: String) {
        playersCollection.document(id).delete().await()
    }

    fun getAllPlayers(): Flow<List<PlayerModel>> = callbackFlow {
        val subscription = playersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val players = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PlayerModel::class.java)?.copy(id = doc.id)
                }
                trySend(players)
            }
        }

        awaitClose { subscription.remove() }
    }

    suspend fun getPlayerById(id: String): PlayerModel? {
        return playersCollection.document(id).get().await().toObject(PlayerModel::class.java)
    }

    fun searchPlayers(query: String): Flow<List<PlayerModel>> = callbackFlow {
        val queryLower = query.lowercase()
        val subscription = playersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val players = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PlayerModel::class.java)?.copy(id = doc.id)
                }.filter { player ->
                    "${player.firstName} ${player.lastName}".lowercase().contains(queryLower)
                }
                trySend(players)
            }
        }

        awaitClose { subscription.remove() }
    }

    fun getPlayersSortedByFirstName(): Flow<List<PlayerModel>> = callbackFlow {
        val subscription = playersCollection.orderBy("firstName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val players = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PlayerModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(players)
                }
            }

        awaitClose { subscription.remove() }
    }

    fun getPlayersSortedByLastName(): Flow<List<PlayerModel>> = callbackFlow {
        val subscription = playersCollection.orderBy("lastName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val players = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PlayerModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(players)
                }
            }

        awaitClose { subscription.remove() }
    }

    fun getPlayersSortedByNumber(): Flow<List<PlayerModel>> = callbackFlow {
        val subscription = playersCollection.orderBy("numberOfShirt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val players = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PlayerModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(players)
                }
            }

        awaitClose { subscription.remove() }
    }

    fun getPlayersSortedByPosition(): Flow<List<PlayerModel>> = callbackFlow {
        val subscription = playersCollection.orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val players = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PlayerModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(players)
                }
            }

        awaitClose { subscription.remove() }
    }
}