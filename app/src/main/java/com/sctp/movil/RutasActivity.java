package com.sctp.movil;

import android.content.Intent;
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
import com.sctp.movil.model.RutaMovil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RutasActivity extends AppCompatActivity
{
    private ProgressBar progressBar;
    private RecyclerView rvRutas;
    private final List<RutaMovil> listaRutas = new ArrayList<>();
    private RutasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutas);

        progressBar = findViewById(R.id.progressBarRutas);
        rvRutas = findViewById(R.id.rvRutas);

        rvRutas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RutasAdapter(listaRutas, ruta ->
        {
            Intent intent = new Intent(RutasActivity.this, ParadasActivity.class);
            intent.putExtra("id_ruta", ruta.getId());
            intent.putExtra("nombre_ruta", ruta.getNombre());
            intent.putExtra("clave_ruta", ruta.getClave());
            startActivity(intent);
        });
        rvRutas.setAdapter(adapter);

        cargarRutasDesdeServidor();
    }

    private void cargarRutasDesdeServidor()
    {
        progressBar.setVisibility(View.VISIBLE);

        ConexionMovil.ejecutar(() ->
        {
            List<RutaMovil> resultado = new ArrayList<>();
            String sql = "SELECT id_ruta, clave, nombre, origen, destino, estado FROM ruta WHERE estado = 'ACTIVA' ORDER BY clave ASC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    resultado.add(new RutaMovil(
                            rs.getInt("id_ruta"),
                            rs.getString("clave"),
                            rs.getString("nombre"),
                            rs.getString("estado")
                    ));
                }
            }
            return resultado;
        }, new ConexionMovil.Callback<List<RutaMovil>>()
        {
            @Override
            public void onSuccess(List<RutaMovil> result)
            {
                progressBar.setVisibility(View.GONE);
                listaRutas.clear();
                listaRutas.addAll(result);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e)
            {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RutasActivity.this, "Error al cargar rutas: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public interface OnRutaClickListener
    {
        void onRutaClick(RutaMovil ruta);
    }

    private static class RutasAdapter extends RecyclerView.Adapter<RutasAdapter.ViewHolder>
    {
        private final List<RutaMovil> items;
        private final OnRutaClickListener listener;

        public RutasAdapter(List<RutaMovil> items, OnRutaClickListener listener)
        {
            this.items = items;
            this.listener = listener;
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
            RutaMovil item = items.get(position);
            holder.lblClave.setText(item.getClave());
            holder.lblNombre.setText(item.getNombre());
            holder.lblEstado.setText(item.getEstado());
            holder.lblDetalle.setText("Toca para ver recorrido, paradas y autobús en vivo");

            holder.itemView.setOnClickListener(v ->
            {
                if (listener != null)
                {
                    listener.onRutaClick(item);
                }
            });
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
