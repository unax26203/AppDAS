package com.example.appseguimiento;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.function.BiConsumer;

public class UsefulPlaces {

    public static void buscarLugares(Context context, String tipoLugar, Location location, BiConsumer<LatLng, String> onLugarEncontrado) {
        String servidorPhp = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/config.php";
        String url = servidorPhp + "?lat=" + location.getLatitude()
                + "&lng=" + location.getLongitude()
                + "&tipo=" + tipoLugar;

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("API_RESPONSE", response.toString());
                        JSONArray resultados = response.getJSONArray("results");
                        boolean foundPlaces = false;

                        for (int i = 0; i < resultados.length(); i++) {
                            JSONObject lugar = resultados.getJSONObject(i);
                            JSONObject geometry = lugar.getJSONObject("geometry").getJSONObject("location");
                            double lat = geometry.getDouble("lat");
                            double lng = geometry.getDouble("lng");
                            String nombre = lugar.getString("name");

                            LatLng posicion = new LatLng(lat, lng);
                            onLugarEncontrado.accept(posicion, nombre);
                            foundPlaces = true;
                        }

                        if (!foundPlaces) {
                            Log.d("UsefulPlaces", "No places found for type: " + tipoLugar);
                            if (context instanceof MapActivity) {
                                Toast.makeText(context, "No se encontraron lugares del tipo solicitado", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(context, "Error al procesar lugares", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(context, "Error en la búsqueda: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }
}