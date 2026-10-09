package com.example.priori

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TaskAdapter(
    private var tasks: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onDeleteClick: ((Task) -> Unit)? = null,
    private val onStatusChanged: ((Task) -> Unit)? = null
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val priorityBorder: View = view.findViewById(R.id.priorityBorder)
        val tvName: TextView = view.findViewById(R.id.tvTaskName)
        val tvPriorityLabel: TextView = view.findViewById(R.id.tvPriorityLabel)
        val tvSubject: TextView = view.findViewById(R.id.tvTaskSubject)
        val ivDateIcon: ImageView = view.findViewById(R.id.ivDateIcon)
        val tvDueDate: TextView = view.findViewById(R.id.tvTaskDueDate)
        val tvStatusLabel: TextView = view.findViewById(R.id.tvStatusLabel)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTaskItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val context = holder.itemView.context
        val task = tasks[position]

        holder.tvName.text = task.name
        holder.tvSubject.text = task.subject

        setupDateAndPriority(task, holder, context)
        setupStatusLabel(task, holder, context)

        if (task.status == TaskStatus.COMPLETADA) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                onDeleteClick?.invoke(task)
            }
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.btnDelete.setOnClickListener(null)
        }

        holder.tvStatusLabel.setOnClickListener {
            val nextStatus = when (task.status) {
                TaskStatus.PENDIENTE -> TaskStatus.EN_PROCESO
                TaskStatus.EN_PROCESO -> TaskStatus.COMPLETADA
                TaskStatus.COMPLETADA -> TaskStatus.PENDIENTE
            }
            task.status = nextStatus
            setupStatusLabel(task, holder, context)

            if (task.status == TaskStatus.COMPLETADA) {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnDelete.setOnClickListener {
                    onDeleteClick?.invoke(task)
                }
            } else {
                holder.btnDelete.visibility = View.GONE
                holder.btnDelete.setOnClickListener(null)
            }

            onStatusChanged?.invoke(task)
        }

        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    private fun setupDateAndPriority(task: Task, holder: TaskViewHolder, context: Context) {
        val today = LocalDate.now()
        val daysRemaining = ChronoUnit.DAYS.between(today, task.dueDate)

        val (priorityText, dateText, color) = when {
            daysRemaining < 0 -> Triple("Vencida", "Venció hace ${-daysRemaining} día(s)", ContextCompat.getColor(context, R.color.priority_overdue))
            daysRemaining == 0L -> Triple("Urgente", "Vence hoy", ContextCompat.getColor(context, R.color.priority_high))
            daysRemaining in 1..2 -> Triple("Urgente", "Vence en $daysRemaining días", ContextCompat.getColor(context, R.color.priority_high))
            daysRemaining in 3..5 -> Triple("Media", "Vence en $daysRemaining días", ContextCompat.getColor(context, R.color.priority_medium))
            else -> Triple("Baja", "Vence en $daysRemaining días", ContextCompat.getColor(context, R.color.priority_low))
        }

        holder.tvPriorityLabel.text = priorityText
        holder.tvPriorityLabel.setTextColor(color)
        holder.priorityBorder.setBackgroundColor(color)
        holder.ivDateIcon.imageTintList = ColorStateList.valueOf(color)
        holder.tvDueDate.text = dateText
        holder.tvDueDate.setTextColor(color)
    }

    private fun setupStatusLabel(task: Task, holder: TaskViewHolder, context: Context) {
        val (statusText, statusColor) = when (task.status) {
            TaskStatus.PENDIENTE -> "Pendiente" to ContextCompat.getColor(context, R.color.status_pending)
            TaskStatus.EN_PROCESO -> "En proceso" to ContextCompat.getColor(context, R.color.status_in_progress)
            TaskStatus.COMPLETADA -> "Completada" to ContextCompat.getColor(context, R.color.priority_low)
        }
        holder.tvStatusLabel.text = statusText
        holder.tvStatusLabel.setTextColor(statusColor)
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}