package com.example.limpialoya.Interfaces;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import com.example.limpialoya.R;
import com.example.limpialoya.Servicios.ApiService;

public class RankingUI extends AppCompatActivity {
    // =================================================================================================
    //NAVBAR
    // =================================================================================================
    private ImageButton btnPerfil,btnReporte;
    // =================================================================================================

    private LinearLayout llContainer;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking_ui);


        // =================================================================================================
        //NAVBAR
        // =================================================================================================
        // Encontrar los botones de la barra de navegación
        btnReporte = findViewById(R.id.btnReportar);
        btnPerfil = findViewById(R.id.btnPerfil);

        // Configurar el botón reporte
        btnReporte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(RankingUI.this, ReportarUI.class);
                startActivity(mapIntent);
                finish();  // Finaliza la actividad actual para que no se quede en la pila
            }
        });

        // Configurar el botón rankings
        btnPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(RankingUI.this, UserUI.class);
                startActivity(mapIntent);
                finish();  // Finaliza la actividad actual para que no se quede en la pila
            }
        });

        // =================================================================================================


        llContainer = findViewById(R.id.llContainer);
        new FetchRankingTask().execute();

    }

    private class FetchRankingTask extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(RankingUI.this);
            progressDialog.setMessage("Cargando rankings...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            try {
                URL url = new URL(ApiService.BASE_URL + "get_ranking.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream is = connection.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                is.close();

                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.getString("status").equals("success")) {
                    return jsonResponse.getJSONArray("usuarios");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray usuarios) {
            super.onPostExecute(usuarios);
            progressDialog.dismiss();
            if (usuarios == null) return;

            try {
                LayoutInflater inflater = LayoutInflater.from(RankingUI.this);
                for (int i = 0; i < usuarios.length(); i++) {
                    JSONObject usuario = usuarios.getJSONObject(i);
                    View userView = inflater.inflate(R.layout.ranking_user_item, llContainer, false);

                    TextView txtNombre = userView.findViewById(R.id.txtNombre);
                    TextView txtEcoPoints = userView.findViewById(R.id.txtEcoPoints);
                    ImageView imgPerfil = userView.findViewById(R.id.imgPerfil);

                    txtNombre.setText(usuario.getString("username"));
                    txtEcoPoints.setText(usuario.getInt("ecoPoints") + " EcoPoints");

                    String imageUrl = usuario.optString("perfil");
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        imgPerfil.setImageResource(R.drawable.logo); // Imagen por defecto
                    } else {
                        Picasso.get().load(imageUrl).error(R.drawable.logo).into(imgPerfil);
                    }

                    llContainer.addView(userView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RankingUI.this, UserUI.class);
        startActivity(intent);
        finish();
    }

}