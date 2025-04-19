package com.example.appseguimiento.ui.dialog;

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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.appseguimiento.R;

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
import java.util.Locale;

import android.Manifest;
import android.util.Base64;

public class NuevoMediaDialog extends DialogFragment {

    public interface NuevoMediaDialogListener {
        void onMediaAdded(String titulo, String descripcion, String tipo, String nombreImagen);
    }

    private NuevoMediaDialogListener listener;
    private Uri uriImagenCapturada;
    private ImageView ivPreview;
    private String nombreImagenSubida = "";
    private Bitmap imagenBitmap;

    private ActivityResultLauncher<Intent> tomarFotoLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            listener = (NuevoMediaDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " debe implementar NuevoMediaDialogListener");
        }

        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        if (uriImagenCapturada != null) {
                            try {
                                imagenBitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(uriImagenCapturada));
                                ivPreview.setImageBitmap(imagenBitmap);
                                subirImagenComoBase64(imagenBitmap, nombreImagenSubida);
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Error al cargar imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Foto cancelada o error al capturar", Toast.LENGTH_LONG).show();
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        tomarFoto();
                    } else {
                        Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_LONG).show();
                    }
                }
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_new_title));

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_nuevo_media, null);

        final EditText etTitulo = view.findViewById(R.id.etTitulo);
        final EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        final Spinner spinnerTipo = view.findViewById(R.id.spinnerTipo);
        ivPreview = view.findViewById(R.id.ivPreview);
        Button btnTomarFoto = view.findViewById(R.id.btnTomarFoto);
        btnTomarFoto.setOnClickListener(v -> verificarPermisosYTomarFoto());

        ivPreview.setOnClickListener(v -> verificarPermisosYTomarFoto());

        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                getContext(), R.array.tipo_entries, android.R.layout.simple_spinner_item
        );
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapterSpinner);

        builder.setView(view)
                .setPositiveButton(getString(R.string.dialog_new_positive), (dialog, id) -> {
                    if (nombreImagenSubida == null || nombreImagenSubida.isEmpty()) {
                        Toast.makeText(getContext(), "Primero toma una foto", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String titulo = etTitulo.getText().toString();
                    String descripcion = etDescripcion.getText().toString();
                    int position = spinnerTipo.getSelectedItemPosition();
                    String tipo = getResources().getStringArray(R.array.tipo_values)[position];
                    listener.onMediaAdded(titulo, descripcion, tipo, nombreImagenSubida);
                })
                .setNegativeButton(getString(R.string.dialog_new_negative), (dialog, id) -> dialog.cancel());

        return builder.create();
    }

    private void verificarPermisosYTomarFoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            tomarFoto();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void tomarFoto() {
        Context context = requireContext();
        File directorio = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!directorio.exists()) directorio.mkdirs();

        nombreImagenSubida = "img_" + System.currentTimeMillis() + ".jpg";
        File archivoImagen = new File(directorio, nombreImagenSubida);

        uriImagenCapturada = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                archivoImagen
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImagenCapturada);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        tomarFotoLauncher.launch(intent);
    }

    private void subirImagenComoBase64(Bitmap bitmap, String nombreArchivo) {
        new Thread(() -> {
            try {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Reduciendo y preparando para subir...", Toast.LENGTH_LONG).show()
                );

                int maxWidth = 800;
                int maxHeight = 800;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;

                int finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > ratioBitmap) {
                    finalWidth = (int) ((float) maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float) maxWidth / ratioBitmap);
                }

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/upload_image.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String parametros = "imagen=" + URLEncoder.encode(imagenBase64, "UTF-8")
                        + "&nombre=" + URLEncoder.encode(nombreArchivo, "UTF-8");

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
