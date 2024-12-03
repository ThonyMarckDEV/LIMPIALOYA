package com.example.limpialoya.Interfaces;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapUIUser extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map_uiuser);

        // Inicializar el servicio de localización
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtener el mapa asíncronamente
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Habilitar el botón de ubicación en el mapa de Google
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);  // Habilitar el botón de ubicación
            centerMapOnMyLocation();
        } else {
            // Solicitar permisos de ubicación si no han sido otorgados
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Establecer el adaptador personalizado para InfoWindow
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Llamar a la función para obtener los reportes aprobados
        obtenerReportesAprobados();
    }

    private void obtenerReportesAprobados() {
        new Thread(() -> {
            try {
                URL url = new URL(ApiService.BASE_URL + "get_reportes.php");  // Cambia por la URL de tu servidor
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                connection.disconnect();

                // Imprime el resultado JSON obtenido en el log para verificar
                String jsonResponse = result.toString();
                System.out.println("Respuesta JSON: " + jsonResponse);  // Aquí puedes usar Log.d si prefieres usar el log de Android Studio

                JSONArray reportesArray = new JSONArray(jsonResponse);

                // Actualizar el mapa con los reportes obtenidos
                runOnUiThread(() -> agregarMarcadoresEnMapa(reportesArray));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void agregarMarcadoresEnMapa(JSONArray reportes) {
        try {
            for (int i = 0; i < reportes.length(); i++) {
                JSONObject reporte = reportes.getJSONObject(i);
                double latitud = reporte.getDouble("latitud");
                double longitud = reporte.getDouble("longitud");
                String descripcion = reporte.getString("descripcion");
                String imagenUrl = reporte.getString("imagen_url");
                String username = reporte.getString("username");  // Obtener el nombre de usuario

                LatLng ubicacionReporte = new LatLng(latitud, longitud);

                // Generar un color de marcador aleatorio
                float hue = (float) (Math.random() * 360);

                // Agregar un marcador en el mapa con el username y descripción
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(ubicacionReporte)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)) // Establecer el color aleatorio
                        .title(descripcion + " - Reportado por: " + username)  // Mostrar la descripción y el username en el título
                        .snippet(imagenUrl));  // La URL de la imagen se usa en el snippet para luego mostrarla

                // Mostrar la imagen en un InfoWindow personalizado
                marker.showInfoWindow();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Método para centrar el mapa en la ubicación actual del usuario
    private void centerMapOnMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Obtén la última ubicación conocida
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                            // Opcional: establecer una posición de cámara más detallada
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(userLocation)
                                    .zoom(15)
                                    .build();
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        } else {
                            Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mWindow;

        CustomInfoWindowAdapter() {
            mWindow = LayoutInflater.from(MapUIUser.this).inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);
            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        private void render(Marker marker, View view) {
            String imageUrl = marker.getSnippet();

            TextView title = view.findViewById(R.id.title);
            title.setText(marker.getTitle());

            ImageView imageView = view.findViewById(R.id.image);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get().load(imageUrl)
                        .placeholder(R.drawable.logo)  // Imagen temporal mientras carga
                        .error(R.drawable.loadingperfil)  // Imagen en caso de error
                        .into(imageView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                // Forzar la actualización del InfoWindow cuando la imagen se haya cargado
                                if (marker.isInfoWindowShown()) {
                                    marker.hideInfoWindow();
                                    marker.showInfoWindow();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                            }
                        });
            } else {
                imageView.setImageResource(R.drawable.logo);  // Imagen por defecto si no hay URL
            }
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();  // Volverá a la actividad anterior si hay una en la pila
    }

}
