package com.example.appseguimiento.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.example.appseguimiento.R;
import com.example.appseguimiento.data.MediaItem;

public class EditarMediaDialog extends DialogFragment {

    public interface EditarMediaDialogListener {
        void onMediaUpdated(MediaItem item);
        void onMediaDeleted(MediaItem item);
    }

    private static final String ARG_ID = "id";
    private static final String ARG_TITULO = "titulo";
    private static final String ARG_DESCRIPCION = "descripcion";
    private static final String ARG_ESTADO = "estado";

    // Nuevo campo para diferenciar: "libro" o "pelicula"
    private static final String ARG_TIPO = "tipo";

    private EditarMediaDialogListener listener;

    public static EditarMediaDialog newInstance(MediaItem item) {
        EditarMediaDialog fragment = new EditarMediaDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, item.getId());
        args.putString(ARG_TITULO, item.getTitulo());
        args.putString(ARG_DESCRIPCION, item.getDescripcion());
        args.putBoolean(ARG_ESTADO, item.isCompleted());
        args.putString(ARG_TIPO, item.getTipo());
        fragment.setArguments(args);
        return fragment;
    }
    // Crear el diálogo
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            listener = (EditarMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar EditarMediaDialogListener");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Utilizamos getString para el título y concatenamos el título del elemento a editar.
        builder.setTitle(getString(R.string.dialog_edit_title) + getArguments().getString(ARG_TITULO));

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_editar_media, null);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        final Switch switchEstado = view.findViewById(R.id.switchEstado);
        Button btnShare = view.findViewById(R.id.btnShare);

        etDescripcion.setText(getArguments().getString(ARG_DESCRIPCION));
        switchEstado.setChecked(getArguments().getBoolean(ARG_ESTADO));

        // Compartir el contenido del elemento
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Construir el mensaje a compartir
                String shareText = "Título: " + getArguments().getString(ARG_TITULO) + "\n" +
                        "Descripción: " + getArguments().getString(ARG_DESCRIPCION) + "\n" +
                        "Estado: " + (getArguments().getBoolean(ARG_ESTADO) ? "Terminada" : "Pendiente");

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
            }
        });
        // Crear el diálogo con los botones
        builder.setView(view)
                .setPositiveButton(getString(R.string.dialog_edit_positive), (dialog, id) -> {
                    int idItem = getArguments().getInt(ARG_ID);
                    String nuevaDesc = etDescripcion.getText().toString();
                    boolean nuevoEstado = switchEstado.isChecked();
                    MediaItem item = new MediaItem(getArguments().getString(ARG_TITULO), nuevaDesc, nuevoEstado, getArguments().getString(ARG_TIPO));
                    item.setId(idItem);
                    listener.onMediaUpdated(item);
                })
                .setNegativeButton(getString(R.string.dialog_edit_negative), (dialog, id) -> dialog.cancel())
                .setNeutralButton(getString(R.string.dialog_edit_neutral), (dialog, id) -> {
                    int idItem = getArguments().getInt(ARG_ID);
                    MediaItem item = new MediaItem(getArguments().getString(ARG_TITULO), getArguments().getString(ARG_DESCRIPCION),
                            getArguments().getBoolean(ARG_ESTADO), getArguments().getString(ARG_TIPO));
                    item.setId(idItem);
                    listener.onMediaDeleted(item);
                });

        return builder.create();
    }
}
