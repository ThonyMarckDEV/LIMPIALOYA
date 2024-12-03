package com.example.limpialoya.Interfaces;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportarUI extends AppCompatActivity {
    // =================================================================================================
    //NAVBAR
    // =================================================================================================
    private ImageButton btnPerfil,btnRanking;
    // =================================================================================================

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int TAKE_PHOTO_REQUEST = 1;
    private EditText textArea;
    private TextView txvfecha;
    private ImageView imgvReporte;
    private Button  btnReportar;
    private Button btnCargarFoto;
    private Bitmap imageBitmap;  // Almacena la imagen después de tomarla
    private ProgressDialog progressDialog;
    private String idUsuario;
    private String username;
    private String imageUrl;  // Almacenar la URL de la imagen después de subirla

    // Añadir variables para la API de ubicación
    private FusedLocationProviderClient fusedLocationClient;
    private double latitud, longitud;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportar_ui);

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            obtenerUbicacion();
        }

        // =================================================================================================
        //NAVBAR
        // =================================================================================================
        // Encontrar los botones de la barra de navegación
        btnPerfil = findViewById(R.id.btnPerfil);
        btnRanking = findViewById(R.id.btnRankings);

        // Configurar el botón perfil
        btnPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(ReportarUI.this, UserUI.class);
                startActivity(mapIntent);
                finish();  // Finaliza la actividad actual para que no se quede en la pila
            }
        });


        // Configurar el botón rankings
        btnRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(ReportarUI.this, RankingUI.class);
                startActivity(mapIntent);
                finish();  // Finaliza la actividad actual para que no se quede en la pila
            }
        });

        // =================================================================================================


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        textArea = findViewById(R.id.textArea);
        txvfecha = findViewById(R.id.txvfecha);
        imgvReporte = findViewById(R.id.imgvReporte);
        btnCargarFoto = findViewById(R.id.btnCargarFoto);
        btnReportar = findViewById(R.id.btnSubirReporte);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando reporte...");
        progressDialog.setCancelable(false);


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        txvfecha.setText(currentDateAndTime);

        // Recuperar el username de SharedPreferences
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = preferences.getString("username", "Usuario no encontrado"); // "Usuario no encontrado" es el valor por defecto

        if (username != null) {
            new ObtenerIdUsuarioTask().execute(username);
        }


        // Verifica si imageBitmap es nulo antes de enviar el reporte
        btnReportar.setOnClickListener(v -> {
            if (idUsuario != null && imageBitmap != null) {  // Cambia esta línea para asegurarte de que imageBitmap no sea nulo
                new EnviarReporteTask().execute();  // Enviar el reporte cuando se presiona el botón
            } else {
                Toast.makeText(this, "No se pudo obtener el idUsuario o la imagen no está seleccionada", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para abrir la actividad de la cámara
        btnCargarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(ReportarUI.this, CameraCaptureUI.class);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        });

    }


    // Método para obtener la ubicación del usuario
    private void obtenerUbicacion() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);  // Intervalo de actualización de 10 segundos
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }

                    // Obtén la última ubicación conocida
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        latitud = location.getLatitude();
                        longitud = location.getLongitude();
                    }
                }
            }, Looper.getMainLooper());
        }
    }



    // Asegúrate de pedir permisos si no los tienes
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ObtenerIdUsuarioTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            try {
                // URL del servicio que obtiene el idUsuario basado en el username
                URL url = new URL(ApiService.BASE_URL + "get_user_id.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                // Escribir los parámetros del POST
                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write("username=" + username);
                writer.flush();
                writer.close();
                os.close();

                // Leer la respuesta
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                    return result.toString(); // Esto debería ser el idUsuario
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                idUsuario = result.trim();  // Asegúrate de que no haya espacios o saltos de línea extraños
            } else {
                Toast.makeText(ReportarUI.this, "Error al obtener idUsuario", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = Uri.parse(data.getStringExtra("capturedImageUri"));
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                imageBitmap = BitmapFactory.decodeStream(imageStream);  // Asigna la imagen cargada a imageBitmap
                imgvReporte.setImageBitmap(imageBitmap);  // Muestra la imagen en imgvReporte
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Tarea para enviar el reporte junto con la latitud y longitud
    private class EnviarReporteTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                // Convertir la imagen en un array de bytes para subirla (ya implementado)
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // Subir la imagen primero (ya implementado)
                String imageUploadUrl = ApiService.BASE_URL + "upload_image.php";  // URL del script de subida
                URL url = new URL(imageUploadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
                connection.setRequestProperty("Connection", "Keep-Alive");

                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                dos.writeBytes("--*****\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n");
                dos.writeBytes("\r\n");
                dos.write(imageBytes);
                dos.writeBytes("\r\n");
                dos.writeBytes("--*****--\r\n");

                dos.flush();
                dos.close();

                // Leer la respuesta del servidor después de subir la imagen (ya implementado)
                InputStream responseStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsear la respuesta para obtener la URL de la imagen subida (ya implementado)
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getString("status").equals("success")) {
                    imageUrl = jsonResponse.getString("image_url");  // URL de la imagen subida
                } else {
                    return null;  // Error al subir la imagen
                }

                // Enviar los datos del reporte junto con la URL de la imagen, latitud y longitud
                URL reportUrl = new URL(ApiService.BASE_URL + "report_data.php");
                HttpURLConnection reportConnection = (HttpURLConnection) reportUrl.openConnection();
                reportConnection.setRequestMethod("POST");
                reportConnection.setDoOutput(true);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String fechaHora = sdf.format(new Date());
                String descripcion = textArea.getText().toString();

                // Añadir la latitud y longitud a los datos del reporte
                String postData = "idUsuario=" + idUsuario + "&fecha=" + fechaHora + "&descripcion=" + descripcion +
                        "&imagen_url=" + imageUrl + "&latitud=" + latitud + "&longitud=" + longitud;

                // Enviar los datos del reporte (ya implementado)
                OutputStream os = reportConnection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = reportConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return "Reporte enviado exitosamente";
                } else {
                    return "Error al enviar el reporte";
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result != null) {
                Toast.makeText(ReportarUI.this, result, Toast.LENGTH_SHORT).show();
                // Limpiar campos después del reporte
                textArea.setText("");
                imgvReporte.setImageResource(android.R.color.transparent);
                imageBitmap = null;
                imageUrl = null;  // Limpiar la URL después de enviar
                textArea.clearFocus();
            } else {
                Toast.makeText(ReportarUI.this, "Error al enviar reporte", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ReportarUI.this, UserUI.class);
        startActivity(intent);
        finish();
    }
}