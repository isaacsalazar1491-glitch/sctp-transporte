package com.sctp.movil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sctp.movil.db.ConexionMovil;
import com.sctp.movil.model.ParadaMovil;
import com.sctp.movil.model.RutaMovil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParadasActivity extends AppCompatActivity
{
    private static final int PERMISSION_LOCATION_REQUEST = 101;

    private ProgressBar progressBar;
    private RecyclerView rvParadas;
    private WebView webViewMapa;

    // cabecera y pestañas
    private View btnVolverAtras;
    private TextView lblTituloRutas;
    private TextView lblEstadoGps;
    private View btnVerMapa;
    private View btnVerLista;
    private TextView lblTabMapa;
    private TextView lblTabParadas;

    // selector horizontal dinamico de rutas
    private LinearLayout layoutPillsRutas;

    // tarjeta de llegada del autobus
    private View layoutInfoAutobus;
    private TextView lblUnidadEstado;
    private TextView lblRutaSeleccionada;
    private TextView lblTiempoNumero;
    private View barraProgresoTrayecto;
    private TextView lblEstimacionLlegada;
    private View fabMiUbicacion;

    // barra inferior dock
    private View btnNavHome;
    private View btnNavRutas;
    private View btnNavMapa;
    private View btnNavAlertas;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location ultimaUbicacion;

    private final List<ParadaMovil> listaParadas = new ArrayList<>();
    private final List<RutaMovil> listaRutasDisponibles = new ArrayList<>();
    private ParadasAdapter adapter;
    private boolean mapaListo = false;
    private int idRutaFiltro = 1;
    private String nombreRutaFiltro = "Tianguistenco – Toluca";
    private String claveRutaFiltro = "R-01";
    private String busquedaDestino = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paradas);

        // obtener parametros enviados desde otras actividades
        int idParam = getIntent().getIntExtra("id_ruta", 0);
        if (idParam > 0)
        {
            idRutaFiltro = idParam;
        }
        String nomParam = getIntent().getStringExtra("nombre_ruta");
        if (nomParam != null)
        {
            nombreRutaFiltro = nomParam;
        }
        String cveParam = getIntent().getStringExtra("clave_ruta");
        if (cveParam != null)
        {
            claveRutaFiltro = cveParam;
        }
        busquedaDestino = getIntent().getStringExtra("busqueda");

        enlazarVistas();
        configurarWebView();
        configurarBotonesPestana();
        configurarNavegacionInferior();
        configurarGps();

        cargarRutasDinamicas();
        cargarParadasDesdeServidor();
    }

    private void enlazarVistas()
    {
        progressBar = findViewById(R.id.progressBarParadas);
        rvParadas = findViewById(R.id.rvParadas);
        webViewMapa = findViewById(R.id.webViewMapa);

        btnVolverAtras = findViewById(R.id.btnVolverAtras);
        lblTituloRutas = findViewById(R.id.lblTituloRutas);
        lblEstadoGps = findViewById(R.id.lblEstadoGps);

        btnVerMapa = findViewById(R.id.btnVerMapa);
        btnVerLista = findViewById(R.id.btnVerLista);
        lblTabMapa = findViewById(R.id.lblTabMapa);
        lblTabParadas = findViewById(R.id.lblTabParadas);

        layoutPillsRutas = findViewById(R.id.layoutPillsRutas);

        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        layoutInfoAutobus = findViewById(R.id.layoutInfoAutobus);
        lblUnidadEstado = findViewById(R.id.lblUnidadEstado);
        lblRutaSeleccionada = findViewById(R.id.lblRutaSeleccionada);
        lblTiempoNumero = findViewById(R.id.lblTiempoNumero);
        barraProgresoTrayecto = findViewById(R.id.barraProgresoTrayecto);
        lblEstimacionLlegada = findViewById(R.id.lblEstimacionLlegada);

        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavRutas = findViewById(R.id.btnNavRutas);
        btnNavMapa = findViewById(R.id.btnNavMapa);
        btnNavAlertas = findViewById(R.id.btnNavAlertas);

        rvParadas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParadasAdapter(listaParadas);
        rvParadas.setAdapter(adapter);

        if (btnVolverAtras != null)
        {
            btnVolverAtras.setOnClickListener(v -> finish());
        }
    }

    private void cargarRutasDinamicas()
    {
        ConexionMovil.ejecutar(() ->
        {
            List<RutaMovil> rutas = new ArrayList<>();
            String sql = "SELECT id_ruta, clave, nombre, destino FROM ruta WHERE estado = 'ACTIVA' ORDER BY id_ruta ASC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    rutas.add(new RutaMovil(
                            rs.getInt("id_ruta"),
                            rs.getString("clave"),
                            rs.getString("nombre"),
                            rs.getString("destino")
                    ));
                }
            }
            return rutas;
        }, new ConexionMovil.Callback<List<RutaMovil>>()
        {
            @Override
            public void onSuccess(List<RutaMovil> result)
            {
                listaRutasDisponibles.clear();
                listaRutasDisponibles.addAll(result);

                // si la ruta activa actual no esta en la lista, seleccionar la primera
                if (!listaRutasDisponibles.isEmpty())
                {
                    boolean existe = false;
                    for (RutaMovil r : listaRutasDisponibles)
                    {
                        if (r.getId() == idRutaFiltro)
                        {
                            existe = true;
                            nombreRutaFiltro = r.getNombre();
                            claveRutaFiltro = r.getClave();
                            break;
                        }
                    }
                    if (!existe)
                    {
                        RutaMovil primera = listaRutasDisponibles.get(0);
                        idRutaFiltro = primera.getId();
                        nombreRutaFiltro = primera.getNombre();
                        claveRutaFiltro = primera.getClave();
                    }
                }

                generarPillsDinamicas();
                actualizarTextosRutaActiva();
            }

            @Override
            public void onError(Exception e)
            {
                // mantener valores por defecto si no hay conexion
                actualizarTextosRutaActiva();
            }
        });
    }

    private void generarPillsDinamicas()
    {
        if (layoutPillsRutas == null) return;
        layoutPillsRutas.removeAllViews();

        for (RutaMovil r : listaRutasDisponibles)
        {
            TextView pill = new TextView(this);
            pill.setText(r.getClave());
            pill.setTextSize(14f);
            pill.setTypeface(null, android.graphics.Typeface.BOLD);
            pill.setClickable(true);
            pill.setFocusable(true);

            int padH = dpToPx(20);
            int padV = dpToPx(9);
            pill.setPadding(padH, padV, padH, padV);

            boolean seleccionada = (r.getId() == idRutaFiltro);
            pill.setBackgroundResource(seleccionada ? R.drawable.bg_pill_route_selected : R.drawable.bg_pill_route_unselected);
            pill.setTextColor(seleccionada ? 0xffffffff : 0xff0f172a);
            pill.setElevation(seleccionada ? dpToPx(3) : dpToPx(2));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(dpToPx(10));
            pill.setLayoutParams(lp);

            pill.setOnClickListener(v -> cambiarRutaActiva(r.getId(), r.getClave(), r.getNombre()));
            layoutPillsRutas.addView(pill);
        }
    }

    private int dpToPx(int dp)
    {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void cambiarRutaActiva(int nuevoIdRuta, String nuevaClave, String nuevoNombre)
    {
        if (idRutaFiltro == nuevoIdRuta) return;
        idRutaFiltro = nuevoIdRuta;
        claveRutaFiltro = nuevaClave;
        nombreRutaFiltro = nuevoNombre;

        generarPillsDinamicas();
        actualizarTextosRutaActiva();
        cargarParadasDesdeServidor();
    }

    private void actualizarTextosRutaActiva()
    {
        // buscar destino de la ruta activa
        String destino = null;
        for (RutaMovil r : listaRutasDisponibles)
        {
            if (r.getId() == idRutaFiltro)
            {
                destino = r.getEstado(); // almacenamos el destino temporalmente en estado
                break;
            }
        }

        if (destino != null && !destino.isEmpty())
        {
            lblRutaSeleccionada.setText("Llega a " + destino);
        }
        else if (nombreRutaFiltro != null)
        {
            lblRutaSeleccionada.setText("Llega a " + nombreRutaFiltro);
        }
        else
        {
            lblRutaSeleccionada.setText("Llega a Terminal Toluca");
        }

        if (busquedaDestino != null && !busquedaDestino.trim().isEmpty())
        {
            lblRutaSeleccionada.setText("Llega a " + busquedaDestino);
        }
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

        // permitir acceso de origen cruzado para consultas osrm desde assets locales
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        webViewMapa.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage)
            {
                android.util.Log.d("MAPA_JS", consoleMessage.message() + " [" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "]");
                return true;
            }
        });

        webViewMapa.addJavascriptInterface(new BridgeJS(), "AndroidBridge");

        webViewMapa.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);
                mapaListo = true;
                if (!listaParadas.isEmpty())
                {
                    inyectarParadasAlMapa();
                }
                if (ultimaUbicacion != null)
                {
                    enviarUbicacionAlMapa(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude(), false);
                }
            }
        });

        webViewMapa.loadUrl("file:///android_asset/mapa_movil.html");
    }

    private void configurarBotonesPestana()
    {
        btnVerMapa.setOnClickListener(v ->
        {
            btnVerMapa.setBackgroundResource(R.drawable.bg_tab_active_yellow);
            lblTabMapa.setTextColor(0xff000000);
            btnVerLista.setBackgroundResource(android.R.color.transparent);
            lblTabParadas.setTextColor(0xffffffff);

            webViewMapa.setVisibility(View.VISIBLE);
            layoutInfoAutobus.setVisibility(View.VISIBLE);
            fabMiUbicacion.setVisibility(View.VISIBLE);
            if (layoutPillsRutas != null) layoutPillsRutas.setVisibility(View.VISIBLE);
            rvParadas.setVisibility(View.GONE);
        });

        btnVerLista.setOnClickListener(v ->
        {
            btnVerLista.setBackgroundResource(R.drawable.bg_tab_active_yellow);
            lblTabParadas.setTextColor(0xff000000);
            btnVerMapa.setBackgroundResource(android.R.color.transparent);
            lblTabMapa.setTextColor(0xffffffff);

            rvParadas.setVisibility(View.VISIBLE);
            webViewMapa.setVisibility(View.GONE);
            layoutInfoAutobus.setVisibility(View.GONE);
            fabMiUbicacion.setVisibility(View.GONE);
        });

        fabMiUbicacion.setOnClickListener(v ->
        {
            if (ultimaUbicacion != null)
            {
                enviarUbicacionAlMapa(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude(), true);
                Toast.makeText(this, "Centrado en tu ubicación GPS", Toast.LENGTH_SHORT).show();
            }
            else
            {
                solicitarPermisoUbicacion();
            }
        });
    }

    private void configurarNavegacionInferior()
    {
        if (btnNavHome != null)
        {
            btnNavHome.setOnClickListener(v ->
            {
                Intent intent = new Intent(ParadasActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (btnNavRutas != null)
        {
            btnNavRutas.setOnClickListener(v ->
            {
                Intent intent = new Intent(ParadasActivity.this, RutasActivity.class);
                startActivity(intent);
            });
        }

        if (btnNavAlertas != null)
        {
            btnNavAlertas.setOnClickListener(v ->
            {
                Intent intent = new Intent(ParadasActivity.this, IncidenciasActivity.class);
                startActivity(intent);
            });
        }
    }

    private void configurarGps()
    {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener()
        {
            @Override
            public void onLocationChanged(@NonNull Location location)
            {
                ultimaUbicacion = location;
                lblEstadoGps.setText("Ubicación activa");
                if (mapaListo)
                {
                    enviarUbicacionAlMapa(location.getLatitude(), location.getLongitude(), false);
                }
            }

            @Override
            public void onProviderEnabled(@NonNull String provider)
            {
                lblEstadoGps.setText("Ubicación activa");
            }

            @Override
            public void onProviderDisabled(@NonNull String provider)
            {
                lblEstadoGps.setText("Activa el GPS");
            }
        };

        solicitarPermisoUbicacion();
    }

    private void solicitarPermisoUbicacion()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_LOCATION_REQUEST
            );
            return;
        }

        iniciarLecturaGps();
    }

    @SuppressLint("MissingPermission")
    private void iniciarLecturaGps()
    {
        try
        {
            if (locationManager == null) return;

            Location locGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location loc = locGps != null ? locGps : locNet;

            if (loc != null)
            {
                ultimaUbicacion = loc;
                lblEstadoGps.setText("Ubicación activa");
                if (mapaListo)
                {
                    enviarUbicacionAlMapa(loc.getLatitude(), loc.getLongitude(), false);
                }
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 5, locationListener);
        }
        catch (Exception e)
        {
            lblEstadoGps.setText("GPS activo");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_LOCATION_REQUEST)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                iniciarLecturaGps();
            }
        }
    }

    private void enviarUbicacionAlMapa(double lat, double lng, boolean centrar)
    {
        String script = String.format(
                Locale.US,
                "javascript:window.actualizarUbicacionGps(%f, %f, 'Coordenadas en vivo', %b);",
                lat,
                lng,
                centrar
        );
        webViewMapa.evaluateJavascript(script, null);
    }

    private void cargarParadasDesdeServidor()
    {
        progressBar.setVisibility(View.VISIBLE);

        ConexionMovil.ejecutar(() ->
        {
            List<ParadaMovil> resultado = new ArrayList<>();
            // filtrar paradas con coordenadas validas de la ruta activa
            String sql = "SELECT id_parada, id_ruta, nombre, orden, minutos_desde_origen, latitud, longitud " +
                    "FROM parada WHERE id_ruta = ? AND latitud IS NOT NULL AND longitud IS NOT NULL ORDER BY orden ASC";

            try (Connection con = ConexionMovil.obtenerConexionDirecta();
                 PreparedStatement ps = con.prepareStatement(sql))
            {
                ps.setInt(1, idRutaFiltro);

                try (ResultSet rs = ps.executeQuery())
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
        boolean primero = true;
        for (int i = 0; i < listaParadas.size(); i++)
        {
            ParadaMovil p = listaParadas.get(i);
            if (p.getLatitud() == null || p.getLongitud() == null) continue;

            if (!primero)
            {
                json.append(",");
            }
            primero = false;

            json.append(String.format(
                    Locale.US,
                    "{\"idParada\":%d,\"idRuta\":%d,\"nombre\":\"%s\",\"orden\":%d,\"minutosDesdeOrigen\":%d,\"latitud\":%f,\"longitud\":%f}",
                    p.getId(),
                    p.getRutaId(),
                    p.getNombre().replace("\"", "\\\"").replace("'", "\\'"),
                    p.getOrden(),
                    p.getMinutosDesdeOrigen(),
                    p.getLatitud(),
                    p.getLongitud()
            ));
        }
        json.append("]");

        // inyectar directamente como objeto javascript seguro sin comillas de texto
        String script = "javascript:window.cargarParadasJson(" + json.toString() + ", " + idRutaFiltro + ");";
        webViewMapa.evaluateJavascript(script, null);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (locationManager != null && locationListener != null)
        {
            locationManager.removeUpdates(locationListener);
        }
    }

    public class BridgeJS
    {
        @JavascriptInterface
        public void notificarEstimacion(String hora, int minutos, int porcentaje)
        {
            runOnUiThread(() ->
            {
                if (lblTiempoNumero != null)
                {
                    lblTiempoNumero.setText(String.valueOf(minutos));
                }
                if (lblEstimacionLlegada != null)
                {
                    lblEstimacionLlegada.setText("Llegada estimada " + hora + " · " + porcentaje + "% del trayecto");
                }
                if (barraProgresoTrayecto != null)
                {
                    View parent = (View) barraProgresoTrayecto.getParent();
                    if (parent != null && parent.getWidth() > 0)
                    {
                        int nuevoAncho = Math.max(30, (parent.getWidth() * porcentaje) / 100);
                        ViewGroup.LayoutParams lp = barraProgresoTrayecto.getLayoutParams();
                        lp.width = nuevoAncho;
                        barraProgresoTrayecto.setLayoutParams(lp);
                    }
                }
            });
        }

        @JavascriptInterface
        public void notificarEstimacion(String hora, int minutos)
        {
            notificarEstimacion(hora, minutos, 25);
        }
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
