package com.example.limpialoya.Interfaces;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;
import com.squareup.picasso.Picasso;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class GestionarReporteUI extends AppCompatActivity {

    private TextView tvIdReporte, tvUsuario, tvFecha, tvDescripcion;
    private ImageView imgvReporte;
    private Button btnRevisar,btnEliminarReporte;;
    private ProgressDialog progressDialog;  // Declarar el ProgressDialog
    private String idReporte, fecha, descripcion, imagenUrl;
    private String username; // Declara username aquí

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gestionar_reporte_ui);

        // Inicializa el username desde el Intent
        username = getIntent().getStringExtra("username");

        // Vincular los componentes UI con el layout
        tvIdReporte = findViewById(R.id.tvIdReporte);
        tvUsuario = findViewById(R.id.tvUsuario);  // Corregido
        tvFecha = findViewById(R.id.tvFecha);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        imgvReporte = findViewById(R.id.imgvReporte);
        btnRevisar = findViewById(R.id.btnRevisarReporte);
        btnEliminarReporte = findViewById(R.id.btnEliminarReporte);

        // Obtener los datos del Intent
        Intent intent = getIntent();
        idReporte = String.valueOf(getIntent().getIntExtra("idReporte", -1)); // Convertir el int a String para usarlo en UI
        fecha = intent.getStringExtra("fecha");
        descripcion = intent.getStringExtra("descripcion");
        imagenUrl = intent.getStringExtra("imagenUrl");

        // Mostrar los datos en la UI
        tvIdReporte.setText("ID Reporte: " + idReporte);
        tvUsuario.setText("Usuario: " + username);  // Corregido
        tvFecha.setText("Fecha: " + fecha);
        tvDescripcion.setText("Descripción: " + descripcion);

        // Cargar la imagen en el ImageView usando Picasso
        Picasso.get().load(imagenUrl).into(imgvReporte);

        // Manejar el botón Revisar
        btnRevisar.setOnClickListener(v -> {
            // Mostrar el ProgressDialog
            progressDialog = new ProgressDialog(GestionarReporteUI.this);
            progressDialog.setMessage("Revisando reporte...");
            progressDialog.setCancelable(false); // Evitar que se cancele tocando fuera del diálogo
            progressDialog.show();
            // Llamar a una tarea asíncrona para mover el reporte a reportes_revisados
            new RevisarReporteTask().execute();
        });

        // Eliminar reporte
        btnEliminarReporte.setOnClickListener(v -> {
            progressDialog = new ProgressDialog(GestionarReporteUI.this);
            progressDialog.setMessage("Eliminando reporte...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new EliminarReporteTask().execute();
        });
    }

    // Tarea para eliminar el reporte
    private class EliminarReporteTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(ApiService.BASE_URL + "eliminar_reporte.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                String postData = URLEncoder.encode("idReporte", "UTF-8") + "=" + URLEncoder.encode(idReporte, "UTF-8");

                writer.write(postData);
                writer.flush();
                writer.close();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return "Reporte eliminado con éxito";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Error al eliminar el reporte";
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            Toast.makeText(GestionarReporteUI.this, result, Toast.LENGTH_SHORT).show();

            if (result.equals("Reporte eliminado con éxito")) {
                Intent intent = new Intent(GestionarReporteUI.this, AdminUI.class);
                intent.putExtra("username", username);
                startActivity(intent);
                finish();
            }
        }
    }

    private class RevisarReporteTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Construir la URL para cambiar el estado del reporte
                URL url = new URL(ApiService.BASE_URL + "cambiar_estado_reporte.php?idReporte=" + idReporte);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Luego de actualizar el estado del reporte exitosamente, intentamos actualizar los EcoPoints
                    URL urlEcoPoints = new URL(ApiService.BASE_URL + "actualizar_ecopoints.php");
                    HttpURLConnection connectionEcoPoints = (HttpURLConnection) urlEcoPoints.openConnection();
                    connectionEcoPoints.setRequestMethod("POST");
                    connectionEcoPoints.setDoOutput(true);

                    // Enviar los datos de usuario para la actualización de EcoPoints
                    OutputStream os = connectionEcoPoints.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    String postData = "username=" + URLEncoder.encode(username, "UTF-8");
                    writer.write(postData);
                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCodeEcoPoints = connectionEcoPoints.getResponseCode();
                    if (responseCodeEcoPoints == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(connectionEcoPoints.getInputStream()));
                        String line;
                        StringBuilder responseOutput = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            responseOutput.append(line);
                        }
                        br.close();
                        return responseOutput.toString();  // Cambio para ver el mensaje del servidor
                    } else {
                        return "Reporte revisado pero falló la actualización de EcoPoints";
                    }
                } else {
                    return "Error al revisar el reporte";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error al conectar con el servidor";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();  // Asegúrate de que el ProgressDialog se cierre en cualquier caso.
            Log.d("GestionarReporteUI", result);  // Log para depuración.

            // Parsear y verificar la respuesta del servidor antes de proceder
            try {
                JSONObject response = new JSONObject(result);
                if (response.optString("status").equals("success")) {
                    Toast.makeText(GestionarReporteUI.this, response.optString("message"), Toast.LENGTH_LONG).show();
                    finish();  // Solo finalizar si fue exitoso.
                } else {
                    Toast.makeText(GestionarReporteUI.this, response.optString("message", "Error desconocido"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Toast.makeText(GestionarReporteUI.this, "Error al procesar la respuesta del servidor", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
     finish();
    }
}
