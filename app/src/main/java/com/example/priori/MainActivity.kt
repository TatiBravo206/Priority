package com.example.priori

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
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
    private var selectedFilterId = R.id.btnFilterPending

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hideSystemUI()

        // Crear canal de notificaciones y solicitar permisos
        createNotificationChannel()
        requestNotificationPermission()

        // CARGAR TAREAS
        taskList = TaskManager.loadTasks(this)

        updateHeaderDate()

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
                NotificationScheduler.cancelTaskNotification(this, taskToDelete)
                taskList.remove(taskToDelete)
                TaskManager.saveTasks(this, taskList)
                updateDashboardCounters()
                applyFilter()
            },
            onStatusChanged = { updatedTask ->
                if (updatedTask.status == TaskStatus.COMPLETADA) {
                    NotificationScheduler.cancelTaskNotification(this, updatedTask)
                } else {
                    NotificationScheduler.scheduleTaskNotification(this, updatedTask)
                }

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

        findViewById<Button>(R.id.btnChangeLanguage).setOnClickListener {
            toggleLanguage()
        }

        findViewById<ImageButton>(R.id.fabAddTask).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()

        updateHeaderDate()
        taskList = TaskManager.loadTasks(this)
        updateDashboardCounters()
        applyFilter()

        if (shouldShowCelebration) {
            shouldShowCelebration = false
            showCelebrationDialog()
        } else if (shouldShowTaskAdded) {
            shouldShowTaskAdded = false
            showTaskAddedDialog()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "PRIORI_REMINDERS"
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun updateHeaderDate() {
        val tvHeaderDate = findViewById<TextView>(R.id.tvHeaderDate)
        val today = LocalDate.now()
        val currentLocales = LocaleListCompat.getDefault()
        val currentLocale = if (!currentLocales.isEmpty) currentLocales[0] ?: Locale.getDefault() else Locale.getDefault()
        val pattern = if (currentLocale.language == "en") "'Today, 'EEEE, MMMM d" else "'Hoy, 'EEEE, d 'de' MMMM"
        val formatter = DateTimeFormatter.ofPattern(pattern, currentLocale)
        tvHeaderDate.text = today.format(formatter).replaceFirstChar { it.uppercase() }
    }

    private fun toggleLanguage() {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!currentLocales.isEmpty) currentLocales[0]?.language else Locale.getDefault().language
        val newLang = if (currentLang == "en") "es" else "en"

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
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

        val defaultSelectedBtn = findViewById<Button>(selectedFilterId) ?: btnPending
        updateSelectedButton(defaultSelectedBtn)

        buttons.forEach { button ->
            button.setOnClickListener {
                updateSelectedButton(button)
                selectedFilterId = button.id
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (selectedFilterId) {
            R.id.btnFilterPending -> taskList.filter { it.status == TaskStatus.PENDIENTE }
            R.id.btnFilterAll -> taskList
            R.id.btnFilterInProgress -> taskList.filter { it.status == TaskStatus.EN_PROCESO }
            R.id.btnFilterCompleted -> taskList.filter { it.status == TaskStatus.COMPLETADA }
            R.id.btnFilterOverdue -> taskList.filter { it.priority == TaskPriority.VENCIDA }
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
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvCelebrationSubtitle)

        tvTitle?.text = getString(R.string.celebration_title)
        tvSubtitle?.text = getString(R.string.celebration_subtitle)

        val celebrationImages = listOf(
            R.drawable.completo_1,
            R.drawable.completo_2,
            R.drawable.completo_3
        )

        val imgCelebration = dialogView.findViewById<ImageView>(R.id.imgCelebration)
        imgCelebration?.setImageResource(celebrationImages.random())

        alertDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
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
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvCelebrationSubtitle)

        tvTitle?.text = getString(R.string.task_added_title)
        tvSubtitle?.visibility = View.GONE

        val taskAddedImages = listOf(
            R.drawable.agregada_1,
            R.drawable.agregada_2,
            R.drawable.agregada_3
        )

        val imgCelebration = dialogView.findViewById<ImageView>(R.id.imgCelebration)
        imgCelebration?.setImageResource(taskAddedImages.random())

        alertDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        alertDialog.window?.attributes?.dimAmount = 0.95f

        alertDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed && alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }, 2500)
    }
}