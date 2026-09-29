package com.example.priori

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.util.Calendar

class AddTaskActivity : AppCompatActivity() {

    private var selectedDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val etName = findViewById<EditText>(R.id.etTaskName)
        val etSubject = findViewById<EditText>(R.id.etSubject)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val tvDate = findViewById<TextView>(R.id.tvSelectedDate)
        val btnDate = findViewById<android.view.View>(R.id.btnSelectDate)
        val btnSave = findViewById<Button>(R.id.btnSaveTask)
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                tvDate.text = "Fecha: $selectedDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val subject = etSubject.text.toString()
            val desc = etDescription.text.toString()

            if (name.isBlank() || subject.isBlank() || desc.isBlank() || selectedDate == null) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newTask = Task(name = name, subject = subject, description = desc, dueDate = selectedDate!!)
            MainActivity.taskList.add(newTask)
            TaskManager.saveTasks(this, MainActivity.taskList)
            finish()
        }

        btnBack.setOnClickListener { finish() }
    }
}