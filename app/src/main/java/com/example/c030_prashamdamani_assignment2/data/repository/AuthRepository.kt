package com.example.c030_prashamdamani_assignment2.data.repository

import com.example.c030_prashamdamani_assignment2.data.model.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    suspend fun register(name: String, roll: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Account created but no user returned")
        // Only non-secret profile data is stored. Passwords live in Firebase Auth only.
        val profile = mapOf(
            "fullName" to name,
            "email" to email,
            "rollNumber" to roll,
            "createdAt" to System.currentTimeMillis()
        )
        db.getReference("users").child(user.uid).setValue(profile)
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun sendReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun logout() = auth.signOut()

    fun updateProfile(user: FirebaseUser, name: String, roll: String) {
        db.getReference("users").child(user.uid)
            .updateChildren(mapOf<String, Any>("fullName" to name, "rollNumber" to roll, "email" to (user.email ?: "")))
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
    }

    fun observeProfile(user: FirebaseUser): Flow<UserProfile> = callbackFlow {
        val ref = db.getReference("users").child(user.uid)
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val name = try { s.child("fullName").getValue(String::class.java) } catch (e: Exception) { null }
                val roll = try { s.child("rollNumber").getValue(String::class.java) } catch (e: Exception) { null }
                trySend(fallbackProfile(user).copy(
                    fullName = name?.takeIf { it.isNotBlank() } ?: fallbackProfile(user).fullName,
                    rollNumber = roll.orEmpty()
                ))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(fallbackProfile(user))
            }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    companion object {
        fun fallbackProfile(user: FirebaseUser) = UserProfile(
            uid = user.uid,
            fullName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "User",
            email = user.email.orEmpty(),
            rollNumber = "",
            createdAt = user.metadata?.creationTimestamp ?: 0L
        )

        fun friendlyError(e: Throwable): String = when (e) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthInvalidUserException -> "Invalid email or password."
            is FirebaseNetworkException -> "No network connection. Check your internet and try again."
            else -> e.localizedMessage ?: "Something went wrong. Please try again."
        }
    }
}
