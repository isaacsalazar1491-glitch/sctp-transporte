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
        adapter = new RutasAdapter(listaRutas);
        rvRutas.setAdapter(adapter);

        cargarRutasDesdeServidor();
    }

    private void cargarRutasDesdeServidor()
    {
        progressBar.setVisibility(View.VISIBLE);

        ConexionMovil.ejecutar(() ->
        {
            List<RutaMovil> resultado = new ArrayList<>();
            String sql = "SELECT id, clave, nombre, estado FROM ruta WHERE activo = true ORDER BY clave ASC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    resultado.add(new RutaMovil(
                            rs.getInt("id"),
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

    private static class RutasAdapter extends RecyclerView.Adapter<RutasAdapter.ViewHolder>
    {
        private final List<RutaMovil> items;

        public RutasAdapter(List<RutaMovil> items)
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
            RutaMovil item = items.get(position);
            holder.lblClave.setText(item.getClave());
            holder.lblNombre.setText(item.getNombre());
            holder.lblEstado.setText(item.getEstado());
            holder.lblDetalle.setText("Ruta activa de transporte público SCTP");
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

