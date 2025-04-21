package com.example.appseguimiento;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class ReminderScheduler {
        public static void scheduleReminder(Context context) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            // Configurar la alarma para que se dispare cada 5 minutos
            long interval = 5 * 60 * 1000; // 5 minutos en milisegundos

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.SECOND, 0);

            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        interval,
                        pendingIntent
                );
            }
        }
}