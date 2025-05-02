package com.example.tmasemestralnapraca.post

import com.example.tmasemestralnapraca.player.PlayerModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postCollection = firestore.collection("post")

    suspend fun savePost(postModel: PostModel) {
        val newPost = postModel.copy()
        if (newPost.id == null) {
            val documentRef = postCollection.document()
            newPost.id = documentRef.id
            documentRef.set(newPost).await()
        } else {
            postCollection.document(newPost.id!!).set(newPost).await()
        }
    }

    suspend fun updatePost(postModel: PostModel) {
        postModel.id?.let { id ->
            postCollection.document(id).set(postModel).await()
        }
    }

    suspend fun deleteImage(postModel: PostModel) {
        postModel.id?.let { id ->
            postCollection.document(id).delete().await()
        }
    }

    suspend fun deletePostById(id: String) {
        postCollection.document(id).delete().await()
    }

    suspend fun getPostById(id: String): PostModel? {
        return postCollection.document(id).get().await().toObject(PostModel::class.java)
    }

    fun getAllPosts(): Flow<List<PostModel>> = callbackFlow {
        val subscription = postCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java)?.copy(id = doc.id)
                }
                trySend(posts)
            }
        }

        awaitClose { subscription.remove() }
    }

    fun getPostSortedByDateAsc(): Flow<List<PostModel>> = callbackFlow {
        val subscription = postCollection.orderBy("postDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PostModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(posts)
                }
            }

        awaitClose { subscription.remove() }
    }

    fun getPostSortedByDateDesc(): Flow<List<PostModel>> = callbackFlow {
        val subscription = postCollection.orderBy("postDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PostModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(posts)
                }
            }

        awaitClose { subscription.remove() }
    }
}