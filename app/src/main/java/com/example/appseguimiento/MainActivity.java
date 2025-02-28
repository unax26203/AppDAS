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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
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

    private void exportDataToFile() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Obtener la lista de items desde la base de datos
                final List<MediaItem> items = dao.getAllItems();
                StringBuilder sb = new StringBuilder();
                // Construir una línea para cada MediaItem, usando coma (,) como delimitador.
                // Por ejemplo: titulo,descripcion,estado,tipo
                for (MediaItem item : items) {
                    sb.append(item.getTitulo()).append(",");
                    sb.append(item.getDescripcion()).append(",");
                    // Usamos "1" para terminado y "0" para pendiente
                    sb.append(item.isCompleted() ? "1" : "0").append(",");
                    sb.append(item.getTipo());  // Asegúrate de que MediaItem tenga este campo si lo has añadido.
                    sb.append("\n");
                }
                String data = sb.toString();

                // Elegir una ubicación para el archivo. Aquí usamos el directorio de archivos externos privados.
                File file = new File(getExternalFilesDir(null), "media_export.txt");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    // Opcional: mostrar un mensaje de error en la UI.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error exportando datos", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                // Mostrar un mensaje en la UI indicando que se exportó correctamente.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Datos exportados a: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }


    private void importDataFromFile() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Ubicación del archivo que exportamos anteriormente
                File file = new File(getExternalFilesDir(null), "media_export.txt");
                if (!file.exists()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Separamos la línea por comas
                        String[] parts = line.split(",");
                        if (parts.length >= 4) {
                            String titulo = parts[0];
                            String descripcion = parts[1];
                            boolean isCompleted = parts[2].equals("1");
                            String tipo = parts[3];
                            MediaItem item = new MediaItem(titulo, descripcion, isCompleted, tipo);
                            dao.insertItem(item);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error importando datos", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Datos importados", Toast.LENGTH_SHORT).show();
                        cargarDatos();  // Actualiza la lista con los datos importados
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }
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
    public void onMediaAdded(final String titulo, final String descripcion, final String tipo) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Suponiendo que has actualizado MediaItem para incluir el tipo
                MediaItem nuevo = new MediaItem(titulo, descripcion, false, tipo);
                dao.insertItem(nuevo);
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
        int id = item.getItemId();
        if (id == R.id.action_cambiar_idioma) {
            mostrarDialogoCambiarIdioma();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_export) {
            exportDataToFile();
            return true;
        } else if (id == R.id.action_import) {
            importDataFromFile();
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