package com.example.appseguimiento;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.appseguimiento.BuildConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private FusedLocationProviderClient proveedorUbicacion;
    private final int REQUEST_CODE_UBICACION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        proveedorUbicacion = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_UBICACION);
        } else {
            mapa.setMyLocationEnabled(true);
            obtenerUbicacion();
        }
    }

    private void obtenerUbicacion1() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        proveedorUbicacion.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());
                mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15));
                mapa.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title("Estás aquí")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                buscarLugares("library", location);
                buscarLugares("movie_theater", location);
            }
        });
    }

    private void obtenerUbicacion() {
        LatLng posicionSimulada = new LatLng(40.4168, -3.7038); // Centro de Madrid
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionSimulada, 15));
        mapa.addMarker(new MarkerOptions()
                .position(posicionSimulada)
                .title("Ubicación simulada")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        Location ubicacionSimulada = new Location("");
        ubicacionSimulada.setLatitude(40.4168);
        ubicacionSimulada.setLongitude(-3.7038);

        buscarLugares("library", ubicacionSimulada);
        buscarLugares("movie_theater", ubicacionSimulada);
    }

    private void buscarLugares(String tipoLugar, Location location) {
        String servidorPhp = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/uzardoya001/WEB/config.php";
        String url = servidorPhp + "?lat=" + location.getLatitude()
                + "&lng=" + location.getLongitude()
                + "&tipo=" + tipoLugar;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("API_RESPONSE", response.toString());
                        JSONArray resultados = response.getJSONArray("results");
                        for (int i = 0; i < resultados.length(); i++) {
                            JSONObject lugar = resultados.getJSONObject(i);
                            JSONObject geometry = lugar.getJSONObject("geometry").getJSONObject("location");
                            double lat = geometry.getDouble("lat");
                            double lng = geometry.getDouble("lng");
                            String nombre = lugar.getString("name");

                            LatLng posicion = new LatLng(lat, lng);
                            mapa.addMarker(new MarkerOptions()
                                    .position(posicion)
                                    .title(nombre));
                        }

                        if (resultados.length() == 0) {
                            Toast.makeText(this, "No se encontraron lugares del tipo solicitado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al procesar lugares", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error en la búsqueda: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_UBICACION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mapa.setMyLocationEnabled(true);
                obtenerUbicacion();
            }
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
        }
    }
}
