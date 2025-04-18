package com.example.appseguimiento.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.appseguimiento.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NuevoMediaDialog extends DialogFragment {

    public interface NuevoMediaDialogListener {
        void onMediaAdded(String titulo, String descripcion, String tipo, String nombreImagen);
    }

    private NuevoMediaDialogListener listener;
    private Uri uriImagenCapturada;
    private ImageView ivPreview;

    private String nombreImagenSubida = "";

    private ActivityResultLauncher<Intent> tomarFotoLauncher;



    private static final int REQUEST_TOMAR_FOTO = 101;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            listener = (NuevoMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar NuevoMediaDialogListener");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_new_title));

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_nuevo_media, null);

        final EditText etTitulo = view.findViewById(R.id.etTitulo);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        final Spinner spinnerTipo = view.findViewById(R.id.spinnerTipo);
        ivPreview = view.findViewById(R.id.ivPreview);

        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                getContext(),
                R.array.tipo_entries,
                android.R.layout.simple_spinner_item
        );
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapterSpinner);

        ivPreview.setOnClickListener(v -> tomarFoto());

        builder.setView(view)
                .setPositiveButton(getString(R.string.dialog_new_positive), (dialog, id) -> {
                    if (nombreImagenSubida == null || nombreImagenSubida.isEmpty()) {
                        Toast.makeText(getContext(), "Primero toma una foto", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String titulo = etTitulo.getText().toString();
                    String descripcion = etDescripcion.getText().toString();
                    int position = spinnerTipo.getSelectedItemPosition();
                    String tipo = getResources().getStringArray(R.array.tipo_values)[position];
                    listener.onMediaAdded(titulo, descripcion, tipo, nombreImagenSubida);
                })
                .setNegativeButton(getString(R.string.dialog_new_negative), (dialog, id) -> dialog.cancel());
        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && uriImagenCapturada != null) {
                        ivPreview.setImageURI(uriImagenCapturada);
                        subirImagen(uriImagenCapturada);
                    }
                }
        );

        return builder.create();
    }

    private void tomarFoto() {
        Context context = requireContext();
        File directorio = context.getExternalFilesDir("imagenes");
        String nombreArchivo = "img_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
        File archivoImagen = new File(directorio, nombreArchivo);

        uriImagenCapturada = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                archivoImagen
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImagenCapturada);
        tomarFotoLauncher.launch(intent);
    }

    private void subirImagen(Uri uri) {
        new Thread(() -> {
            try {
                String nombreArchivo = "img_" + System.currentTimeMillis() + ".jpg";
                String boundary = "===" + System.currentTimeMillis() + "===";
                String LINE_FEED = "\r\n";

                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/upload_image.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream outputStream = conn.getOutputStream();
                BufferedOutputStream writer = new BufferedOutputStream(outputStream);

                String header = "--" + boundary + LINE_FEED +
                        "Content-Disposition: form-data; name=\"image\"; filename=\"" + nombreArchivo + "\"" + LINE_FEED +
                        "Content-Type: image/jpeg" + LINE_FEED + LINE_FEED;
                writer.write(header.getBytes());

                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                }
                writer.write(LINE_FEED.getBytes());
                inputStream.close();

                String footer = "--" + boundary + "--" + LINE_FEED;
                writer.write(footer.getBytes());
                writer.flush();
                writer.close();

                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_OK) {
                    nombreImagenSubida = nombreArchivo;
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Imagen subida", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    );
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Excepción al subir imagen", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
