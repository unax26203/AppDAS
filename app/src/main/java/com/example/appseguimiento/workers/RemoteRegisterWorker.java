package com.example.appseguimiento.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RemoteRegisterWorker extends Worker {

    public RemoteRegisterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String nombre = getInputData().getString("nombre");
        String email = getInputData().getString("email");
        String password = getInputData().getString("password");

        HttpURLConnection urlConnection = null;
        String resultado = "";

        try {
            // URL del archivo PHP de registro
            URL destino = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/puzardoya001/WEB/register.php");
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Codificar parámetros
            String parametros = "nombre=" + URLEncoder.encode(nombre, "UTF-8")
                    + "&email=" + URLEncoder.encode(email, "UTF-8")
                    + "&password=" + URLEncoder.encode(password, "UTF-8");

            // Enviar al servidor
            OutputStream os = urlConnection.getOutputStream();
            os.write(parametros.getBytes("UTF-8"));
            os.flush();
            os.close();

            // Recoger respuesta
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200) {
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    resultado += line;
                }
                inputStream.close();
            } else {
                resultado = "ERROR";
            }

        } catch (Exception e) {
            resultado = "ERROR";
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        Data output = new Data.Builder()
                .putString("resultado", resultado)
                .build();

        return Result.success(output);
    }
}