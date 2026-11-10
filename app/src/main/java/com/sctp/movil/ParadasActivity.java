package com.sctp.movil;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sctp.movil.db.ConexionMovil;
import com.sctp.movil.model.ParadaMovil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParadasActivity extends AppCompatActivity
{
    private ProgressBar progressBar;
    private RecyclerView rvParadas;
    private WebView webViewMapa;
    private Button btnVerMapa;
    private Button btnVerLista;

    private final List<ParadaMovil> listaParadas = new ArrayList<>();
    private ParadasAdapter adapter;
    private boolean mapaListo = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paradas);

        progressBar = findViewById(R.id.progressBarParadas);
        rvParadas = findViewById(R.id.rvParadas);
        webViewMapa = findViewById(R.id.webViewMapa);
        btnVerMapa = findViewById(R.id.btnVerMapa);
        btnVerLista = findViewById(R.id.btnVerLista);

        rvParadas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParadasAdapter(listaParadas);
        rvParadas.setAdapter(adapter);

        configurarWebView();
        configurarBotonesPestana();
        cargarParadasDesdeServidor();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarWebView()
    {
        WebSettings settings = webViewMapa.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webViewMapa.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);
                mapaListo = true;
                inyectarParadasAlMapa();
            }
        });

        webViewMapa.loadUrl("file:///android_asset/mapa_movil.html");
    }

    private void configurarBotonesPestana()
    {
        btnVerMapa.setOnClickListener(v ->
        {
            btnVerMapa.setBackgroundTintList(ColorStateList.valueOf(0xff047857));
            btnVerLista.setBackgroundTintList(ColorStateList.valueOf(0xff1e293b));
            webViewMapa.setVisibility(View.VISIBLE);
            rvParadas.setVisibility(View.GONE);
        });

        btnVerLista.setOnClickListener(v ->
        {
            btnVerLista.setBackgroundTintList(ColorStateList.valueOf(0xff047857));
            btnVerMapa.setBackgroundTintList(ColorStateList.valueOf(0xff1e293b));
            rvParadas.setVisibility(View.VISIBLE);
            webViewMapa.setVisibility(View.GONE);
        });
    }

    private void cargarParadasDesdeServidor()
    {
        progressBar.setVisibility(View.VISIBLE);

        ConexionMovil.ejecutar(() ->
        {
            List<ParadaMovil> resultado = new ArrayList<>();
            String sql = "SELECT id_parada, id_ruta, nombre, orden, minutos_desde_origen, latitud, longitud FROM parada ORDER BY orden ASC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    resultado.add(new ParadaMovil(
                            rs.getInt("id_parada"),
                            rs.getInt("id_ruta"),
                            rs.getString("nombre"),
                            rs.getInt("orden"),
                            rs.getInt("minutos_desde_origen"),
                            rs.getDouble("latitud"),
                            rs.getDouble("longitud")
                    ));
                }
            }
            return resultado;
        }, new ConexionMovil.Callback<List<ParadaMovil>>()
        {
            @Override
            public void onSuccess(List<ParadaMovil> result)
            {
                progressBar.setVisibility(View.GONE);
                listaParadas.clear();
                listaParadas.addAll(result);
                adapter.notifyDataSetChanged();

                if (mapaListo)
                {
                    inyectarParadasAlMapa();
                }
            }

            @Override
            public void onError(Exception e)
            {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ParadasActivity.this, "Error al cargar paradas: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void inyectarParadasAlMapa()
    {
        if (listaParadas.isEmpty())
        {
            return;
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < listaParadas.size(); i++)
        {
            ParadaMovil p = listaParadas.get(i);
            json.append(String.format(
                    Locale.US,
                    "{\"idParada\":%d,\"idRuta\":%d,\"nombre\":\"%s\",\"orden\":%d,\"minutosDesdeOrigen\":%d,\"latitud\":%f,\"longitud\":%f}",
                    p.getId(),
                    p.getRutaId(),
                    p.getNombre().replace("\"", "\\\""),
                    p.getOrden(),
                    p.getMinutosDesdeOrigen(),
                    p.getLatitud() != null ? p.getLatitud() : 0.0,
                    p.getLongitud() != null ? p.getLongitud() : 0.0
            ));
            if (i < listaParadas.size() - 1)
            {
                json.append(",");
            }
        }
        json.append("]");

        String script = "javascript:window.cargarParadasJson('" + json.toString().replace("'", "\\'") + "');";
        webViewMapa.evaluateJavascript(script, null);
    }

    private static class ParadasAdapter extends RecyclerView.Adapter<ParadasAdapter.ViewHolder>
    {
        private final List<ParadaMovil> items;

        public ParadasAdapter(List<ParadaMovil> items)
        {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ruta, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            ParadaMovil item = items.get(position);
            holder.lblClave.setText("#" + item.getOrden());
            holder.lblNombre.setText(item.getNombre());
            holder.lblEstado.setText("+" + item.getMinutosDesdeOrigen() + " min");
            holder.lblDetalle.setText("Parada oficial SCTP • Conexión con rutas locales");
        }

        @Override
        public int getItemCount()
        {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder
        {
            TextView lblClave;
            TextView lblNombre;
            TextView lblEstado;
            TextView lblDetalle;

            public ViewHolder(@NonNull View itemView)
            {
                super(itemView);
                lblClave = itemView.findViewById(R.id.lblClaveRuta);
                lblNombre = itemView.findViewById(R.id.lblNombreRuta);
                lblEstado = itemView.findViewById(R.id.lblEstadoRuta);
                lblDetalle = itemView.findViewById(R.id.lblDetalleRuta);
            }
        }
    }
}
