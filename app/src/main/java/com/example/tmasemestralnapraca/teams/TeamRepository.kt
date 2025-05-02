package com.example.tmasemestralnapraca.teams

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TeamRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val teamsCollection = firestore.collection("teams")


    suspend fun saveTeam(team: TeamModel) {
        val newTeam = team.copy()
        if (newTeam.id == null) {
            val documentRef = teamsCollection.document()
            newTeam.id = documentRef.id
            documentRef.set(newTeam).await()
        } else {
            teamsCollection.document(newTeam.id!!).set(newTeam).await()
        }
    }

    suspend fun updateTeam(team: TeamModel) {
        team.id?.let { id ->
            teamsCollection.document(id).set(team).await()
        }
    }

    suspend fun deleteTeam(team: TeamModel) {
        team.id?.let { id ->
            teamsCollection.document(id).delete().await()
        }
    }

    suspend fun deleteTeamById(id: String) {
        teamsCollection.document(id).delete().await()
    }

    fun getAllTeams(): Flow<List<TeamModel>> = callbackFlow {
        val subscription = teamsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val teams = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TeamModel::class.java)?.copy(id = doc.id)
                }
                trySend(teams)
            }
        }

        awaitClose { subscription.remove() }
    }

    suspend fun getTeamById(id: String): TeamModel? {
        return teamsCollection.document(id).get().await().toObject(TeamModel::class.java)
    }

    suspend fun getAllTeamsForSelection(): List<TeamModel> = withContext(Dispatchers.IO) {
        val snapshot = teamsCollection.get().await()
        return@withContext snapshot.documents.mapNotNull {
            it.toObject(TeamModel::class.java)?.apply { id = it.id }
        }
    }

    suspend fun getTeamByIdForSelection(teamId: String): TeamModel? = withContext(Dispatchers.IO) {
        val document = teamsCollection.document(teamId).get().await()
        return@withContext document.toObject(TeamModel::class.java)?.apply { id = document.id }
    }


    fun getTeamsSortedByPosition(): Flow<List<TeamModel>> = callbackFlow {
        val subscription = teamsCollection.orderBy("firstName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val teams = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TeamModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(teams)
                }
            }

        awaitClose { subscription.remove() }
    }

    fun getTeamsSortedByRanking(): Flow<List<TeamModel>> = callbackFlow {
        val subscription = teamsCollection
            .orderBy("points", Query.Direction.DESCENDING)
            .orderBy("goalsScored", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val teams = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TeamModel::class.java)?.copy(id = doc.id)
                    }
                    // Dodatočné zoradenie podľa rozdielu gólov
                    val sortedTeams = teams.sortedWith(
                        compareByDescending<TeamModel> { it.points }
                            .thenByDescending { it.goalsScored }
                            .thenByDescending { it.goalsScored - it.goalsConceded } // goalDifference
                    )
                    trySend(sortedTeams)
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun getTeamsOrderedForRanking(): List<TeamModel> {
        return try {
            val snapshot = teamsCollection
                .orderBy("points", Query.Direction.DESCENDING)
                .orderBy("goalsScored", Query.Direction.DESCENDING)
                .get()
                .await()

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(TeamModel::class.java)?.copy(id = doc.id)
            }

            // Dodatočné zoradenie podľa rozdielu gólov
            teams.sortedWith(
                compareByDescending<TeamModel> { it.points }
                    .thenByDescending { it.goalsScored }
                    .thenByDescending { it.goalsScored - it.goalsConceded } // goalDifference
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateTeams(teams: List<TeamModel>) {
        val batch = firestore.batch()

        teams.forEach { team ->
            team.id?.let { id ->
                val teamRef = teamsCollection.document(id)
                batch.set(teamRef, team)
            }
        }

        batch.commit().await()
    }
}