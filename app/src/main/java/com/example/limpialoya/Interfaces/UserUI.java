package com.example.limpialoya.Interfaces;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.bumptech.glide.Glide;
import com.example.limpialoya.Clases.PerfilImagenLoader;
import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;
import com.example.limpialoya.Servicios.CheckUpdate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class UserUI extends AppCompatActivity {
    private TextView txtNombreUsuario;
    private ImageView imgvPerfil,btnDeslogear;
    private ImageView imgvLoading; // Agregar esta línea
    private String username; // Declara username aquí
    private Button btnMapa;
    private TextView txtEcoPoints;
    // =================================================================================================
    //NAVBAR
    // =================================================================================================
    private ImageButton btnReporte,btnRanking;
    // =================================================================================================


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_ui);

//        // Llamamos a la clase UpdateChecker para verificar actualizaciones
//        CheckUpdate updateChecker = new CheckUpdate(this);
//
//        // Llamamos a checkForUpdate y pasamos un nuevo UpdateListener con los métodos implementados
//        updateChecker.checkForUpdate();

        // Inicializar elementos
        txtNombreUsuario = findViewById(R.id.txtName);
        btnDeslogear = findViewById(R.id.btnLogout);
        imgvPerfil = findViewById(R.id.imgvPerfil);
        imgvLoading = findViewById(R.id.imgvLoading);
        btnMapa = findViewById(R.id.btnMAPA);
        txtEcoPoints = findViewById(R.id.txtEcoPoints);

        // Cargar y mostrar ecoPoints iniciales de SharedPreferences
        loadEcoPointsFromPrefs();

        // Actualizar ecoPoints desde el servidor
        updateEcoPointsFromServer();

        // Mostrar el GIF de carga
        if (imgvLoading != null) {
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.loadingperfil)
                    .into(imgvLoading);
        }

        // Recuperar el username de SharedPreferences
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = preferences.getString("username", "Usuario no encontrado"); // "Usuario no encontrado" es el valor por defecto

        PerfilImagenLoader perfilLoader = new PerfilImagenLoader(this, imgvLoading, imgvPerfil);
        perfilLoader.cargarImagen(username);


        // Colocar el username en el TextView
        txtNombreUsuario.setText(username);


        // =================================================================================================
        //NAVBAR
        // =================================================================================================
        // Encontrar los botones de la barra de navegación
        btnReporte = findViewById(R.id.btnReportar);
        btnRanking = findViewById(R.id.btnRankings);

        // Configurar el botón reporte
        btnReporte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(UserUI.this, ReportarUI.class);
                startActivity(mapIntent);
                finish();  // Finaliza la actividad actual para que no se quede en la pila
            }
        });

        // Configurar el botón rankings
        btnRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(UserUI.this, RankingUI.class);
                startActivity(mapIntent);
                finish();  // Finaliza la actividad actual para que no se quede en la pila
            }
        });

        // =================================================================================================

    // Configurar el botón verMapa
        btnMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(UserUI.this, MapUIUser.class);
                startActivity(mapIntent);
            }
        });

        btnDeslogear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar la pantalla de carga
                Intent cargaIntent = new Intent(UserUI.this, CargaUI.class);
                startActivity(cargaIntent);
                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("username");
                editor.apply();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL(ApiService.BASE_URL + "update_status.php");
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                            connection.setDoOutput(true);

                            // Enviar el nombre de usuario al servidor
                            String postData = "username=" + URLEncoder.encode(username, "UTF-8");
                            OutputStream os = connection.getOutputStream();
                            os.write(postData.getBytes());
                            os.flush();
                            os.close();

                            // Verificar el código de respuesta
                            int responseCode = connection.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                Log.d("Deslogear", "Estado actualizado exitosamente");
                            } else {
                                Log.d("Deslogear", "Error al actualizar estado: " + responseCode);
                            }

                            connection.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("Deslogear", "Error en la solicitud: " + e.getMessage());
                        }
                    }
                }).start();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(UserUI.this, LoginUI.class);
                        startActivity(intent);
                        finish();
                    }
                }, 500);
            }
        });


    }

    private void loadEcoPointsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int ecoPoints = prefs.getInt("ecoPoints", 0);  // 0 es el valor predeterminado si no hay nada guardado
        txtEcoPoints.setText(String.valueOf(ecoPoints));
    }

    private void updateEcoPointsFromServer() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username != null) {
            new FetchEcoPointsTask().execute(username);
        }
    }

    private class FetchEcoPointsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(ApiService.BASE_URL + "get_ecopoints.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String postData = "username=" + URLEncoder.encode(params[0], "UTF-8");
                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                InputStream is = connection.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) return;

            try {
                JSONObject jsonResponse = new JSONObject(result);
                String status = jsonResponse.getString("status");
                if (status.equals("success")) {
                    int ecoPoints = jsonResponse.getInt("ecoPoints");
                    txtEcoPoints.setText(String.valueOf(ecoPoints));

                    // Guardar los nuevos ecoPoints en SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("ecoPoints", ecoPoints);
                    editor.apply();
                } else {
                    String message = jsonResponse.getString("message");
                    Toast.makeText(UserUI.this, message, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(UserUI.this, "Error al procesar la respuesta: " + result, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkLocationEnabled();
        }
    }

    private void checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            openMap();
        }
    }

    private void openMap() {
        Intent mapIntent = new Intent(UserUI.this, ReportarUI.class);
        mapIntent.putExtra("username", username);
        startActivity(mapIntent);
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}