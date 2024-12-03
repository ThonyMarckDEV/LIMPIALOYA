package com.example.limpialoya.Interfaces;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VerReportesUI extends AppCompatActivity {

    private LinearLayout contenedorReportes;
    private ProgressDialog progressDialog; // Declarar el ProgressDialog
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ver_reportes_ui);

        // Inicializar el contenedor donde se mostrarán los reportes
        contenedorReportes = findViewById(R.id.contenedorReportes);

        // Inicializar el ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Cargando reportes...");
        progressDialog.setCancelable(false); // Evitar que se pueda cancelar tocando fuera del diálogo
        progressDialog.show(); // Mostrar el diálogo de progreso

        // Obtener los reportes desde el servidor
        obtenerReportesPendientes();
    }

    // Método para obtener los reportes pendientes
    private void obtenerReportesPendientes() {
        new Thread(() -> {
            try {
                URL url = new URL(ApiService.BASE_URL + "listarReportes.php");  // Cambia por tu URL
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

                // Procesar la respuesta
                JSONArray reportesArray = new JSONArray(result.toString());

                // Mostrar los reportes en el UI principal
                runOnUiThread(() -> agregarReportesAlLayout(reportesArray));
                progressDialog.dismiss(); // Ocultar el ProgressDialog cuando los reportes se hayan cargado

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(VerReportesUI.this, "Error al obtener reportes", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Método para agregar cada reporte al layout
    private void agregarReportesAlLayout(JSONArray reportes) {
        try {
            for (int i = 0; i < reportes.length(); i++) {
                JSONObject reporte = reportes.getJSONObject(i);
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_reporte, contenedorReportes, false);

                // Verificar si el idReporte está presente en el JSON y asignarlo, manejar si es nulo
                int idReporte = reporte.has("idReporte") ? reporte.optInt("idReporte", -1) : -1;  // Si el idReporte no existe, poner -1
                if (idReporte == -1) {
                    Toast.makeText(this, "ID del reporte no válido", Toast.LENGTH_SHORT).show();
                    continue;  // Saltar a la siguiente iteración si no hay un id válido
                }

                // Obtener los demás datos del reporte
                String descripcion = reporte.optString("descripcion", "Sin descripción");
                String fecha = reporte.optString("fecha", "Fecha no disponible");
                String username = reporte.optString("username", "Usuario desconocido");
                String imagenUrl = reporte.optString("imagen_url", "");  // URL de la imagen

                // Asignar los datos a las vistas
                TextView txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
                TextView txtUsuario = itemView.findViewById(R.id.txtUsuario);
                TextView txtFecha = itemView.findViewById(R.id.txtFecha);
                ImageView imgReporte = itemView.findViewById(R.id.imgReporte);
                Button btnVerReporte = itemView.findViewById(R.id.btnVerReporte);

                txtDescripcion.setText(descripcion);
                txtUsuario.setText("Usuario: " + username);
                txtFecha.setText("Fecha: " + fecha);

                // Usar Picasso para cargar la imagen
                Picasso.get().load(imagenUrl).placeholder(R.drawable.logo).into(imgReporte);

                // Configurar el botón "Ver Reporte"
                btnVerReporte.setOnClickListener(v -> {
                    // Abrir nueva actividad con detalles del reporte
                    Intent intent = new Intent(VerReportesUI.this, GestionarReporteUI.class);

                    // Pasar los datos del reporte al intent
                    intent.putExtra("idReporte", idReporte); // Pasa el idReporte como entero
                    intent.putExtra("fecha", fecha);
                    intent.putExtra("descripcion", descripcion);
                    intent.putExtra("imagenUrl", imagenUrl);
                    intent.putExtra("username", username);

                    // Iniciar la actividad GestionarReporteUI
                    startActivity(intent);

                    finish();
                });

                // Añadir la vista del reporte al contenedor
                contenedorReportes.addView(itemView);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}