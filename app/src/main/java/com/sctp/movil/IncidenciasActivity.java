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
import com.sctp.movil.model.IncidenciaMovil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class IncidenciasActivity extends AppCompatActivity
{
    private ProgressBar progressBar;
    private RecyclerView rvIncidencias;
    private final List<IncidenciaMovil> listaIncidencias = new ArrayList<>();
    private IncidenciasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidencias);

        progressBar = findViewById(R.id.progressBarIncidencias);
        rvIncidencias = findViewById(R.id.rvIncidencias);

        rvIncidencias.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidenciasAdapter(listaIncidencias);
        rvIncidencias.setAdapter(adapter);

        cargarIncidenciasDesdeServidor();
    }

    private void cargarIncidenciasDesdeServidor()
    {
        progressBar.setVisibility(View.VISIBLE);

        ConexionMovil.ejecutar(() ->
        {
            List<IncidenciaMovil> resultado = new ArrayList<>();
            String sql = "SELECT id, tipo, descripcion, severidad, fecha_hora FROM incidencia WHERE estado != 'CERRADA' ORDER BY fecha_hora DESC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    resultado.add(new IncidenciaMovil(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            rs.getString("descripcion"),
                            rs.getString("severidad"),
                            rs.getString("fecha_hora")
                    ));
                }
            }
            return resultado;
        }, new ConexionMovil.Callback<List<IncidenciaMovil>>()
        {
            @Override
            public void onSuccess(List<IncidenciaMovil> result)
            {
                progressBar.setVisibility(View.GONE);
                listaIncidencias.clear();
                listaIncidencias.addAll(result);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e)
            {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(IncidenciasActivity.this, "Error al cargar incidencias: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class IncidenciasAdapter extends RecyclerView.Adapter<IncidenciasAdapter.ViewHolder>
    {
        private final List<IncidenciaMovil> items;

        public IncidenciasAdapter(List<IncidenciaMovil> items)
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
            IncidenciaMovil item = items.get(position);
            holder.lblClave.setText(item.getTipo());
            holder.lblNombre.setText(item.getDescripcion());
            holder.lblEstado.setText(item.getSeveridad());
            holder.lblDetalle.setText("Fecha: " + item.getFecha());
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
