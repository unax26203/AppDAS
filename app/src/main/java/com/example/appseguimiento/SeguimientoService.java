package com.example.appseguimiento;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class SeguimientoService extends Service {

    public static final String CHANNEL_ID = "SeguimientoServiceChannel";
    public static final String ACTION_NEARBY_PLACE = "com.example.appseguimiento.NEARBY_PLACE";
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    private String lastNotifiedPlace = null;

    @Override
    public void onCreate() {
        super.onCreate();

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        verificarLugaresCercanos(location);
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoreo de ubicación")
                .setContentText("Buscando lugares cercanos...")
                .setSmallIcon(R.drawable.ic_notification)
                .build();

        startForeground(1, notification);

        iniciarMonitoreoUbicacion();

        return START_STICKY;
    }

    private void iniciarMonitoreoUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SeguimientoService", "Permisos de ubicación no concedidos");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000) // 30 segundos
                .setMinUpdateIntervalMillis(5000) // 5 segundos
                .build();

        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void verificarLugaresCercanos(Location location) {
        Log.d("SeguimientoService", "Buscando lugares del tipo: library");
        UsefulPlaces.buscarLugares(this, "library", location, (posicion, nombre) -> {
            Log.d("SeguimientoService", "Lugar encontrado (library): " + nombre);
            handleNotificationUpdate("Biblioteca", nombre);
            Intent intent = new Intent(ACTION_NEARBY_PLACE);
            intent.putExtra("tipo", "Biblioteca");
            intent.putExtra("nombre", nombre);
            sendBroadcast(intent);
        });

        Log.d("SeguimientoService", "Buscando lugares del tipo: movie_theater");
        UsefulPlaces.buscarLugares(this, "movie_theater", location, (posicion, nombre) -> {
            Log.d("SeguimientoService", "Lugar encontrado (movie_theater): " + nombre);
            handleNotificationUpdate("Cine", nombre);
            Intent intent = new Intent(ACTION_NEARBY_PLACE);
            intent.putExtra("tipo", "Cine");
            intent.putExtra("nombre", nombre);
            sendBroadcast(intent);
        });
    }

    private void handleNotificationUpdate(String tipo, String nombre) {
        String newPlace = tipo + ": " + nombre;

        // Solo reproducir sonido si el lugar/lugares ha(n) cambiado
        if (!newPlace.equals(lastNotifiedPlace)) {
            lastNotifiedPlace = newPlace;
            updateNotification("Lugar cercano: " + newPlace, true);
        } else {
            Log.d("SeguimientoService", "Lugar no ha cambiado, no se reproduce sonido.");
        }
    }


    private void updateNotification(String contentText, boolean playSound) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoreo de ubicación")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification);

        if (playSound) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL); // Sonido, vibración etc
        } else {
            builder.setDefaults(0); // Sin sonido ni vibración
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, builder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Seguimiento Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}