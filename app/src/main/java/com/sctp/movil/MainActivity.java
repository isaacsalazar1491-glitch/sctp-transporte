package com.sctp.movil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sctp.movil.db.ConexionMovil;

import java.sql.Connection;

public class MainActivity extends AppCompatActivity
{
    private TextView lblEstadoConexion;
    private View cardRutas;
    private View cardParadas;
    private View cardIncidencias;
    private View btnComoLlegar;
    private View btnBuscarDestino;
    private View cardMapaVista;
    private View btnFavorito;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // enlace de vistas principales
        lblEstadoConexion = findViewById(R.id.lblEstadoConexion);
        cardRutas = findViewById(R.id.cardRutas);
        cardParadas = findViewById(R.id.cardParadas);
        cardIncidencias = findViewById(R.id.cardIncidencias);
        btnComoLlegar = findViewById(R.id.btnComoLlegar);
        btnBuscarDestino = findViewById(R.id.btnBuscarDestino);
        cardMapaVista = findViewById(R.id.cardMapaVista);
        btnFavorito = findViewById(R.id.btnFavorito);

        verificarConexionRemota();
        configurarEventos();
    }

    private void configurarEventos()
    {
        // navegacion a lista de rutas
        if (cardRutas != null)
        {
            cardRutas.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, RutasActivity.class);
                startActivity(intent);
            });
        }

        // navegacion a paradas y mapa
        if (cardParadas != null)
        {
            cardParadas.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, ParadasActivity.class);
                startActivity(intent);
            });
        }

        // navegacion a incidencias y alertas
        if (cardIncidencias != null)
        {
            cardIncidencias.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, IncidenciasActivity.class);
                startActivity(intent);
            });
        }

        // boton como llegar abre paradas y mapa
        if (btnComoLlegar != null)
        {
            btnComoLlegar.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, ParadasActivity.class);
                startActivity(intent);
            });
        }

        // clic en la tarjeta de mapa
        if (cardMapaVista != null)
        {
            cardMapaVista.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, ParadasActivity.class);
                startActivity(intent);
            });
        }

        // clic en la barra de busqueda
        if (btnBuscarDestino != null)
        {
            btnBuscarDestino.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, ParadasActivity.class);
                startActivity(intent);
            });
        }

        // boton de favoritos
        if (btnFavorito != null)
        {
            btnFavorito.setOnClickListener(v ->
            {
                Toast.makeText(MainActivity.this, "Parada guardada en favoritos", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void verificarConexionRemota()
    {
        lblEstadoConexion.setText("● Conectando a PostgreSQL (100.127.110.88)...");
        lblEstadoConexion.setBackgroundColor(0xfffef3c7);
        lblEstadoConexion.setTextColor(0xff92400e);

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
                    lblEstadoConexion.setText("● Conectado a servidor PostgreSQL");
                    lblEstadoConexion.setBackgroundColor(0xffdcfce7);
                    lblEstadoConexion.setTextColor(0xff166534);
                }
            }

            @Override
            public void onError(Exception e)
            {
                lblEstadoConexion.setText("● Modo fuera de línea");
                lblEstadoConexion.setBackgroundColor(0xfffee2e2);
                lblEstadoConexion.setTextColor(0xff991b1b);
            }
        });
    }
}
