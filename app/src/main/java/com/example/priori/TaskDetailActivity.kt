package com.example.priori

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.util.Calendar

class TaskDetailActivity : AppCompatActivity() {

    private var task: Task? = null
    private var selectedDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        val taskId = intent.getStringExtra("TASK_ID")
        task = MainActivity.taskList.find { it.id == taskId }

        if (task == null) {
            finish()
            return
        }

        val etName = findViewById<EditText>(R.id.etDetailName)
        val etSubject = findViewById<EditText>(R.id.etDetailSubject)
        val etDescription = findViewById<EditText>(R.id.etDetailDescription)
        val btnDate = findViewById<View>(R.id.btnDetailDate)
        val tvSelectedDate = findViewById<TextView>(R.id.tvDetailSelectedDate)
        
        val btnPending = findViewById<Button>(R.id.btnStatusPending)
        val btnInProgress = findViewById<Button>(R.id.btnStatusInProgress)
        val btnCompleted = findViewById<Button>(R.id.btnStatusCompleted)
        
        val btnUpdate = findViewById<Button>(R.id.btnUpdateTask)
        val btnDelete = findViewById<View>(R.id.btnDeleteTask)
        val btnBack = findViewById<View>(R.id.btnDetailBack)

        etName.setText(task!!.name)
        etSubject.setText(task!!.subject)
        etDescription.setText(task!!.description)
        selectedDate = task!!.dueDate
        tvSelectedDate.text = selectedDate.toString()

        var selectedStatus = task!!.status

        fun updateStatusButtonsUi() {
            val context = this
            val normalBg = R.drawable.bg_status_chip_unselected
            val selectedBg = R.drawable.bg_status_chip_selected
            val whiteColor = ContextCompat.getColor(context, R.color.white)

            listOf(btnPending, btnInProgress, btnCompleted).forEach { btn ->
                btn.backgroundTintList = null
            }

            btnPending.setBackgroundResource(if (selectedStatus == TaskStatus.PENDIENTE) selectedBg else normalBg)
            btnPending.setTextColor(whiteColor)

            btnInProgress.setBackgroundResource(if (selectedStatus == TaskStatus.EN_PROCESO) selectedBg else normalBg)
            btnInProgress.setTextColor(whiteColor)

            btnCompleted.setBackgroundResource(if (selectedStatus == TaskStatus.COMPLETADA) selectedBg else normalBg)
            btnCompleted.setTextColor(whiteColor)
        }

        updateStatusButtonsUi()

        btnPending.setOnClickListener {
            selectedStatus = TaskStatus.PENDIENTE
            updateStatusButtonsUi()
        }
        btnInProgress.setOnClickListener {
            selectedStatus = TaskStatus.EN_PROCESO
            updateStatusButtonsUi()
        }
        btnCompleted.setOnClickListener {
            selectedStatus = TaskStatus.COMPLETADA
            updateStatusButtonsUi()
        }

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                tvSelectedDate.text = selectedDate.toString()
            }, selectedDate!!.year, selectedDate!!.monthValue - 1, selectedDate!!.dayOfMonth).show()
        }

        btnUpdate.setOnClickListener {
            if (selectedStatus == TaskStatus.COMPLETADA && task!!.status != TaskStatus.COMPLETADA) {
                MainActivity.shouldShowCelebration = true
            }
            task!!.name = etName.text.toString()
            task!!.subject = etSubject.text.toString()
            task!!.description = etDescription.text.toString()
            task!!.dueDate = selectedDate!!
            task!!.status = selectedStatus
            TaskManager.saveTasks(this, MainActivity.taskList)
            finish()
        }

        btnDelete.setOnClickListener {
            MainActivity.taskList.remove(task)
            TaskManager.saveTasks(this, MainActivity.taskList)
            finish()
        }

        btnBack.setOnClickListener { finish() }
    }
}