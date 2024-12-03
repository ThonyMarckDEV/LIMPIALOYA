package com.example.limpialoya.Interfaces;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.limpialoya.Clases.PerfilImagenLoader;
import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AdminUI extends AppCompatActivity {
    private TextView txtNombreAdmin;
    private ImageView imgvPerfil,btnDeslogear;
    private ImageView imgvLoading; // Agregar esta línea
    private String username; // Declara username aquí
    private Button btnVerReportes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_ui);

        // Inicializar elementos
        txtNombreAdmin = findViewById(R.id.txtName);
        btnDeslogear = findViewById(R.id.btnLogout);
        imgvPerfil = findViewById(R.id.imgvPerfil);
        imgvLoading = findViewById(R.id.imgvLoading);
        btnVerReportes = findViewById(R.id.btnVerReportes);

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
        txtNombreAdmin.setText(username);

        btnDeslogear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar la pantalla de carga
                Intent cargaIntent = new Intent(AdminUI.this, CargaUI.class);
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
                        Intent intent = new Intent(AdminUI.this, LoginUI.class);
                        startActivity(intent);
                        finish();
                    }
                }, 500);
            }
        });

        btnVerReportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(AdminUI.this, VerReportesUI.class);
                startActivity(mapIntent);
            }
        });



    }
}