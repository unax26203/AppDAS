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
import android.net.Uri;
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

    // Códigos de solicitud para exportar e importar
    private static final int EXPORT_REQUEST_CODE = 1;
    private static final int IMPORT_REQUEST_CODE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar preferencias de idioma y tema
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentLanguage = prefs.getString("language_preference", "es");
        Locale locale = new Locale(currentLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        currentTheme = prefs.getString("theme_preference", "light");
        if (currentTheme.equals("dark")) {
            setTheme(R.style.AppTheme_Dark); // Asegúrate de que este estilo esté definido en styles.xml
        } else {
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState);

        // Inflar el layout
        setContentView(R.layout.activity_main);

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
        // Configurar el FAB para agregar nuevos ítems
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


    // Función para cargar los datos de la base de datos en el RecyclerView
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
    // Función para exportar los datos a un archivo de texto
    private void exportDataToUri(Uri uri) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<MediaItem> items = dao.getAllItems();
                StringBuilder sb = new StringBuilder();
                for (MediaItem item : items) {
                    sb.append(item.getTitulo()).append(",");
                    sb.append(item.getDescripcion()).append(",");
                    sb.append(item.isCompleted() ? "1" : "0").append(",");
                    sb.append(item.getTipo());
                    sb.append("\n");
                }
                String data = sb.toString();

                try {
                    // Usa ContentResolver para abrir un OutputStream con el URI
                    if (uri != null) {
                        FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri);
                        if (fos != null) {
                            fos.write(data.getBytes());
                            fos.close();
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Datos exportados correctamente.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error exportando datos.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    private void exportDataUsingPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "media_export.txt"); // Nombre sugerido
        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }
    // Función para importar datos desde un archivo de texto
    private void importDataFromUri(Uri uri) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Lee el archivo línea por línea y agrega los elementos a la base de datos
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
                            MediaItem item = new MediaItem(titulo, descripcion, isCompleted, tipo);
                            dao.insertItem(item);
                        }
                    }
                    bufferedReader.close();
                    runOnUiThread(new Runnable() {
                        // Actualiza la UI después de importar los datos
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Datos importados correctamente.", Toast.LENGTH_SHORT).show();
                            cargarDatos();
                        }
                    });
                } catch (IOException e) {
                    // Maneja cualquier error de lectura del archivo
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error importando datos.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }



    private void importDataUsingPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, IMPORT_REQUEST_CODE);
    }

    // Para cambiar el idioma de la app
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        // Usamos "language_preference" aquí
        String lang = prefs.getString("language_preference", "es");
        Locale newLocale = new Locale(lang);
        Locale.setDefault(newLocale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(newLocale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }


    // Para actualizar el idioma y el tema de la app
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String newTheme = prefs.getString("theme_preference", "light");
        String newLang = prefs.getString("language_preference", "es");
        if (!newLang.equals(currentLanguage)) {
            // Actualiza la variable y reinicia la Activity completamente
            currentLanguage = newLang;
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            startActivity(intent);
        }
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
    // Callback para borrar un ítem
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
    // Para mostrar el menú de opciones en el Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                exportDataToUri(uri);
            }
        } else if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                importDataFromUri(uri);
            }
        }
    }

    // Para manejar las acciones del menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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

    // función para actualizar la visibilidad de la información extra
    private void actualizarExtraInfo() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showExtraInfo = prefs.getBoolean("show_extra_info", true);
        // Supongamos que tienes un TextView con id tvExtraInfo
        TextView tvExtraInfo = findViewById(R.id.tvExtraInfo);
        if (tvExtraInfo != null) {
            tvExtraInfo.setVisibility(showExtraInfo ? View.VISIBLE : View.GONE);
        }
    }
}