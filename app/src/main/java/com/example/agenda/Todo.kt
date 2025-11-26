package com.example.agenda

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Todo(
    val id: String = "",
    val text: String = "",
    val completed: Boolean = false,
    val userId: String = "",
    val dueAt: Date? = null,
    @ServerTimestamp val createdAt: Date? = null
)