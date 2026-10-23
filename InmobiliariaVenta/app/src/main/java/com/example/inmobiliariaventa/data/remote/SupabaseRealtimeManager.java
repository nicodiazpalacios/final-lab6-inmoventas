package com.example.inmobiliariaventa.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.domain.entity.Pago;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Administrador de Supabase Realtime via WebSockets (Protocolo Phoenix Channels v2).
 * Escucha eventos en vivo de Postgres (INSERT, UPDATE, DELETE) en caliente
 * para todas las tablas del esquema public y mantiene actualizada la base de datos local SQLite (Room) en milisegundos.
 */
public class SupabaseRealtimeManager {

    private static final String TAG = "SupabaseRealtime";
    private static volatile SupabaseRealtimeManager INSTANCE;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient wsClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocket webSocket;
    private boolean activo = false;
    private boolean conectado = false;
    private ScheduledFuture<?> heartbeatTask;
    private long heartbeatRef = 0;

    public static SupabaseRealtimeManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SupabaseRealtimeManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SupabaseRealtimeManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private SupabaseRealtimeManager(Context context) {
        this.appContext = context;
        this.wsClient = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * Inicia la conexion Realtime persistente con Supabase.
     */
    public synchronized void iniciar() {
        if (activo) return;
        activo = true;
        conectar();
    }

    /**
     * Detiene la conexion Realtime y cancela tareas en segundo plano.
     */
    public synchronized void detener() {
        activo = false;
        conectado = false;
        detenerHeartbeat();
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Cierre normal");
            } catch (Exception ignored) {}
            webSocket = null;
        }
    }

    private synchronized void conectar() {
        if (!activo) return;

        detenerHeartbeat();
        if (webSocket != null) {
            try {
                webSocket.cancel();
            } catch (Exception ignored) {}
            webSocket = null;
        }

        String base = SupabaseClient.SUPABASE_URL
                .replace("https://", "wss://")
                .replace("http://", "ws://");
        String wsUrl = base + "/realtime/v1/websocket?apikey=" + SupabaseClient.SUPABASE_KEY + "&vsn=1.0.0";

        Log.d(TAG, "Conectando a Supabase Realtime: " + base);
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = wsClient.newWebSocket(request, new RealtimeSocketListener());
    }

    private void iniciarHeartbeat() {
        detenerHeartbeat();
        heartbeatTask = scheduler.scheduleWithFixedDelay(() -> {
            if (conectado && webSocket != null) {
                heartbeatRef++;
                String hb = "{\"topic\":\"phoenix\",\"event\":\"heartbeat\",\"payload\":{},\"ref\":\"hb_" + heartbeatRef + "\"}";
                webSocket.send(hb);
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    private void detenerHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
    }

    private void suscribirACanales() {
        if (webSocket == null || !conectado) return;

        // Unirse al canal para escuchar cambios de todo el esquema public (todas las tablas habilitadas)
        String joinJson = "{"
                + "\"topic\":\"realtime:inmobiliaria_live\","
                + "\"event\":\"phx_join\","
                + "\"payload\":{"
                +   "\"config\":{"
                +     "\"broadcast\":{\"self\":false},"
                +     "\"presence\":{\"key\":\"\"},"
                +     "\"postgres_changes\":["
                +       "{\"event\":\"*\",\"schema\":\"public\"}"
                +     "]"
                +   "}"
                + "},"
                + "\"ref\":\"join_live\""
                + "}";

        webSocket.send(joinJson);
        Log.d(TAG, "Suscripcion enviada para todas las tablas del esquema public en Supabase Realtime");
    }

    private class RealtimeSocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            Log.d(TAG, "Conexion WebSocket establecida con Supabase Realtime");
            conectado = true;
            iniciarHeartbeat();
            suscribirACanales();
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            procesarMensajeRealtime(text);
        }

        @Override
        public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
            Log.d(TAG, "WebSocket cerrado: " + reason + " (" + code + ")");
            conectado = false;
            reprogramarReconexion();
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, @Nullable Response response) {
            Log.w(TAG, "Fallo conexion WebSocket: " + t.getMessage());
            conectado = false;
            reprogramarReconexion();
        }
    }

    private void reprogramarReconexion() {
        detenerHeartbeat();
        if (activo) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler.postDelayed(this::conectar, 5000);
        }
    }

    private void procesarMensajeRealtime(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String event = json.has("event") && !json.get("event").isJsonNull()
                    ? json.get("event").getAsString() : "";

            if ("postgres_changes".equals(event)) {
                JsonObject payload = json.has("payload") && json.get("payload").isJsonObject()
                        ? json.getAsJsonObject("payload") : null;
                if (payload != null) {
                    JsonObject data = payload.has("data") && payload.get("data").isJsonObject()
                            ? payload.getAsJsonObject("data") : payload;

                    String type = data.has("type") && !data.get("type").isJsonNull()
                            ? data.get("type").getAsString() : "";
                    String table = data.has("table") && !data.get("table").isJsonNull()
                            ? data.get("table").getAsString() : "";

                    JsonObject record = data.has("record") && data.get("record").isJsonObject()
                            ? data.getAsJsonObject("record") : null;
                    JsonObject oldRecord = data.has("old_record") && data.get("old_record").isJsonObject()
                            ? data.getAsJsonObject("old_record") : null;

                    aplicarCambioEnRoom(table, type, record, oldRecord);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error procesando mensaje Realtime: " + e.getMessage());
        }
    }

    private void aplicarCambioEnRoom(String table, String type, JsonObject record, JsonObject oldRecord) {
        AppDatabase db = AppDatabase.getInstance(appContext);
        Gson gson = SupabaseClient.getGson();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if ("DELETE".equalsIgnoreCase(type)) {
                    String id = null;
                    if (oldRecord != null && oldRecord.has("id") && !oldRecord.get("id").isJsonNull()) {
                        id = oldRecord.get("id").getAsString();
                    } else if (record != null && record.has("id") && !record.get("id").isJsonNull()) {
                        id = record.get("id").getAsString();
                    }

                    if ("favoritos".equalsIgnoreCase(table)) {
                        String uId = null, pId = null;
                        JsonObject source = oldRecord != null ? oldRecord : record;
                        if (source != null) {
                            if (source.has("usuarioid") && !source.get("usuarioid").isJsonNull()) {
                                uId = source.get("usuarioid").getAsString();
                            } else if (source.has("usuarioId") && !source.get("usuarioId").isJsonNull()) {
                                uId = source.get("usuarioId").getAsString();
                            }

                            if (source.has("propiedadid") && !source.get("propiedadid").isJsonNull()) {
                                pId = source.get("propiedadid").getAsString();
                            } else if (source.has("propiedadId") && !source.get("propiedadId").isJsonNull()) {
                                pId = source.get("propiedadId").getAsString();
                            }
                        }
                        if (uId != null && pId != null) {
                            Log.d(TAG, "Realtime DELETE en favoritos: usuario=" + uId + ", prop=" + pId);
                            db.favoritoDao().eliminarPorIds(uId, pId);
                        }
                    } else if (id != null && !id.isEmpty()) {
                        Log.d(TAG, "Realtime DELETE en tabla [" + table + "], id=" + id);
                        if ("propiedades".equalsIgnoreCase(table)) {
                            db.propiedadDao().eliminarPorId(id);
                        } else if ("contratos".equalsIgnoreCase(table)) {
                            db.contratoDao().eliminarPorId(id);
                            db.pagoDao().eliminarPorContrato(id);
                        } else if ("pagos".equalsIgnoreCase(table)) {
                            db.pagoDao().eliminarPorId(id);
                        } else if ("mensajes".equalsIgnoreCase(table) || "mensajes_contacto".equalsIgnoreCase(table)) {
                            db.mensajeContactoDao().eliminarPorRemoteId(id);
                        } else if ("notificaciones".equalsIgnoreCase(table)) {
                            db.notificacionDao().eliminarPorId(id);
                        } else if ("usuarios".equalsIgnoreCase(table)) {
                            db.usuarioDao().eliminarPorId(id);
                        }
                    }
                } else if ("INSERT".equalsIgnoreCase(type) || "UPDATE".equalsIgnoreCase(type)) {
                    if (record == null) return;
                    Log.d(TAG, "Realtime " + type + " en tabla [" + table + "]");

                    if ("propiedades".equalsIgnoreCase(table)) {
                        Propiedad p = gson.fromJson(record, Propiedad.class);
                        if (p != null && p.id != null) {
                            p.estadoSincronizacion = "SINCRONIZADO";
                            db.propiedadDao().insertar(p);
                        }
                    } else if ("contratos".equalsIgnoreCase(table)) {
                        Contrato c = gson.fromJson(record, Contrato.class);
                        if (c != null && c.id != null) {
                            c.normalizarDesdeSupabase();
                            c.estadoSincronizacion = "SINCRONIZADO";
                            if (c.propiedadTitulo == null || c.propiedadTitulo.isEmpty()) {
                                Propiedad prop = db.propiedadDao().obtenerPorIdSync(c.propiedadId);
                                if (prop != null) c.propiedadTitulo = prop.titulo;
                            }
                            db.contratoDao().insertar(c);
                        }
                    } else if ("pagos".equalsIgnoreCase(table)) {
                        Pago p = gson.fromJson(record, Pago.class);
                        if (p != null && p.id != null) {
                            p.estadoSincronizacion = "SINCRONIZADO";
                            db.pagoDao().insertar(p);
                        }
                    } else if ("mensajes".equalsIgnoreCase(table) || "mensajes_contacto".equalsIgnoreCase(table)) {
                        MensajeContacto m = gson.fromJson(record, MensajeContacto.class);
                        if (m != null) {
                            m.estadoSincronizacion = "SINCRONIZADO";
                            m.calcularTimestampSiEsNecesario();
                            MensajeContacto exist = m.remoteId != null
                                    ? db.mensajeContactoDao().getMensajePorRemoteId(m.remoteId) : null;
                            if (exist != null) {
                                m.id = exist.id;
                                if (exist.timestamp > 0 && m.timestamp <= 0) {
                                    m.timestamp = exist.timestamp;
                                }
                                db.mensajeContactoDao().update(m);
                            } else {
                                db.mensajeContactoDao().insert(m);
                            }
                        }
                    } else if ("notificaciones".equalsIgnoreCase(table)) {
                        Notificacion n = gson.fromJson(record, Notificacion.class);
                        if (n != null && n.id != null) {
                            n.estadoSincronizacion = "SINCRONIZADO";
                            db.notificacionDao().insertar(n);
                        }
                    } else if ("usuarios".equalsIgnoreCase(table)) {
                        Usuario u = gson.fromJson(record, Usuario.class);
                        if (u != null && u.id != null) {
                            u.estadoSincronizacion = "SINCRONIZADO";
                            db.usuarioDao().insertar(u);
                        }
                    } else if ("favoritos".equalsIgnoreCase(table)) {
                        Favorito f = gson.fromJson(record, Favorito.class);
                        if (f != null && f.usuarioId != null && f.propiedadId != null) {
                            f.estadoSincronizacion = "SINCRONIZADO";
                            db.favoritoDao().insertar(f);
                        }
                    } else if ("configuracion_empresa".equalsIgnoreCase(table)) {
                        ConfiguracionEmpresa cfg = gson.fromJson(record, ConfiguracionEmpresa.class);
                        if (cfg != null) {
                            cfg.id = "empresa_principal";
                            db.configuracionEmpresaDao().insertarOActualizar(cfg);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error aplicando cambio en Room desde Realtime: " + e.getMessage());
            }
        });
    }
}
