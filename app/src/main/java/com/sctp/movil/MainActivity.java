package com.sctp.movil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sctp.movil.db.ConexionMovil;

import java.sql.Connection;

public class MainActivity extends AppCompatActivity
{
    private TextView lblEstadoConexion;
    private Button btnIrRutas;
    private Button btnIrParadas;
    private Button btnIrIncidencias;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblEstadoConexion = findViewById(R.id.lblEstadoConexion);
        btnIrRutas = findViewById(R.id.btnIrRutas);
        btnIrParadas = findViewById(R.id.btnIrParadas);
        btnIrIncidencias = findViewById(R.id.btnIrIncidencias);

        verificarConexionRemota();

        btnIrRutas.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, RutasActivity.class);
            startActivity(intent);
        });

        btnIrParadas.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, ParadasActivity.class);
            startActivity(intent);
        });

        btnIrIncidencias.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, IncidenciasActivity.class);
            startActivity(intent);
        });
    }

    private void verificarConexionRemota()
    {
        lblEstadoConexion.setText("● Conectando a PostgreSQL (100.127.110.88)...");
        lblEstadoConexion.setBackgroundColor(0xffd97706);

        ConexionMovil.ejecutar(() ->
        {
            try (Connection con = ConexionMovil.obtenerConexionDirecta())
            {
                return con != null && !con.isClosed();
            }
        }, new ConexionMovil.Callback<Boolean>()
        {
            @Override
            public void onSuccess(Boolean conectado)
            {
                if (conectado)
                {
                    lblEstadoConexion.setText("● Conectado a servidor PostgreSQL (100.127.110.88)");
                    lblEstadoConexion.setBackgroundColor(0xff14532d);
                }
            }

            @Override
            public void onError(Exception e)
            {
                lblEstadoConexion.setText("● Error de conexión: " + e.getMessage());
                lblEstadoConexion.setBackgroundColor(0xffb91c1c);
                Toast.makeText(MainActivity.this, "No se pudo conectar al servidor: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

