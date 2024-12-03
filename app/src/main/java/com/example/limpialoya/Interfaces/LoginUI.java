package com.example.limpialoya.Interfaces;

import static com.example.limpialoya.Clases.LocationHelper.LOCATION_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;
import com.example.limpialoya.Servicios.CheckUpdate;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class LoginUI extends AppCompatActivity {

    private EditText txtUsername, txtPassword;

    private Intent cargaIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_ui);

        // Aquí solicita permisos de ubicación
        requestLocationPermissions();

//        // Llamamos a la clase UpdateChecker para verificar actualizaciones
//        CheckUpdate updateChecker = new CheckUpdate(this);
//
//        // Llamamos a checkForUpdate y pasamos un nuevo UpdateListener con los métodos implementados
//        updateChecker.checkForUpdate();

        cargaIntent = new Intent(LoginUI.this, CargaUI.class);

        // Inicializar campos de entrada y botones
        txtUsername = findViewById(R.id.txtUserNameLogin);
        txtPassword = findViewById(R.id.txtPasswordPerfil);
        Button btnLogin = findViewById(R.id.btnLogearse);
        TextView lblNuevo = findViewById(R.id.lblNuevo);
        TextView lblResta = findViewById(R.id.lblRestablecer);

        //Obtenemos Datos de sharedPreferences si es que hay
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedUsername = preferences.getString("username", null);
        String savedRol = preferences.getString("rol", null);

        if (savedUsername != null && savedRol != null) {
            verificarRol(savedUsername);

        } else {
            // Inicializar la Intent para la pantalla de carga

            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = txtUsername.getText().toString();
                    String password = txtPassword.getText().toString();

                    // Validar campos de entrada
                    if (username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(LoginUI.this, "Por favor ingrese ambos campos", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Ejecutar la tarea de verificación de estado
                    new CheckUserStatusTask().execute(username);
                }
            });

            lblNuevo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    irRegister();
                }
            });

            lblResta.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    irResta();
                }
            });
        }
    }

    private void verificarRol(String username) {
        // Obtener el rol desde SharedPreferences u otra fuente (como el servidor).
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rol = preferences.getString("rol", null); // Asume que ya has guardado el rol previamente.


        if (rol == null) {
            Toast.makeText(LoginUI.this, "No se pudo obtener el rol del usuario", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent;
        switch (rol) {
            case "usuario":
                intent = new Intent(LoginUI.this, UserUI.class);
                break;
            case "admin":
                intent = new Intent(LoginUI.this, AdminUI.class);
                break;
            case "basurero":
                intent = new Intent(LoginUI.this, BasureroUI.class);
                break;
            default:
                Toast.makeText(LoginUI.this, "Rol desconocido", Toast.LENGTH_LONG).show();
                return;
        }

        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    private void irRegister() {

        // Usar Handler para retrasar el inicio de la nueva actividad y permitir que la pantalla de carga se muestre
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LoginUI.this, RegisterUI.class);
                startActivity(intent);
                finish(); // Cerrar la actividad actual para que el usuario no vuelva a ella
            }
        }, 500); // Esperar 500 ms antes de iniciar RegisterUI
    }


    private void irResta() {

        // Usar Handler para retrasar el inicio de la nueva actividad y permitir que la pantalla de carga se muestre
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LoginUI.this, RestablecerUI.class);
                startActivity(intent);
                finish(); // Cerrar la actividad actual para que el usuario no vuelva a ella
            }
        }, 500); // Esperar 500 ms antes de iniciar RegisterUI
    }

    //METOOD OBTENER ESTADO DEL SERVER
    // Clase para verificar el estado del usuario
    private class CheckUserStatusTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return getStatusFromServer(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonResponse = new JSONObject(result);
                String status = jsonResponse.getString("status");

                if (status.equals("loggedOn")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginUI.this, "USUARIO YA LOGEADO!!!", Toast.LENGTH_LONG).show();
                            txtUsername.setText("");
                            txtPassword.setText("");
                        }
                    });
                } else {
                    // Si no está logueado, proceder con el LoginTask
                    String username = txtUsername.getText().toString();
                    String password = txtPassword.getText().toString();
                    new LoginTask().execute(username, password);
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginUI.this, "Error al verificar el estado del usuario", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    // Método para obtener el estado del servidor
    private String getStatusFromServer(String username) {
        String response = "";
        try {
            URL url = new URL(ApiService.BASE_URL + "get_user_status.php?username=" + URLEncoder.encode(username, "UTF-8"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);

            InputStream is = connection.getInputStream();
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            response = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }

        return response;
    }

    private class LoginTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startActivity(cargaIntent);  // Mostrar la pantalla de carga
        }

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            try {
                URL url = new URL(ApiService.BASE_URL + "login.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "username=" + URLEncoder.encode(username, "UTF-8") +
                        "&password=" + URLEncoder.encode(password, "UTF-8");
                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                InputStream is = connection.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                is.close();

                if (response.startsWith("Conexion Exitosa")) {
                    response = response.substring(response.indexOf("{"));
                }

                return response;
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonResponse = new JSONObject(result);
                String status = jsonResponse.getString("status");
                if (status.equals("success")) {
                    String rol = jsonResponse.getString("rol");
                    String username = jsonResponse.getString("username");
                    int idUsuario = jsonResponse.getInt("idUsuario");  // Asume que estos campos están disponibles
                    int ecoPoints = jsonResponse.getInt("ecoPoints");

                    SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("username", username);
                    editor.putString("rol", rol);
                    editor.putInt("idUsuario", idUsuario);
                    editor.putInt("ecoPoints", ecoPoints);
                    editor.apply();  // Confirmar los cambios

                    Intent intent;
                    switch (rol) {
                        case "usuario":
                            intent = new Intent(LoginUI.this, UserUI.class);
                            break;
                        case "admin":
                            intent = new Intent(LoginUI.this, AdminUI.class);
                            break;
                        case "basurero":
                            intent = new Intent(LoginUI.this, BasureroUI.class);
                            break;
                        default:
                            Toast.makeText(LoginUI.this, "Rol desconocido", Toast.LENGTH_LONG).show();
                            return;
                    }

                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginUI.this, jsonResponse.getString("message"), Toast.LENGTH_LONG).show();
                    regresarALogin();  // Método para regresar a LoginUI
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(LoginUI.this, "Error al procesar la respuesta: " + result, Toast.LENGTH_LONG).show();
                regresarALogin();  // Método para regresar a LoginUI
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
            // Redirigir a la configuración de ubicación
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationEnabled();
            } else {
                Toast.makeText(this, "Se necesita permiso de ubicación para continuar.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void regresarALogin() {
        // Limpiar los campos de texto para evitar que se mantengan los datos ingresados anteriormente
        txtUsername.setText("");
        txtPassword.setText("");
        startActivity(new Intent(LoginUI.this, LoginUI.class));
        finish(); // Finalizar la actividad actual para evitar que el usuario regrese al estado anterior
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity(); // Cierra la aplicación
    }

}