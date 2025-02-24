package com.example.appseguimiento.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.example.appseguimiento.R;

public class NuevoMediaDialog extends DialogFragment {

    public interface NuevoMediaDialogListener {
        void onMediaAdded(String titulo, String descripcion);
    }

    private NuevoMediaDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // El Activity debe implementar NuevoMediaDialogListener
        try {
            listener = (NuevoMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar NuevoMediaDialogListener");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Nuevo Libro/Película");

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_nuevo_media, null);
        final EditText etTitulo = view.findViewById(R.id.etTitulo);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);

        builder.setView(view)
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String titulo = etTitulo.getText().toString();
                    String descripcion = etDescripcion.getText().toString();
                    listener.onMediaAdded(titulo, descripcion);
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.cancel());

        return builder.create();
    }
}
