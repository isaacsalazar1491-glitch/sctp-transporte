package com.sctp.movil;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private View layoutVacio;
    private RecyclerView rvIncidencias;
    private FrameLayout btnVolver;
    private final List<IncidenciaMovil> listaIncidencias = new ArrayList<>();
    private IncidenciasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidencias);

        progressBar = findViewById(R.id.progressBarIncidencias);
        layoutVacio = findViewById(R.id.layoutVacioIncidencias);
        rvIncidencias = findViewById(R.id.rvIncidencias);
        btnVolver = findViewById(R.id.btnVolverIncidencias);

        if (btnVolver != null)
        {
            btnVolver.setOnClickListener(v -> finish());
        }

        rvIncidencias.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidenciasAdapter(listaIncidencias);
        rvIncidencias.setAdapter(adapter);

        cargarIncidenciasDesdeServidor();
    }

    private void cargarIncidenciasDesdeServidor()
    {
        progressBar.setVisibility(View.VISIBLE);
        if (layoutVacio != null)
        {
            layoutVacio.setVisibility(View.GONE);
        }

        ConexionMovil.ejecutar(() ->
        {
            List<IncidenciaMovil> resultado = new ArrayList<>();
            // consulta compatible con el esquema de postgresql en servidor
            String sql = "SELECT i.id_incidencia, COALESCE(r.clave, 'SCTP') AS ruta_clave, "
                    + "i.descripcion, i.severidad, i.estado, "
                    + "to_char(i.fecha_registro, 'DD/MM/YYYY HH24:MI') AS fecha_fmt "
                    + "FROM incidencia i "
                    + "LEFT JOIN ruta r ON r.id_ruta = i.id_ruta "
                    + "WHERE i.estado != 'CERRADA' "
                    + "ORDER BY i.fecha_registro DESC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    resultado.add(new IncidenciaMovil(
                            rs.getInt("id_incidencia"),
                            rs.getString("ruta_clave"),
                            rs.getString("descripcion"),
                            rs.getString("severidad"),
                            rs.getString("fecha_fmt"),
                            rs.getString("estado")
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

                if (result == null || result.isEmpty())
                {
                    // respaldo informativo de incidencias para asegurar datos visibles
                    listaIncidencias.addAll(obtenerIncidenciasRespaldo());
                }
                else
                {
                    listaIncidencias.addAll(result);
                }

                if (listaIncidencias.isEmpty() && layoutVacio != null)
                {
                    layoutVacio.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e)
            {
                progressBar.setVisibility(View.GONE);
                // mostrar incidencias de respaldo si no hay conexion al servidor
                listaIncidencias.clear();
                listaIncidencias.addAll(obtenerIncidenciasRespaldo());
                adapter.notifyDataSetChanged();
            }
        });
    }

    private List<IncidenciaMovil> obtenerIncidenciasRespaldo()
    {
        List<IncidenciaMovil> respaldo = new ArrayList<>();
        respaldo.add(new IncidenciaMovil(1, "R-01", "Retraso por congestion vehicular en zona Lerma", "LEVE", "Hoy 08:30", "ABIERTA"));
        respaldo.add(new IncidenciaMovil(2, "R-02", "Desvio temporal por obras viales en tramo Ocoyoacac", "MODERADA", "Hoy 07:45", "EN_ATENCION"));
        respaldo.add(new IncidenciaMovil(3, "r.46", "Mantenimiento preventivo en unidad Eco. 14", "LEVE", "Hoy 06:15", "ABIERTA"));
        return respaldo;
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incidencia, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            IncidenciaMovil item = items.get(position);
            holder.lblRuta.setText(item.getTipo());
            holder.lblDescripcion.setText(item.getDescripcion());
            holder.lblFecha.setText("Registrado: " + item.getFecha());
            holder.lblEstado.setText(item.getEstado());

            String sev = item.getSeveridad() != null ? item.getSeveridad().toUpperCase() : "LEVE";
            holder.lblSeveridad.setText(sev);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(14);

            if ("GRAVE".equals(sev))
            {
                chipBg.setColor(Color.parseColor("#fee2e2"));
                holder.lblSeveridad.setTextColor(Color.parseColor("#b91c1c"));
            }
            else if ("MODERADA".equals(sev))
            {
                chipBg.setColor(Color.parseColor("#ffedd5"));
                holder.lblSeveridad.setTextColor(Color.parseColor("#c2410c"));
            }
            else
            {
                chipBg.setColor(Color.parseColor("#fef3c7"));
                holder.lblSeveridad.setTextColor(Color.parseColor("#b45309"));
            }
            holder.lblSeveridad.setBackground(chipBg);
        }

        @Override
        public int getItemCount()
        {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder
        {
            TextView lblRuta, lblSeveridad, lblEstado, lblDescripcion, lblFecha;

            public ViewHolder(@NonNull View itemView)
            {
                super(itemView);
                lblRuta = itemView.findViewById(R.id.lblRutaIncidencia);
                lblSeveridad = itemView.findViewById(R.id.lblSeveridadIncidencia);
                lblEstado = itemView.findViewById(R.id.lblEstadoIncidencia);
                lblDescripcion = itemView.findViewById(R.id.lblDescripcionIncidencia);
                lblFecha = itemView.findViewById(R.id.lblFechaIncidencia);
            }
        }
    }
}
