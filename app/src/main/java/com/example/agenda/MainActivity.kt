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
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Referencias a Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas de la interfaz
    private lateinit var recyclerView: RecyclerView
    private lateinit var todoEditText: EditText
    private lateinit var dueDateEditText: EditText
    private lateinit var addButton: Button
    private lateinit var signOutButton: Button

    // Fecha seleccionada en el DatePicker (puede ser null si no se eligió)
    private var selectedDueDate: Calendar? = null

    // Adaptador del RecyclerView y listener de Firestore
    private lateinit var todoAdapter: TodoAdapter
    private var todosListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Si no hay usuario autenticado se envía a la pantalla de login
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

    // Obtiene las referencias a las vistas del layout
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        todoEditText = findViewById(R.id.todoEditText)
        dueDateEditText = findViewById(R.id.dueDateEditText)
        addButton = findViewById(R.id.addButton)
        signOutButton = findViewById(R.id.signOutButton)
    }

    // Configura el RecyclerView y el adaptador
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Callback para cuando se marca/desmarca una tarea como completada
        todoAdapter = TodoAdapter { todo, isChecked ->
            updateTodoCompleted(todo, isChecked)
        }

        // Editar texto de la tarea al pulsar sobre el texto
        todoAdapter.setOnEditClickListener { todo ->
            showEditDialog(todo)
        }

        // Eliminar tarea al pulsar el botón de eliminar
        todoAdapter.setOnDeleteClickListener { todo ->
            deleteTodo(todo)
        }

        recyclerView.adapter = todoAdapter
    }

    // Configura los listeners de los botones y del campo de fecha
    private fun setupClickListeners() {
        addButton.setOnClickListener { addNewTodo() }
        signOutButton.setOnClickListener { signOut() }
        dueDateEditText.setOnClickListener { showDatePicker() }
    }

    // Crea una nueva tarea y la guarda en Firestore
    private fun addNewTodo() {
        val todoText = todoEditText.text.toString().trim()
        if (todoText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa una tarea", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val dueDate = selectedDueDate?.time

        // Datos que se enviarán a Firestore
        val todo = hashMapOf(
            "text" to todoText,
            "completed" to false,
            "userId" to userId,
            "dueAt" to dueDate,
            "createdAt" to FieldValue.serverTimestamp()
        )

        Log.d(TAG, "Agregando nuevo todo: $todoText")

        db.collection("todos")
            .add(todo)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Todo agregado exitosamente con ID: ${documentReference.id}")

                // Limpiar campos y estado local
                todoEditText.text.clear()
                dueDateEditText.text.clear()
                selectedDueDate = null

                Toast.makeText(this, "Tarea agregada", Toast.LENGTH_SHORT).show()

                // Crear el objeto local para mostrarlo inmediatamente en la lista
                val localTodo = Todo(
                    id = documentReference.id,
                    text = todoText,
                    completed = false,
                    userId = userId,
                    dueAt = dueDate,
                    createdAt = null
                )
                todoAdapter.addTodo(localTodo)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al agregar todo", e)
                Toast.makeText(
                    this,
                    "Error al agregar tarea: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Actualiza el estado "completed" de una tarea en Firestore
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
                Toast.makeText(
                    this,
                    "Error al actualizar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Elimina una tarea de Firestore y del adaptador
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

                // Eliminar también de la lista local del RecyclerView
                todoAdapter.removeTodo(todo.id)
                Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar todo", e)
                Toast.makeText(
                    this,
                    "Error al eliminar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Muestra un DatePicker para seleccionar la fecha de entrega
    private fun showDatePicker() {
        val calendar = selectedDueDate ?: Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                // Se fija la hora al final del día (23:59)
                cal.set(year, month, dayOfMonth, 23, 59, 0)
                selectedDueDate = cal

                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dueDateEditText.setText(format.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    // Escucha los cambios en la colección de tareas del usuario y actualiza la lista
    private fun loadTodos() {
        val userId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Cargando todos para usuario: $userId")

        // Remover el listener anterior si existía, para evitar duplicados
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

                // Convertir los documentos de Firestore a objetos Todo
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
                    Toast.makeText(
                        this,
                        "No hay tareas. ¡Agrega una!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Diálogo para editar el texto de una tarea existente
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
                    Toast.makeText(
                        this,
                        "El texto no puede estar vacío",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    // Actualiza el texto de una tarea en Firestore y en el adaptador
    private fun updateTodoText(todo: Todo, newText: String) {
        db.collection("todos")
            .document(todo.id)
            .update("text", newText)
            .addOnSuccessListener {
                Log.d(TAG, "Tarea actualizada exitosamente")

                // Crear un nuevo objeto Todo con el texto actualizado
                val updatedTodo = todo.copy(text = newText)

                // Notificar al adaptador para que refresque el ítem
                todoAdapter.updateTodo(updatedTodo)

                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar tarea", e)
                Toast.makeText(
                    this,
                    "Error al actualizar tarea: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Cierra la sesión del usuario y limpia el listener
    private fun signOut() {
        Log.d(TAG, "Cerrando sesión")
        todosListener?.remove()
        auth.signOut()
        navigateToLogin()
    }

    // Navega a la pantalla de login y cierra esta actividad
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // Cuando la actividad se destruye, se elimina el listener de Firestore
    override fun onDestroy() {
        super.onDestroy()
        todosListener?.remove()
        todosListener = null
    }
}