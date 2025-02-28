package com.example.appseguimiento.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import com.example.appseguimiento.R;

public class NuevoMediaDialog extends DialogFragment {

    public interface NuevoMediaDialogListener {
        void onMediaAdded(String titulo, String descripcion, String tipo);
    }

    private NuevoMediaDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            listener = (NuevoMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar NuevoMediaDialogListener");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_new_title)); // Asegúrate de tenerlo en strings.xml

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_nuevo_media, null);
        final EditText etTitulo = view.findViewById(R.id.etTitulo);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        final Spinner spinnerTipo = view.findViewById(R.id.spinnerTipo);

        // Configurar el Spinner con los valores
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                getContext(),
                R.array.tipo_entries,
                android.R.layout.simple_spinner_item
        );
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapterSpinner);

        builder.setView(view)
                .setPositiveButton(getString(R.string.dialog_new_positive), (dialog, id) -> {
                    String titulo = etTitulo.getText().toString();
                    String descripcion = etDescripcion.getText().toString();
                    // Obtener el valor real usando la posición seleccionada
                    int position = spinnerTipo.getSelectedItemPosition();
                    String tipo = getResources().getStringArray(R.array.tipo_values)[position];
                    listener.onMediaAdded(titulo, descripcion, tipo);
                })
                .setNegativeButton(getString(R.string.dialog_new_negative), (dialog, id) -> dialog.cancel());

        return builder.create();
    }
}
