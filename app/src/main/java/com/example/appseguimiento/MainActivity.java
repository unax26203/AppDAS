package com.example.appseguimiento;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.example.appseguimiento.data.AppDatabase;
import com.example.appseguimiento.data.MediaItem;
import com.example.appseguimiento.data.MediaDao;
import com.example.appseguimiento.ui.adapter.MediaAdapter;
import com.example.appseguimiento.ui.dialog.EditarMediaDialog;
import com.example.appseguimiento.ui.dialog.NuevoMediaDialog;
import com.example.appseguimiento.utils.NotificacionesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        NuevoMediaDialog.NuevoMediaDialogListener,
        EditarMediaDialog.EditarMediaDialogListener,
        MediaAdapter.OnItemClickListener {

    private RecyclerView rvMedia;
    private MediaAdapter adapter;
    private MediaDao dao;
    private AppDatabase db;
    private FragmentManager fm;

    private String currentLanguage;
    private String currentTheme;

    private static final int EXPORT_REQUEST_CODE = 1;
    private static final int IMPORT_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentLanguage = prefs.getString("language_preference", "es");
        Locale locale = new Locale(currentLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        currentTheme = prefs.getString("theme_preference", "light");
        if (currentTheme.equals("dark")) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MediaAdapter(new ArrayList<>(), this);
        rvMedia.setAdapter(adapter);

        db = AppDatabase.getDatabase(this);
        dao = db.mediaDao();
        fm = getSupportFragmentManager();
        cargarDatos();

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(view -> {
            NuevoMediaDialog dialog = new NuevoMediaDialog();
            dialog.show(fm, "NuevoMediaDialog");
        });

        actualizarExtraInfo();

        //Obtener el token de FCM al iniciar la app
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d("FCM", "Token recibido: " + token);
                    Toast.makeText(this, "Token FCM copiado a logcat", Toast.LENGTH_LONG).show();
                    // Aquí puedes enviarlo a un servidor si lo necesitas
                });
    }

    private void cargarDatos() {
        AsyncTask.execute(() -> {
            List<MediaItem> lista = dao.getAllItems();
            runOnUiThread(() -> adapter.updateList(lista));
        });
    }

    private void exportDataToUri(Uri uri) {
        AsyncTask.execute(() -> {
            List<MediaItem> items = dao.getAllItems();
            StringBuilder sb = new StringBuilder();
            for (MediaItem item : items) {
                sb.append(item.getTitulo()).append(",");
                sb.append(item.getDescripcion()).append(",");
                sb.append(item.isCompleted() ? "1" : "0").append(",");
                sb.append(item.getTipo()).append("\n");
            }

            try {
                if (uri != null) {
                    FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri);
                    if (fos != null) {
                        fos.write(sb.toString().getBytes());
                        fos.close();
                    }
                }
                runOnUiThread(() -> Toast.makeText(this, "Datos exportados correctamente.", Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error exportando datos.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void exportDataUsingPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "media_export.txt");
        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }

    private void importDataFromUri(Uri uri) {
        AsyncTask.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        MediaItem item = new MediaItem(parts[0], parts[1], parts[2].equals("1"), parts[3]);
                        dao.insertItem(item);
                    }
                }
                bufferedReader.close();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Datos importados correctamente.", Toast.LENGTH_SHORT).show();
                    cargarDatos();
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error importando datos.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void importDataUsingPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, IMPORT_REQUEST_CODE);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
    public void onMediaAdded(String titulo, String descripcion, String tipo) {
        AsyncTask.execute(() -> {
            MediaItem nuevo = new MediaItem(titulo, descripcion, false, tipo);
            dao.insertItem(nuevo);
            NotificacionesHelper.mostrarNotificacion(this, "Nuevo ítem", "Añadido: " + titulo);
            runOnUiThread(this::cargarDatos);
        });
    }

    @Override
    public void onMediaUpdated(MediaItem item) {
        AsyncTask.execute(() -> {
            dao.updateItem(item);
            runOnUiThread(this::cargarDatos);
        });
    }

    @Override
    public void onMediaDeleted(MediaItem item) {
        AsyncTask.execute(() -> {
            dao.deleteItem(item);
            runOnUiThread(this::cargarDatos);
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
        } else if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            importDataFromUri(data.getData());
        }
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showExtraInfo = prefs.getBoolean("show_extra_info", true);
        TextView tvExtraInfo = findViewById(R.id.tvExtraInfo);
        if (tvExtraInfo != null) {
            tvExtraInfo.setVisibility(showExtraInfo ? View.VISIBLE : View.GONE);
        }
    }
}
