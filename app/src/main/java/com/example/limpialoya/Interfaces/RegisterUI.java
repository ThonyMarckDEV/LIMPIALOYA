package com.example.limpialoya.Interfaces;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterUI extends AppCompatActivity {

    private EditText txtUsername, txtNombres, txtApellidos, txtEmail, txtPassword;
    private Button btnRegistrarse;
    private Intent cargaIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_ui);

        // Referencias a los campos del formulario
        txtUsername = findViewById(R.id.txtUsernameRegister);
        txtNombres = findViewById(R.id.txtNombresRegister);
        txtApellidos = findViewById(R.id.txtApellidosRegister);
        txtEmail = findViewById(R.id.txtEmailRegister);
        txtPassword = findViewById(R.id.txtPasswordRegister);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);

        // Inicializar la Intent para la pantalla de carga
        cargaIntent = new Intent(RegisterUI.this, CargaUI.class);

        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Recuperar los valores de los campos de texto
                String username = txtUsername.getText().toString().trim();
                String nombres = txtNombres.getText().toString().trim();
                String apellidos = txtApellidos.getText().toString().trim();
                String email = txtEmail.getText().toString().trim();
                String password = txtPassword.getText().toString().trim();

                // Validaciones de los campos
                if (username.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterUI.this, "Por favor, complete todos los campos", Toast.LENGTH_LONG).show();
                } else if (!isValidEmail(email)) {
                    Toast.makeText(RegisterUI.this, "Por favor, ingrese un email válido", Toast.LENGTH_LONG).show();
                } else if (!isValidPassword(password)) {
                    Toast.makeText(RegisterUI.this, "La contraseña debe tener al menos 8 caracteres y contener una mayúscula o un símbolo", Toast.LENGTH_LONG).show();
                } else {
                    // Si todas las validaciones son correctas, ejecutar la tarea de registro
                    new RegisterTask().execute(username, nombres, apellidos, email, password);
                }
            }
        });

    }

    // Método para validar el email
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    // Método para validar la contraseña
    private boolean isValidPassword(String password) {
        return password.length() >= 8;  // Verifica que la contraseña tenga al menos 8 caracteres
    }


    private class RegisterTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Mostrar la pantalla de carga
            startActivity(cargaIntent);
        }

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String nombres = params[1];
            String apellidos = params[2];
            String email = params[3];
            String password = params[4];

            try {
                URL url = new URL(ApiService.BASE_URL + "register.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Enviar los datos al servidor
                String postData = "username=" + username +
                        "&nombres=" + nombres +
                        "&apellidos=" + apellidos +
                        "&email=" + email +
                        "&password=" + password;

                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // Leer la respuesta del servidor
                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return response.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return "Excepción: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                // Parsear la respuesta como JSON
                JSONObject jsonResponse = new JSONObject(result);
                String status = jsonResponse.getString("status");
                String message = jsonResponse.getString("message");

                if (status.equals("success")) {
                    Toast.makeText(RegisterUI.this, message, Toast.LENGTH_LONG).show();
                    // Registro exitoso, redirigir a LoginUI
                    Intent intent = new Intent(RegisterUI.this, LoginUI.class);
                    startActivity(intent);
                    finish();
                } else if (status.equals("error")) {
                    // Mostrar el error y redirigir a LoginUI
                    Toast.makeText(RegisterUI.this, message, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterUI.this, LoginUI.class);
                    startActivity(intent);
                    finish();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(RegisterUI.this, "Error en el registro", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Mostrar la pantalla de carga antes de volver a LoginUI
        Intent cargaIntent = new Intent(RegisterUI.this, CargaUI.class);
        startActivity(cargaIntent);

        // Usar Handler para retrasar el inicio de LoginUI y permitir que la pantalla de carga se muestre
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RegisterUI.this, LoginUI.class);
                startActivity(intent);
                finish();
            }
        }, 500); // Esperar 500 ms antes de iniciar LoginUI
    }
}
