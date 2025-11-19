package com.example.agenda

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var todoEditText: EditText
    private lateinit var addButton: Button
    private lateinit var signOutButton: Button

    // UI/State
    private lateinit var todoAdapter: TodoAdapter
    private var todosListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verificar autenticación ANTES de inflar el layout si vas a navegar
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)
        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadTodos()

        Log.d(TAG, "Usuario autenticado: ${auth.currentUser?.email}")
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        todoEditText = findViewById(R.id.todoEditText)
        addButton = findViewById(R.id.addButton)
        signOutButton = findViewById(R.id.signOutButton)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        todoAdapter = TodoAdapter { todo, isChecked ->
            updateTodoCompleted(todo, isChecked)
        }

        // Configurar el listener para la edición de la tarea
        todoAdapter.setOnEditClickListener { todo ->
            showEditDialog(todo)
        }

        recyclerView.adapter = todoAdapter
        Log.d(TAG, "RecyclerView configurado")
    }

    private fun setupClickListeners() {
        addButton.setOnClickListener { addNewTodo() }
        signOutButton.setOnClickListener { signOut() }
    }

    private fun addNewTodo() {
        val todoText = todoEditText.text.toString().trim()
        if (todoText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa una tarea", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        val todo = hashMapOf(
            "text" to todoText,
            "completed" to false,
            "userId" to userId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        Log.d(TAG, "Agregando nuevo todo: $todoText")

        db.collection("todos")
            .add(todo)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Todo agregado exitosamente con ID: ${documentReference.id}")
                todoEditText.text.clear()
                Toast.makeText(this, "Tarea agregada", Toast.LENGTH_SHORT).show()
                val localTodo = Todo(
                    id = documentReference.id,
                    text = todoText,
                    completed = false,
                    userId = userId,
                    createdAt = null
                )
                todoAdapter.addTodo(localTodo)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al agregar todo", e)
                Toast.makeText(this, "Error al agregar tarea: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTodoCompleted(todo: Todo, isCompleted: Boolean) {
        if (todo.id.isEmpty()) {
            Log.e(TAG, "Error: ID del todo está vacío")
            return
        }

        Log.d(TAG, "Actualizando todo ${todo.id}: completed=$isCompleted")

        db.collection("todos")
            .document(todo.id)
            .update("completed", isCompleted)
            .addOnSuccessListener {
                Log.d(TAG, "Todo actualizado exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar todo", e)
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Cambia private por public o elimina 'private' para que sea accesible
    fun deleteTodo(todo: Todo) {
        if (todo.id.isEmpty()) {
            Log.e(TAG, "Error: ID del todo está vacío")
            return
        }

        Log.d(TAG, "Eliminando todo ${todo.id}")

        db.collection("todos")
            .document(todo.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Todo eliminado exitosamente")
                todoAdapter.removeTodo(todo.id)  // Eliminar también de la lista local del RecyclerView
                Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar todo", e)
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadTodos() {
        val userId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Cargando todos para usuario: $userId")

        // Remover listener anterior si existe
        todosListener?.remove()

        todosListener = db.collection("todos")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar cambios", error)
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.w(TAG, "Snapshots es null")
                    return@addSnapshotListener
                }

                val todoList = snapshots.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Todo::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al parsear documento ${doc.id}", e)
                        null
                    }
                }

                Log.d(TAG, "Todos cargados: ${todoList.size} items")
                todoAdapter.updateTodos(todoList)

                if (todoList.isEmpty()) {
                    Toast.makeText(this, "No hay tareas. ¡Agrega una!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showEditDialog(todo: Todo) {
        val editText = EditText(this)
        editText.setText(todo.text)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar tarea")
            .setView(editText)
            .setPositiveButton("Actualizar") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateTodoText(todo, newText)
                } else {
                    Toast.makeText(this, "El texto no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun updateTodoText(todo: Todo, newText: String) {
        db.collection("todos")
            .document(todo.id)
            .update("text", newText)
            .addOnSuccessListener {
                Log.d(TAG, "Tarea actualizada exitosamente")

                // Crear un nuevo objeto Todo con el nuevo texto
                val updatedTodo = todo.copy(text = newText)

                // Actualizar el adaptador con el nuevo objeto Todo
                todoAdapter.updateTodo(updatedTodo)

                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar tarea", e)
                Toast.makeText(this, "Error al actualizar tarea: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun signOut() {
        Log.d(TAG, "Cerrando sesión")
        todosListener?.remove()
        auth.signOut()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        todosListener?.remove()
        todosListener = null
    }
}
