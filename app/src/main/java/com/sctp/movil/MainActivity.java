package com.sctp.movil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sctp.movil.db.ConexionMovil;

import java.sql.Connection;

public class MainActivity extends AppCompatActivity
{
    private TextView lblEstadoHeader;
    private TextView lblPuntoEstado;
    private AutoCompleteTextView txtBuscarDestino;
    private ImageView btnBorrarBusqueda;
    private ImageView btnIconoBuscar;

    private View cardRutas;
    private View cardParadas;
    private View cardIncidencias;
    private View btnComoLlegar;
    private View cardMapaVista;
    private View btnFavorito;
    private View btnPillRuta;

    // paradas y destinos frecuentes del valle de toluca
    private final String[] destinosSugeridos = new String[]
    {
            "Terminal Toluca",
            "Santiago Tilapa",
            "Lerma",
            "Terminal Tianguistenco",
            "CUUT",
            "UAEMEXCUT",
            "Av. Buenavista y Calz. del Panteón",
            "CU - Ciudad Universitaria",
            "Mercado Juárez",
            "Centro Histórico Toluca",
            "Metepec - Galerías",
            "Ruta R-01: Tianguistenco – Toluca",
            "Ruta R-02: San Mateo Atenco – CU"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // enlazar vistas de cabecera y estado
        lblEstadoHeader = findViewById(R.id.lblEstadoHeader);
        lblPuntoEstado = findViewById(R.id.lblPuntoEstado);

        // enlazar buscador interactivo
        txtBuscarDestino = findViewById(R.id.txtBuscarDestino);
        btnBorrarBusqueda = findViewById(R.id.btnBorrarBusqueda);
        btnIconoBuscar = findViewById(R.id.btnIconoBuscar);

        // enlazar tarjetas y botones
        cardRutas = findViewById(R.id.cardRutas);
        cardParadas = findViewById(R.id.cardParadas);
        cardIncidencias = findViewById(R.id.cardIncidencias);
        btnComoLlegar = findViewById(R.id.btnComoLlegar);
        cardMapaVista = findViewById(R.id.cardMapaVista);
        btnFavorito = findViewById(R.id.btnFavorito);
        btnPillRuta = findViewById(R.id.btnPillRuta);

        configurarBuscador();
        verificarConexionRemota();
        configurarEventos();
    }

    private void configurarBuscador()
    {
        // adaptador de sugerencias en tiempo real
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                destinosSugeridos
        );
        txtBuscarDestino.setAdapter(adapter);

        // seleccion de sugerencia del desplegable
        txtBuscarDestino.setOnItemClickListener((parent, view, position, id) ->
        {
            String destinoSeleccionado = (String) parent.getItemAtPosition(position);
            ejecutarBusqueda(destinoSeleccionado);
        });

        // busqueda al presionar enter o boton buscar del teclado
        txtBuscarDestino.setOnEditorActionListener((v, actionId, event) ->
        {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            {
                ejecutarBusqueda(txtBuscarDestino.getText().toString());
                return true;
            }
            return false;
        });

        // boton para borrar texto escrito
        txtBuscarDestino.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (btnBorrarBusqueda != null)
                {
                    btnBorrarBusqueda.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {
            }
        });

        if (btnBorrarBusqueda != null)
        {
            btnBorrarBusqueda.setOnClickListener(v -> txtBuscarDestino.setText(""));
        }

        if (btnIconoBuscar != null)
        {
            btnIconoBuscar.setOnClickListener(v -> ejecutarBusqueda(txtBuscarDestino.getText().toString()));
        }
    }

    private void ejecutarBusqueda(String consulta)
    {
        if (consulta == null || consulta.trim().isEmpty())
        {
            Toast.makeText(MainActivity.this, "Escribe una parada, ruta o destino", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, ParadasActivity.class);
        intent.putExtra("busqueda", consulta.trim());
        startActivity(intent);
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
                intent.putExtra("busqueda", "Av. Buenavista");
                startActivity(intent);
            });
        }

        // pill de ruta olivo cu
        if (btnPillRuta != null)
        {
            btnPillRuta.setOnClickListener(v ->
            {
                Intent intent = new Intent(MainActivity.this, RutasActivity.class);
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
        if (lblEstadoHeader != null)
        {
            lblEstadoHeader.setText("Conectando con servidor...");
        }
        if (lblPuntoEstado != null)
        {
            lblPuntoEstado.setTextColor(0xfffacc15);
        }

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
                    if (lblEstadoHeader != null)
                    {
                        lblEstadoHeader.setText("En línea · actualizado hace 1 min");
                    }
                    if (lblPuntoEstado != null)
                    {
                        lblPuntoEstado.setTextColor(0xff4ade80);
                    }
                }
            }

            @Override
            public void onError(Exception e)
            {
                if (lblEstadoHeader != null)
                {
                    lblEstadoHeader.setText("Sin conexión remota · Modo local");
                }
                if (lblPuntoEstado != null)
                {
                    lblPuntoEstado.setTextColor(0xfff87171);
                }
            }
        });
    }
}
