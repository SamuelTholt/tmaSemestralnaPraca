package com.example.tmasemestralnapraca.admin

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val adminsCollection = firestore.collection("admins")

    suspend fun getAdminByEmail(email: String): AdminModel? {
        return try {
            val querySnapshot = adminsCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                val admin = document.toObject(AdminModel::class.java)
                admin?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertAdmin(admin: AdminModel) {
        try {
            val data = hashMapOf(
                "email" to admin.email,
                "password" to admin.password
            )
            if (admin.id != null) {
                adminsCollection.document(admin.id!!).set(data).await()
            } else {
                adminsCollection.add(data).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}