package com.example.appseguimiento;



import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.example.appseguimiento.data.AppDatabase;
import com.example.appseguimiento.data.MediaItem;
import com.example.appseguimiento.data.MediaDao;
import com.example.appseguimiento.ui.adapter.MediaAdapter;
import com.example.appseguimiento.ui.dialog.EditarMediaDialog;
import com.example.appseguimiento.ui.dialog.NuevoMediaDialog;
import com.example.appseguimiento.utils.NotificacionesHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        NuevoMediaDialog.NuevoMediaDialogListener,
        EditarMediaDialog.EditarMediaDialogListener,
        MediaAdapter.OnItemClickListener {

    private RecyclerView rvMedia;
    private MediaAdapter adapter;
    private MediaDao dao;
    private AppDatabase db;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar RecyclerView
        rvMedia = findViewById(R.id.rvMedia);
        rvMedia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MediaAdapter(new java.util.ArrayList<MediaItem>(), this);
        rvMedia.setAdapter(adapter);

        // Inicializar base de datos y DAO
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
}
