package com.example.appseguimiento.ui.dialog;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.example.appseguimiento.R;
import com.example.appseguimiento.data.MediaItem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditarMediaDialog extends DialogFragment {

    public interface EditarMediaDialogListener {
        void onMediaUpdated(MediaItem item, String nombreImagen);
        void onMediaDeleted(MediaItem item);
    }

    private static final String ARG_ID = "id";
    private static final String ARG_TITULO = "titulo";
    private static final String ARG_DESCRIPCION = "descripcion";
    private static final String ARG_ESTADO = "estado";
    private static final String ARG_TIPO = "tipo";

    private Uri uriImagenCapturada;
    private String nombreImagenSubida = null;
    private ImageView ivPreview;

    private ActivityResultLauncher<Intent> tomarFotoLauncher;

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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            listener = (EditarMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar EditarMediaDialogListener");
        }

        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && uriImagenCapturada != null) {
                        ivPreview.setImageURI(uriImagenCapturada);
                        subirImagen(uriImagenCapturada);
                    }
                }
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_edit_title) + getArguments().getString(ARG_TITULO));

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_editar_media, null);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        final Switch switchEstado = view.findViewById(R.id.switchEstado);
        Button btnShare = view.findViewById(R.id.btnShare);
        ivPreview = view.findViewById(R.id.ivPreview);

        etDescripcion.setText(getArguments().getString(ARG_DESCRIPCION));
        switchEstado.setChecked(getArguments().getBoolean(ARG_ESTADO));

        ivPreview.setOnClickListener(v -> tomarFoto());

        btnShare.setOnClickListener(v -> {
            String shareText = "Título: " + getArguments().getString(ARG_TITULO) + "\n" +
                    "Descripción: " + getArguments().getString(ARG_DESCRIPCION) + "\n" +
                    "Estado: " + (getArguments().getBoolean(ARG_ESTADO) ? "Terminada" : "Pendiente");

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
        });

        builder.setView(view)
                .setPositiveButton(getString(R.string.dialog_edit_positive), (dialog, id) -> {
                    int idItem = getArguments().getInt(ARG_ID);
                    String nuevaDesc = etDescripcion.getText().toString();
                    boolean nuevoEstado = switchEstado.isChecked();
                    MediaItem item = new MediaItem(
                            getArguments().getString(ARG_TITULO),
                            nuevaDesc,
                            nuevoEstado,
                            getArguments().getString(ARG_TIPO),
                            nombreImagenSubida
                    );
                    item.setId(idItem);
                    listener.onMediaUpdated(item, nombreImagenSubida);
                })
                .setNegativeButton(getString(R.string.dialog_edit_negative), (dialog, id) -> dialog.cancel())
                .setNeutralButton(getString(R.string.dialog_edit_neutral), (dialog, id) -> {
                    int idItem = getArguments().getInt(ARG_ID);
                    MediaItem item = new MediaItem(
                            getArguments().getString(ARG_TITULO),
                            getArguments().getString(ARG_DESCRIPCION),
                            getArguments().getBoolean(ARG_ESTADO),
                            getArguments().getString(ARG_TIPO),
                            getArguments().getString(ARG_TIPO));
                    item.setId(idItem);
                    listener.onMediaDeleted(item);
                });

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
