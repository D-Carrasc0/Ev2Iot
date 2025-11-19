package com.example.agenda

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Todo(
    val id: String = "",
    val text: String = "",
    val completed: Boolean = false,
    val userId: String = "",
    @ServerTimestamp val createdAt: Date? = null
)