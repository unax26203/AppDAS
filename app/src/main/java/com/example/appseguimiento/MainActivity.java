package com.example.appseguimiento;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.appseguimiento.data.AppDatabase;
import com.example.appseguimiento.data.MediaItem;
import com.example.appseguimiento.data.MediaDao;
import com.example.appseguimiento.ui.adapter.MediaAdapter;
import com.example.appseguimiento.ui.dialog.EditarMediaDialog;
import com.example.appseguimiento.ui.dialog.NuevoMediaDialog;
import com.example.appseguimiento.utils.NotificacionesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements
        NuevoMediaDialog.NuevoMediaDialogListener,
        EditarMediaDialog.EditarMediaDialogListener,
        MediaAdapter.OnItemClickListener {

    private RecyclerView rvMedia;
    private MediaAdapter adapter;
    private FragmentManager fm;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> exportLauncher;

    private ActivityResultLauncher<Intent> importLauncher;

    private ActivityResultLauncher<String> locationPermissionLauncher;

    private ActivityResultLauncher<String> notificationPermissionLauncher;


    private String tokenFCM = "";

    private Button btnIniciarServicio;
    private Button btnDetenerServicio;




    private String currentLanguage;
    private String currentTheme;

    private static final int EXPORT_REQUEST_CODE = 1;
    private static final int IMPORT_REQUEST_CODE = 2;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        currentLanguage = prefs.getString("language_preference", "es");

        currentTheme = prefs.getString("theme_preference", "light");
        if ("dark".equals(currentTheme)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnIniciarServicio = findViewById(R.id.btnIniciarServicio);
        btnDetenerServicio = findViewById(R.id.btnDetenerServicio);

        // Inicializar el launcher para el permiso de notificaciones
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permiso de notificaciones concedido
                        checkAndRequestLocationPermission();
                        Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show();
                    } else {
                        // Permiso de notificaciones denegado
                        Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Comprobar si el permiso de notificaciones ya está concedido(para la tarea programada)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Inicializar el launcher para el permiso de ubicación
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Si el permiso de ubicación es concedido, iniciar el servicio
                        startLocationService();
                    } else {
                        // Si el permiso de ubicación es denegado
                        Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        // Programar el recordatorio
        ReminderScheduler.scheduleReminder(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MediaAdapter(new ArrayList<>(), this);
        rvMedia.setAdapter(adapter);

        fm = getSupportFragmentManager();
        cargarDatos();

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> {
            NuevoMediaDialog dialog = new NuevoMediaDialog();
            dialog.show(fm, "NuevoMediaDialog");
        });

        FloatingActionButton fabToggleButtons = findViewById(R.id.fabToggleButtons);
        LinearLayout btnGroup = findViewById(R.id.btnGroup);

        fabToggleButtons.setOnClickListener(v -> {
            if (btnGroup.getVisibility() == View.GONE) {
                btnGroup.setVisibility(View.VISIBLE);
            } else {
                btnGroup.setVisibility(View.GONE);
            }
        });

        actualizarExtraInfo();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    tokenFCM = task.getResult();
                    Log.d("FCM", "Token recibido: " + tokenFCM);
                });

        Button btnMostrarToken = findViewById(R.id.btnMostrarToken);
        btnMostrarToken.setOnClickListener(v -> {
            if (!tokenFCM.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Token FCM");
                builder.setMessage(tokenFCM);
                builder.setPositiveButton("Copy", (dialog, which) -> {
                    // Copiar el token al portapapeles
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("FCM Token", tokenFCM);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Token copiado al portapapeles", Toast.LENGTH_SHORT).show();
                });
                builder.setNegativeButton("Close", null);
                builder.show();
            } else {
                Toast.makeText(this, "Token aún no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        exportDataToUri(uri);
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        importDataFromUri(uri);
                    }
                });

        Button btnAbrirMapa = findViewById(R.id.btnAbrirMapa);
        btnAbrirMapa.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });

        btnIniciarServicio.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Pedir permiso de notificaciones
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Pedir permiso de ubicación
                checkAndRequestLocationPermission();
            }
        });

        btnDetenerServicio.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SeguimientoService.class);
            stopService(intent);
        });
    }

    private void cargarDatos() {
        executor.execute(() -> {
            List<MediaItem> lista = new ArrayList<>();

            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/get_media.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                StringBuilder responseText = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }

                reader.close();
                conn.disconnect();

                //DEBUG: ver la respuesta completa
                Log.d("HTTP_RESPONSE", "Respuesta recibida: " + responseText.toString());

                JSONObject json = new JSONObject(responseText.toString());
                if (json.has("items")) {
                    JSONArray items = json.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject obj = items.getJSONObject(i);
                        MediaItem item = new MediaItem(
                                obj.getString("titulo"),
                                obj.getString("descripcion"),
                                obj.getInt("is_completed") == 1,
                                obj.getString("tipo"),
                                obj.getString("imagen")
                        );
                        item.setId(obj.getInt("id"));
                        lista.add(item);
                    }
                } else {
                    Log.e("HTTP_RESPONSE", "No se encontraron items. Respuesta: " + json.toString());
                }

                runOnUiThread(() -> adapter.updateList(lista));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error cargando datos", Toast.LENGTH_SHORT).show());
            }
        });
    }



    private void exportDataToUri(Uri uri) {
        executor.execute(() -> {
            String url = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/get_media.php";
            RequestQueue queue = Volley.newRequestQueue(this);

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                sb.append(obj.getString("titulo")).append(",")
                                        .append(obj.getString("descripcion")).append(",")
                                        .append(obj.getInt("isCompleted")).append(",")
                                        .append(obj.getString("tipo")).append("\n");
                            }

                            if (uri != null) {
                                try (FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
                                    if (fos != null) fos.write(sb.toString().getBytes());
                                }
                            }
                            runOnUiThread(() -> Toast.makeText(this, "Datos exportados correctamente.", Toast.LENGTH_LONG).show());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> runOnUiThread(() -> Toast.makeText(this, "Error exportando datos.", Toast.LENGTH_SHORT).show()));

            queue.add(request);
        });
    }

    private void exportDataUsingPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "media_export.txt");
        exportLauncher.launch(intent);
    }

    private void importDataFromUri(Uri uri) {
        executor.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        String titulo = parts[0];
                        String descripcion = parts[1];
                        boolean isCompleted = parts[2].equals("1");
                        String tipo = parts[3];

                        JSONObject json = new JSONObject();
                        json.put("titulo", titulo);
                        json.put("descripcion", descripcion);
                        json.put("tipo", tipo);
                        json.put("isCompleted", isCompleted ? 1 : 0);

                        String url = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/insert_media.php";
                        RequestQueue queue = Volley.newRequestQueue(this);

                        String finalLine = line;
                        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                                response -> {
                                    // Opcional: puedes loguear cada inserción
                                },
                                error -> {
                                    Log.e("IMPORT", "Error insertando línea: " + finalLine, error);
                                });

                        queue.add(request);

                        Thread.sleep(100);
                    }
                }

                bufferedReader.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Importación completada", Toast.LENGTH_SHORT).show();
                    cargarDatos();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al importar datos", Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void importDataUsingPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        importLauncher.launch(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        String lang = prefs.getString("language_preference", "es");

        Locale newLocale = new Locale(lang);
        Locale.setDefault(newLocale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(newLocale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        String newTheme = prefs.getString("theme_preference", "light");
        String newLang = prefs.getString("language_preference", "es");

        if (!newLang.equals(currentLanguage)) {
            currentLanguage = newLang;
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            startActivity(intent);
        }

        if (!newTheme.equals(currentTheme)) {
            currentTheme = newTheme;
            recreate();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }

        actualizarExtraInfo();
    }


    @Override
    public void onMediaAdded(String titulo, String descripcion, String tipo, String nombreImagen) {
        executor.execute(() -> {
            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/insert_media.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("titulo", titulo);
                jsonBody.put("descripcion", descripcion);
                jsonBody.put("tipo", tipo);
                jsonBody.put("isCompleted", 0);
                jsonBody.put("imagen", nombreImagen); // Ya subido en el dialog

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("INSERT_JSON", "Respuesta: " + response);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Insertado correctamente", Toast.LENGTH_SHORT).show();
                    cargarDatos();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al insertar", Toast.LENGTH_SHORT).show());
            }
        });
    }



    @Override
    public void onMediaUpdated(MediaItem item, String nombreImagen) {
        executor.execute(() -> {
            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/update_media.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("id", item.getId());
                body.put("titulo", item.getTitulo());
                body.put("descripcion", item.getDescripcion());
                body.put("tipo", item.getTipo());
                body.put("isCompleted", item.isCompleted() ? 1 : 0);

                // Solo incluir imagen si se ha subido una nueva
                if (nombreImagen != null && !nombreImagen.isEmpty()) {
                    body.put("imagen", nombreImagen);
                }

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                InputStream is = (conn.getResponseCode() == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("UPDATE_MEDIA", "Respuesta: " + response);

                runOnUiThread(this::cargarDatos);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error actualizando", Toast.LENGTH_SHORT).show());
            }
        });
    }




    @Override
    public void onMediaDeleted(MediaItem item) {
        executor.execute(() -> {
            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/delete_media.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                // Enviar ID como JSON
                JSONObject json = new JSONObject();
                json.put("id", item.getId());

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                InputStream is = (conn.getResponseCode() == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("DELETE_MEDIA", "Respuesta: " + response);

                runOnUiThread(this::cargarDatos);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error eliminando", Toast.LENGTH_SHORT).show());
            }
        });
    }




    @Override
    public void onItemClick(MediaItem item) {
        EditarMediaDialog dialog = EditarMediaDialog.newInstance(item);
        dialog.show(fm, "EditarMediaDialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            exportDataToUri(data.getData());
        } /*else if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            importDataFromUri(data.getData());
        }*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_export) {
            exportDataUsingPicker();
            return true;
        } else if (id == R.id.action_import) {
            importDataUsingPicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void actualizarExtraInfo() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        boolean showExtraInfo = prefs.getBoolean("show_extra_info", true);
        TextView tvExtraInfo = findViewById(R.id.tvExtraInfo);
        if (tvExtraInfo != null) {
            tvExtraInfo.setVisibility(showExtraInfo ? View.VISIBLE : View.GONE);
        }
    }

    private void startLocationService() {
        Intent intent = new Intent(MainActivity.this, SeguimientoService.class);
        startForegroundService(intent);
    }

    private void checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startLocationService();
        }
    }


}
