package com.example.appseguimiento;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.os.Handler;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "Mensaje recibido");

        String titulo = "Nuevo mensaje";
        String cuerpo = "Has recibido una nueva notificación";
        String mensaje = "";
        String fecha = "";

        if (remoteMessage.getNotification() != null) {
            // Si es notificación
            cuerpo = remoteMessage.getNotification().getBody();
            titulo = remoteMessage.getNotification().getTitle();
            Log.d("FCM", "Notificación: " + cuerpo);
        }

        if (!remoteMessage.getData().isEmpty()) {
            mensaje = remoteMessage.getData().get("mensaje");
            fecha = remoteMessage.getData().get("fecha");
            Log.d("FCM", "Datos: " + remoteMessage.getData());
        }

        // Mostrar notificación
        mostrarNotificacion(titulo, mensaje + " (" + fecha + ")");
    }

    private void mostrarNotificacion(String titulo, String cuerpo) {
        String canalId = "canal_fcm";

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8+: crear canal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalId,
                    "Canal FCM",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            canal.setDescription("Canal para notificaciones FCM");
            notificationManager.createNotificationChannel(canal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}