package com.example.priori

import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class TaskStatus { PENDIENTE, EN_PROCESO, COMPLETADA }
enum class TaskPriority { VENCIDA, URGENTE, MEDIA, BAJA }

data class Task(
    var id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var subject: String,
    var description: String,
    var dueDate: LocalDate,
    var status: TaskStatus = TaskStatus.PENDIENTE
) : Serializable {

    val priority: TaskPriority
        get() {
            val today = LocalDate.now()
            val daysRemaining = ChronoUnit.DAYS.between(today, dueDate)
            return when {
                daysRemaining < 0 -> TaskPriority.VENCIDA
                daysRemaining in 0..2 -> TaskPriority.URGENTE
                daysRemaining in 3..5 -> TaskPriority.MEDIA
                else -> TaskPriority.BAJA
            }
        }
}