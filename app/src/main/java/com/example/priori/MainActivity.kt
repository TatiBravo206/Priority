package com.example.priori

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        var taskList = mutableListOf<Task>()
        var shouldShowCelebration = false
        var shouldShowTaskAdded = false
    }

    private lateinit var adapter: TaskAdapter
    private var currentFilter = "Pendientes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hideSystemUI()

        // CARGAR TAREAS
        taskList = TaskManager.loadTasks(this)

        val tvHeaderDate = findViewById<TextView>(R.id.tvHeaderDate)
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("'Hoy, 'EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
        tvHeaderDate.text = today.format(formatter).replaceFirstChar { it.uppercase() }

        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            tasks = taskList,
            onItemClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("TASK_ID", task.id)
                startActivity(intent)
            },
            onDeleteClick = { taskToDelete ->
                taskList.remove(taskToDelete)
                TaskManager.saveTasks(this, taskList)
                updateDashboardCounters()
                applyFilter()
            },
            onStatusChanged = { updatedTask ->
                TaskManager.saveTasks(this, taskList)
                updateDashboardCounters()
                applyFilter()

                if (updatedTask.status == TaskStatus.COMPLETADA) {
                    showCelebrationDialog()
                }
            }
        )
        rvTasks.adapter = adapter

        setupFilterButtons()

        findViewById<ImageButton>(R.id.fabAddTask).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()

        taskList = TaskManager.loadTasks(this)
        updateDashboardCounters()
        applyFilter()

        //  diálogo a mostrar según el evento
        if (shouldShowCelebration) {
            shouldShowCelebration = false
            showCelebrationDialog()
        } else if (shouldShowTaskAdded) {
            shouldShowTaskAdded = false
            showTaskAddedDialog()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun updateDashboardCounters() {
        val pendingCount = taskList.count { it.status != TaskStatus.COMPLETADA }
        val completedCount = taskList.count { it.status == TaskStatus.COMPLETADA }
        val overdueCount = taskList.count { it.priority == TaskPriority.VENCIDA && it.status != TaskStatus.COMPLETADA }

        findViewById<TextView>(R.id.tvCountPending).text = pendingCount.toString()
        findViewById<TextView>(R.id.tvCountCompleted).text = completedCount.toString()
        findViewById<TextView>(R.id.tvCountOverdue).text = overdueCount.toString()

        // AVATAR DINÁMICO
        val ivAvatarMood = findViewById<ImageView>(R.id.ivAvatarMood)

        when {
            overdueCount > 0 -> {
                ivAvatarMood.setImageResource(R.drawable.estresado)
            }
            pendingCount == 0 -> {
                ivAvatarMood.setImageResource(R.drawable.feli)
            }
            else -> {
                ivAvatarMood.setImageResource(R.drawable.pensante)
            }
        }
    }

    private fun setupFilterButtons() {
        val btnPending = findViewById<Button>(R.id.btnFilterPending)
        val btnAll = findViewById<Button>(R.id.btnFilterAll)
        val btnInProgress = findViewById<Button>(R.id.btnFilterInProgress)
        val btnCompleted = findViewById<Button>(R.id.btnFilterCompleted)
        val btnOverdue = findViewById<Button>(R.id.btnFilterOverdue)

        val buttons = listOf(btnPending, btnAll, btnInProgress, btnCompleted, btnOverdue)

        fun updateSelectedButton(selectedBtn: Button) {
            buttons.forEach { b ->
                b.backgroundTintList = null
                b.setBackgroundResource(R.drawable.bg_filter_chip)
                b.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_gray))
            }
            selectedBtn.backgroundTintList = null
            selectedBtn.setBackgroundResource(R.drawable.bg_filter_chip_selected)
            selectedBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        updateSelectedButton(btnPending)

        buttons.forEach { button ->
            button.setOnClickListener {
                updateSelectedButton(button)
                currentFilter = button.text.toString()
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "Pendientes" -> taskList.filter { it.status == TaskStatus.PENDIENTE }
            "Todas" -> taskList
            "En proceso" -> taskList.filter { it.status == TaskStatus.EN_PROCESO }
            "Completadas" -> taskList.filter { it.status == TaskStatus.COMPLETADA }
            "Vencidas" -> taskList.filter { it.priority == TaskPriority.VENCIDA }
            else -> taskList.filter { it.status == TaskStatus.PENDIENTE }
        }
        adapter.updateTasks(filtered)
    }

    // DIÁLOGO PARA CELEBRAR COMPLETAR TAREA
    fun showCelebrationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_celebration, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvCelebrationTitle)

        tvTitle?.text = "¡Aleluya! Una tarea menos"

        val celebrationImages = listOf(
            R.drawable.completo_1,
            R.drawable.completo_2,
            R.drawable.completo_3
        )

        val imgCelebration = dialogView.findViewById<ImageView>(R.id.imgCelebration)
        imgCelebration?.setImageResource(celebrationImages.random())

        //OSCURECER EL FONDO DETRÁS DEL DIÁLOGO
        alertDialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        alertDialog.window?.attributes?.dimAmount = 0.95f

        alertDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed && alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }, 2500)
    }

    // DIÁLOGO PARA TAREA AGREGADA CON ÉXITO
    fun showTaskAddedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_celebration, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvCelebrationTitle)

        tvTitle?.text = "¡Tienes una tarea nueva!"

        val taskAddedImages = listOf(
            R.drawable.agregada_1,
            R.drawable.agregada_2,
            R.drawable.agregada_3
        )

        val imgCelebration = dialogView.findViewById<ImageView>(R.id.imgCelebration)
        imgCelebration?.setImageResource(taskAddedImages.random())
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        //OSCURECER EL FONDO DETRÁS DEL DIÁLOGO
        alertDialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        alertDialog.window?.attributes?.dimAmount = 0.95f

        alertDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed && alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }, 2500)
    }
}