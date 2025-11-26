package com.example.agenda

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

class TodoAdapter(
    private val onTodoChecked: (Todo, Boolean) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val todos = mutableListOf<Todo>()

    private var onEditClickListener: ((Todo) -> Unit)? = null
    private var onDeleteClickListener: ((Todo) -> Unit)? = null

    // ViewHolder que representa una fila de la lista
    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView as CardView
        val textView: TextView = itemView.findViewById(R.id.textView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    // Listener para editar
    fun setOnEditClickListener(onEditClick: (Todo) -> Unit) {
        this.onEditClickListener = onEditClick
    }

    // Listener para eliminar
    fun setOnDeleteClickListener(onDeleteClick: (Todo) -> Unit) {
        this.onDeleteClickListener = onDeleteClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val currentTodo = todos[position]

        holder.textView.text = currentTodo.text
        holder.checkBox.isChecked = currentTodo.completed

        // Tachar el texto si la tarea está completada
        if (currentTodo.completed) {
            holder.textView.paintFlags =
                holder.textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.textView.paintFlags =
                holder.textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        val context = holder.itemView.context

        // Color de la tarjeta:
        // - si está completada, color neutro
        // - si no está completada, se colorea según la fecha límite
        if (currentTodo.completed) {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.app_background)
            )
        } else {
            val dueDate = currentTodo.dueAt

            if (dueDate != null) {
                val now = System.currentTimeMillis()
                val diff = dueDate.time - now // milisegundos que faltan

                val color = when {
                    diff < 0 -> {
                        // Vencida: rojo
                        ContextCompat.getColor(context, android.R.color.holo_red_light)
                    }
                    diff < TimeUnit.HOURS.toMillis(24) -> {
                        // Menos de 24 horas: naranja
                        ContextCompat.getColor(context, android.R.color.holo_orange_light)
                    }
                    else -> {
                        // Falta más tiempo: blanco
                        ContextCompat.getColor(context, android.R.color.white)
                    }
                }
                holder.cardView.setCardBackgroundColor(color)
            } else {
                // Sin fecha: se deja en blanco
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
            }
        }

        // Editar al hacer clic en el texto
        holder.textView.setOnClickListener {
            onEditClickListener?.invoke(currentTodo)
        }

        // Evitar que se dispare el listener viejo al reciclar vistas
        holder.checkBox.setOnCheckedChangeListener(null)

        // Marcar/desmarcar como completada
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != currentTodo.completed) {
                Log.d("TodoAdapter", "Checkbox changed for ${currentTodo.text}: $isChecked")
                onTodoChecked(currentTodo, isChecked)
            }
        }

        // Eliminar tarea
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener?.invoke(currentTodo)
        }
    }

    override fun getItemCount(): Int = todos.size

    // Reemplaza toda la lista por una nueva
    fun updateTodos(newTodos: List<Todo>) {
        Log.d("TodoAdapter", "Updating todos. New count: ${newTodos.size}")
        todos.clear()
        todos.addAll(newTodos)
        notifyDataSetChanged()
    }

    // Agrega una tarea al principio de la lista
    fun addTodo(todo: Todo) {
        todos.add(0, todo)
        notifyItemInserted(0)
    }

    // Actualiza una tarea existente
    fun updateTodo(updatedTodo: Todo) {
        val index = todos.indexOfFirst { it.id == updatedTodo.id }
        if (index != -1) {
            todos[index] = updatedTodo
            notifyItemChanged(index)
        }
    }

    // Elimina una tarea por id
    fun removeTodo(todoId: String) {
        val index = todos.indexOfFirst { it.id == todoId }
        if (index != -1) {
            todos.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}