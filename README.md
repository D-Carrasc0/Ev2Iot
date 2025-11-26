# Agenda Estudiantil 📚

Aplicación móvil Android desarrollada en **Kotlin** que permite a estudiantes organizar sus tareas académicas, registrando actividades con fecha de entrega, marcándolas como completadas y gestionando su lista de pendientes.

La app utiliza **Firebase Authentication** para el manejo de usuarios y **Cloud Firestore** para almacenar las tareas.

---

## 🎯 Objetivo

Ayudar a los estudiantes a **no olvidar** fechas de entrega y evaluaciones, ofreciendo:

- Un lugar centralizado para registrar tareas.
- Visualización clara de qué es más urgente.
- Posibilidad de marcar tareas como completadas, editar o eliminarlas.

---

## ✨ Funcionalidades principales

- **Registro e inicio de sesión de usuarios**
  - Autenticación por correo y contraseña (Firebase Auth).
  - Validación de formato de correo y longitud mínima de contraseña.
  - Si ya hay una sesión activa, se salta la pantalla de login.

![image alt](https://github.com/D-Carrasc0/Ev2Iot/blob/36d85a724a2d1d6a2f02f612ca2d03b230d200d6/images/pantalla%20principal.PNG)

- **Gestión de tareas**
  ![image alt](https://github.com/D-Carrasc0/Ev2Iot/blob/36d85a724a2d1d6a2f02f612ca2d03b230d200d6/images/lista%20tareas.PNG)
  - Crear nuevas tareas con:
    - Descripción de la tarea.
    - Fecha de entrega (seleccionada con un `DatePicker`).
  ![image alt](https://github.com/D-Carrasc0/Ev2Iot/blob/36d85a724a2d1d6a2f02f612ca2d03b230d200d6/images/fecha.PNG)
  - Listado de tareas en un `RecyclerView`, filtradas por usuario.
  - Marcar tareas como **completadas** mediante un `CheckBox`.
  - Editar el texto de una tarea mediante un diálogo.
![image alt](https://github.com/D-Carrasc0/Ev2Iot/blob/36d85a724a2d1d6a2f02f612ca2d03b230d200d6/images/editar.PNG)
  - Eliminar tareas definitivamente.

- **Indicador visual de urgencia**
  - El color de la tarjeta cambia según la fecha de entrega:
    - **Rojo**: tarea vencida.
    - **Naranja**: falta menos de 24 horas.
    - **Blanco**: aún hay tiempo.
    - **Color de fondo neutro**: tarea completada.

- **Sesión y seguridad básica**
  - Cada usuario solo ve sus propias tareas (se filtra por `userId`).
  - Opción de **cerrar sesión** desde la pantalla principal.
  - Manejo de errores con mensajes claros al usuario (Toasts).

---

## 🧱 Arquitectura general

La aplicación está organizada en las siguientes clases principales:

### Activities

- **`LoginActivity`**
  - Maneja el flujo de autenticación (login y registro).
  - Valida correo y contraseña antes de llamar a Firebase.
  - Si hay un usuario autenticado en `onStart`, navega directamente a `MainActivity`.

- **`MainActivity`**
  - Muestra la lista de tareas del usuario autenticado.
  - Permite crear nuevas tareas, editar, marcar como completadas y eliminar.
  - Incluye selección de fecha de entrega con `DatePickerDialog`.
  - Se suscribe a cambios en Firestore usando un `addSnapshotListener`.

### Modelo de datos

- **`Todo`**
  - Representa una tarea almacenada en Firestore.
  - Campos:
    - `id: String` – identificador del documento en Firestore.
    - `text: String` – descripción de la tarea.
    - `completed: Boolean` – indica si la tarea está completada.
    - `userId: String` – usuario dueño de la tarea.
    - `dueAt: Date?` – fecha de entrega.
    - `createdAt: Date?` – fecha de creación (timestamp de servidor).

### Adaptador de lista

- **`TodoAdapter`**
  - Adaptador de `RecyclerView` que muestra cada `Todo` en un `CardView`.
  - Se encarga de:
    - Mostrar el texto y el estado del `CheckBox`.
    - Tachar el texto cuando la tarea está completada.
    - Cambiar el color de fondo según la fecha y el estado.
    - Invocar callbacks para:
      - Marcar como completado.
      - Editar tarea (clic en el texto).
      - Eliminar tarea (botón “Eliminar”).

---
