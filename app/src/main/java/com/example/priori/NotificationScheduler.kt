package com.example.priori

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationScheduler {

    fun scheduleTaskNotification(context: Context, task: Task) {
        if (task.status == TaskStatus.COMPLETADA) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Programar para el día de vencimiento a las 8:00 AM
        val notificationDateTime = LocalDateTime.of(task.dueDate, LocalTime.of(8, 0))
        val now = LocalDateTime.now()

        if (notificationDateTime.isBefore(now)) {
            // Si la hora de notificación ya pasó, no se programa
            return
        }

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_NAME", task.name)
            putExtra("TASK_SUBJECT", task.subject)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            flags
        )

        val triggerAtMillis = notificationDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelTaskNotification(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskNotificationReceiver::class.java)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            flags
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}