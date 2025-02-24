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
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.example.appseguimiento.data.AppDatabase;
import com.example.appseguimiento.data.MediaItem;
import com.example.appseguimiento.data.MediaDao;
import com.example.appseguimiento.ui.adapter.MediaAdapter;
import com.example.appseguimiento.ui.dialog.EditarMediaDialog;
import com.example.appseguimiento.ui.dialog.NuevoMediaDialog;
import com.example.appseguimiento.utils.NotificacionesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentTheme = prefs.getString("theme_preference", "light");
        if (currentTheme.equals("dark")) {
            setTheme(R.style.AppTheme_Dark); // Asegúrate de que este estilo esté definido en styles.xml
        } else {
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState);

        // Inflar el layout
        setContentView(R.layout.activity_main);

        // Leer la preferencia del idioma y aplicar la configuración
        String lang = prefs.getString("app_language", "es");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Configurar el Toolbar personalizado
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Resto de la inicialización (RecyclerView, FloatingActionButton, etc.)
        rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MediaAdapter(new ArrayList<MediaItem>(), this);
        rvMedia.setAdapter(adapter);

        db = AppDatabase.getDatabase(this);
        dao = db.mediaDao();
        fm = getSupportFragmentManager();
        cargarDatos();

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NuevoMediaDialog dialog = new NuevoMediaDialog();
                dialog.show(fm, "NuevoMediaDialog");
            }
        });
        actualizarExtraInfo();
    }


    // Método para cargar datos en background
    private void cargarDatos() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<MediaItem> lista = dao.getAllItems();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.updateList(lista);
                    }
                });
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String newTheme = prefs.getString("theme_preference", "light");
        if (!newTheme.equals(currentTheme)) {
            // Actualiza currentTheme y recrea la actividad para aplicar el nuevo tema
            currentTheme = newTheme;
            recreate();
        }
        // También puedes actualizar otros elementos, por ejemplo, la visibilidad de la info extra:
        actualizarExtraInfo();
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String lang = prefs.getString("app_language", "es"); // Español por defecto
        Locale newLocale = new Locale(lang);
        Locale.setDefault(newLocale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(newLocale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }


    // Callback desde NuevoMediaDialog (para agregar nuevo ítem)
    @Override
    public void onMediaAdded(final String titulo, final String descripcion) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                MediaItem nuevo = new MediaItem(titulo, descripcion, false);
                dao.insertItem(nuevo);
                // Mostrar notificación
                NotificacionesHelper.mostrarNotificacion(MainActivity.this, "Nuevo ítem", "Añadido: " + titulo);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cargarDatos();
                    }
                });
            }
        });
    }

    // Callback desde EditarMediaDialog (para actualizar o borrar)
    @Override
    public void onMediaUpdated(final MediaItem item) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                dao.updateItem(item);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cargarDatos();
                    }
                });
            }
        });
    }

    @Override
    public void onMediaDeleted(final MediaItem item) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                dao.deleteItem(item);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cargarDatos();
                    }
                });
            }
        });
    }

    // Al pulsar sobre un ítem del RecyclerView, abrimos el diálogo de edición
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cambiar_idioma) {
            mostrarDialogoCambiarIdioma();
            return true;
        }else if(item.getItemId() == R.id.action_settings){
            // Lanzar la actividad de preferencias
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void actualizarExtraInfo() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showExtraInfo = prefs.getBoolean("show_extra_info", true);
        // Supongamos que tienes un TextView con id tvExtraInfo
        TextView tvExtraInfo = findViewById(R.id.tvExtraInfo);
        if (tvExtraInfo != null) {
            tvExtraInfo.setVisibility(showExtraInfo ? View.VISIBLE : View.GONE);
        }
    }

    private void cambiarIdioma(String lang) {
        // Crea un nuevo Locale con el código (es, en, eu)
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        // Actualiza la configuración de recursos
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // (Opcional) Guarda la preferencia para que el idioma se mantenga al reiniciar la app.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("app_language", lang);
        editor.apply();

        // Reinicia la Activity para aplicar el nuevo idioma
        recreate();
    }


    private void mostrarDialogoCambiarIdioma() {
        // Opciones a mostrar
        final String[] idiomas = {"Español", "English", "Euskera"};

        // Usa la cadena 'select_language' para el título (se traducirá según el idioma actual)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_language));

        builder.setItems(idiomas, (dialog, which) -> {
            String idiomaSeleccionado;
            if (which == 0) {
                idiomaSeleccionado = "es";
            } else if (which == 1) {
                idiomaSeleccionado = "en";
            } else { // which == 2
                idiomaSeleccionado = "eu";
            }
            cambiarIdioma(idiomaSeleccionado);
        });
        builder.create().show();
    }

}
