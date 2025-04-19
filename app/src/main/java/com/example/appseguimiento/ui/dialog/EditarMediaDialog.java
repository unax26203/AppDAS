package com.example.appseguimiento.ui.dialog;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.appseguimiento.R;
import com.example.appseguimiento.data.MediaItem;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

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
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;


    public static EditarMediaDialog newInstance(MediaItem item) {
        EditarMediaDialog fragment = new EditarMediaDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, item.getId());
        args.putString(ARG_TITULO, item.getTitulo());
        args.putString(ARG_DESCRIPCION, item.getDescripcion());
        args.putBoolean(ARG_ESTADO, item.isCompleted());
        args.putString(ARG_TIPO, item.getTipo());
        args.putString("imagen", item.getImagen());
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

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        tomarFoto();
                    } else {
                        Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && uriImagenCapturada != null && nombreImagenSubida != null) {
                        Glide.with(getContext()).load(uriImagenCapturada).into(ivPreview);
                        subirImagen(uriImagenCapturada, nombreImagenSubida);
                    } else {
                        Toast.makeText(getContext(), "Foto cancelada o error al capturar", Toast.LENGTH_SHORT).show();
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
        Button btnTomarFoto = view.findViewById(R.id.btnTomarFoto);
        btnTomarFoto.setOnClickListener(v -> tomarFoto());

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

                    String imagenAnterior = getArguments().getString("imagen");
                    String imagenFinal = (nombreImagenSubida != null && !nombreImagenSubida.isEmpty())
                            ? nombreImagenSubida
                            : imagenAnterior;

                    MediaItem item = new MediaItem(
                            getArguments().getString(ARG_TITULO),
                            nuevaDesc,
                            nuevoEstado,
                            getArguments().getString(ARG_TIPO),
                            imagenFinal
                    );
                    item.setId(idItem);
                    listener.onMediaUpdated(item, imagenFinal);
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
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            return;
        }

        Context context = requireContext();
        File directorio = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        nombreImagenSubida = "img_" + System.currentTimeMillis() + ".jpg";
        File archivoImagen = new File(directorio, nombreImagenSubida);

        uriImagenCapturada = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                archivoImagen
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImagenCapturada);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        tomarFotoLauncher.launch(intent);
    }

    private void subirImagen(Uri uri, String nombreArchivo) {
        new Thread(() -> {
            try {
                Bitmap originalBitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(uri));

                // Redimensionar la imagen (máx. 800x800)
                int maxWidth = 800, maxHeight = 800;
                int width = originalBitmap.getWidth();
                int height = originalBitmap.getHeight();
                float ratio = (float) width / height;
                if (width > maxWidth || height > maxHeight) {
                    if (ratio > 1) {
                        width = maxWidth;
                        height = (int) (width / ratio);
                    } else {
                        height = maxHeight;
                        width = (int) (height * ratio);
                    }
                }

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/upload_image.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String parametros = "imagen=" + URLEncoder.encode(imagenBase64, "UTF-8") +
                        "&nombre=" + URLEncoder.encode(nombreArchivo, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(parametros.getBytes());
                os.flush();
                os.close();

                int status = conn.getResponseCode();
                InputStream responseStream = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) sb.append(linea);
                String respuesta = sb.toString();

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Servidor: [" + status + "] " + respuesta, Toast.LENGTH_LONG).show()
                );
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (uriImagenCapturada != null) {
            outState.putParcelable("uriImagen", uriImagenCapturada);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            uriImagenCapturada = savedInstanceState.getParcelable("uriImagen");
            if (uriImagenCapturada != null && ivPreview != null) {
                Glide.with(requireContext()).load(uriImagenCapturada).into(ivPreview);
            }
        }
    }

}
