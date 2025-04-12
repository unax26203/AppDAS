package com.example.appseguimiento.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteLoginWorker extends Worker {

    public RemoteLoginWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Datos de entrada
        String email = getInputData().getString("email");
        String password = getInputData().getString("password");

        try {
            //URL del archivo PHP de inicio de sesión
            URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/login.php");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Parámetros de la solicitud
            String parametros = "email=" + email + "&password=" + password;

            OutputStream os = conn.getOutputStream();
            os.write(parametros.getBytes());
            os.flush();
            os.close();

            // Leer respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder resultado = new StringBuilder();
            String linea;

            while ((linea = reader.readLine()) != null) {
                resultado.append(linea);
            }

            reader.close();

            // Procesar JSON
            org.json.JSONObject json = new org.json.JSONObject(resultado.toString());
            String status = json.getString("status");

            if (status.equals("OK")) {
                // Datos del usuario
                String nombre = json.getString("nombre");
                int id = json.getInt("id");

                Data output = new Data.Builder()
                        .putBoolean("success", true)
                        .putString("nombre", nombre)
                        .putInt("id", id)
                        .build();

                return Result.success(output);
            } else {
                Data output = new Data.Builder()
                        .putBoolean("success", false)
                        .putString("error", json.getString("msg"))
                        .build();

                return Result.success(output);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
