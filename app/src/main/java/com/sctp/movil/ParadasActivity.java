package com.sctp.movil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ParadasActivity extends AppCompatActivity
{
    private ProgressBar progressBar;
    private RecyclerView rvParadas;
    private final List<ParadaMovil> listaParadas = new ArrayList<>();
    private ParadasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paradas);

        progressBar = findViewById(R.id.progressBarParadas);
        rvParadas = findViewById(R.id.rvParadas);

        rvParadas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParadasAdapter(listaParadas);
        rvParadas.setAdapter(adapter);

        cargarParadasDesdeServidor();
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
            }

            @Override
            public void onError(Exception e)
            {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ParadasActivity.this, "Error al cargar paradas: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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
            TextView lblClave, lblNombre, lblEstado, lblDetalle;

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

