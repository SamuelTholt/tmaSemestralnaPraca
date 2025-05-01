package com.example.tmasemestralnapraca.gallery

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ImageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val imageCollection = firestore.collection("gallery")

    suspend fun saveImage(imageModel: ImageModel) {
        val newImage = imageModel.copy()
        if (newImage.id == null) {
            val documentRef = imageCollection.document()
            newImage.id = documentRef.id
            documentRef.set(newImage).await()
        } else {
            imageCollection.document(newImage.id!!).set(newImage).await()
        }
    }

    suspend fun updateImage(imageModel: ImageModel) {
        imageModel.id?.let { id ->
            imageCollection.document(id).set(imageModel).await()
        }
    }

    suspend fun deleteImage(imageModel: ImageModel) {
        imageModel.id?.let { id ->
            imageCollection.document(id).delete().await()
        }
    }

    suspend fun deletePlayerById(id: String) {
        imageCollection.document(id).delete().await()
    }

    fun getAllImages(): Flow<List<ImageModel>> = callbackFlow {
        val subscription = imageCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val images = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ImageModel::class.java)?.copy(id = doc.id)
                }
                trySend(images)
            }
        }

        awaitClose { subscription.remove() }
    }

    fun getImagesSortedByDateAsc(): Flow<List<ImageModel>> = callbackFlow {
        val subscription = imageCollection.orderBy("imageDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val images = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ImageModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(images)
                }
            }

        awaitClose { subscription.remove() }
    }

    fun getImagesSortedByDateDesc(): Flow<List<ImageModel>> = callbackFlow {
        val subscription = imageCollection.orderBy("imageDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val images = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ImageModel::class.java)?.copy(id = doc.id)
                    }
                    trySend(images)
                }
            }

        awaitClose { subscription.remove() }
    }


}