package com.example.appseguimiento.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
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

    private EditarMediaDialogListener listener;

    public static EditarMediaDialog newInstance(MediaItem item) {
        EditarMediaDialog fragment = new EditarMediaDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, item.getId());
        args.putString(ARG_TITULO, item.getTitulo());
        args.putString(ARG_DESCRIPCION, item.getDescripcion());
        args.putBoolean(ARG_ESTADO, item.isCompleted());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            listener = (EditarMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar EditarMediaDialogListener");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Editar: " + getArguments().getString(ARG_TITULO));

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_editar_media, null);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        final Switch switchEstado = view.findViewById(R.id.switchEstado);

        etDescripcion.setText(getArguments().getString(ARG_DESCRIPCION));
        switchEstado.setChecked(getArguments().getBoolean(ARG_ESTADO));

        builder.setView(view)
                .setPositiveButton("Actualizar", (dialog, id) -> {
                    int idItem = getArguments().getInt(ARG_ID);
                    String nuevaDesc = etDescripcion.getText().toString();
                    boolean nuevoEstado = switchEstado.isChecked();
                    MediaItem item = new MediaItem(getArguments().getString(ARG_TITULO), nuevaDesc, nuevoEstado);
                    item.setId(idItem);
                    listener.onMediaUpdated(item);
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.cancel())
                .setNeutralButton("Borrar", (dialog, id) -> {
                    int idItem = getArguments().getInt(ARG_ID);
                    MediaItem item = new MediaItem(getArguments().getString(ARG_TITULO), getArguments().getString(ARG_DESCRIPCION),
                            getArguments().getBoolean(ARG_ESTADO));
                    item.setId(idItem);
                    listener.onMediaDeleted(item);
                });

        return builder.create();
    }
}
