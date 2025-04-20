package com.example.appseguimiento;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class SeguimientoWidget extends AppWidgetProvider {

    private static final String COUNT_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/count_pending.php";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Obtener el conteo de pendientes desde el servidor
        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, COUNT_URL, null,
                response -> {
                    try {
                        int pendingCount = response.getInt("pending_count");
                        views.setTextViewText(R.id.tvPendingCount, "Pendientes: " + pendingCount);
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    } catch (Exception e) {
                        views.setTextViewText(R.id.tvPendingCount, "Error al cargar");
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                },
                error -> {
                    views.setTextViewText(R.id.tvPendingCount, "Error de red");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });

        queue.add(request);

        // Acción al hacer clic en el widget
        Intent intent = new Intent(context, SeguimientoWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tvPendingCount, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}