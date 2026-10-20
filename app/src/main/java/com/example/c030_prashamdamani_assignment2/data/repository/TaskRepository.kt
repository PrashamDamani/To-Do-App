package com.example.c030_prashamdamani_assignment2.data.repository

import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Realtime Database access. Writes are applied to the local cache immediately and synced when
 * online, so they are NOT awaited (an await would hang while offline). Failures such as a
 * rules rejection are reported through [onError].
 */
class TaskRepository(private val db: FirebaseDatabase = FirebaseDatabase.getInstance()) {

    private fun tasksRef(uid: String): DatabaseReference = db.getReference("tasks").child(uid)

    fun newId(uid: String): String = tasksRef(uid).push().key ?: error("Could not generate task id")

    /** Live task list. The listener is removed when the collector is cancelled. */
    fun observeTasks(uid: String): Flow<Result<List<Task>>> = callbackFlow {
        val ref = tasksRef(uid)
        ref.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(Result.success(snapshot.children.mapNotNull { Task.fromSnapshot(it) }))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeConnection(): Flow<Boolean> = callbackFlow {
        val ref = db.getReference(".info/connected")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { trySend(s.getValue(Boolean::class.java) == true) }
            override fun onCancelled(e: DatabaseError) { /* ignore */ }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    fun observeCategories(uid: String): Flow<List<String>> = callbackFlow {
        val ref = db.getReference("users").child(uid).child("categories")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                trySend(s.children.mapNotNull { c -> try { c.getValue(String::class.java) } catch (e: Exception) { null } })
            }
            override fun onCancelled(e: DatabaseError) { trySend(emptyList()) }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    fun addCategory(uid: String, name: String, onError: (String) -> Unit) {
        db.getReference("users").child(uid).child("categories").push().setValue(name)
            .addOnFailureListener { onError(it.message ?: "Could not save category") }
    }

    fun create(uid: String, task: Task, onError: (String) -> Unit) {
        tasksRef(uid).child(task.id).setValue(task.toMap().filterValues { it != null })
            .addOnFailureListener { onError(it.message ?: "Write failed") }
    }

    /** Partial update via updateChildren. A null value removes that field. */
    fun update(uid: String, id: String, fields: Map<String, Any?>, onError: (String) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        tasksRef(uid).child(id).updateChildren(fields as Map<String, Any>)
            .addOnFailureListener { onError(it.message ?: "Update failed") }
    }

    fun delete(uid: String, id: String, onError: (String) -> Unit) {
        tasksRef(uid).child(id).removeValue()
            .addOnFailureListener { onError(it.message ?: "Delete failed") }
    }
}
