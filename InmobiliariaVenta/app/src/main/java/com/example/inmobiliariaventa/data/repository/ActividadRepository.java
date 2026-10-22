package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.ui.admin.ActividadAdmin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ActividadRepository {

    private static final String PREF_NAME = "actividades_admin_prefs";
    public static final String PREF_NOTIF_SETTINGS = "notificaciones_prefs";

    // Preferencias de Notificaciones de Admin
    public static final String KEY_ADMIN_NUEVO_CONTRATO = "pref_admin_notif_nuevos_contratos";
    public static final String KEY_ADMIN_MODIF_CONTRATO = "pref_admin_notif_modif_contratos";
    public static final String KEY_ADMIN_NUEVO_LEAD = "pref_admin_notif_nuevos_leads";
    public static final String KEY_ADMIN_MENSAJE_VISTO = "pref_admin_notif_mensajes_vistos";
    public static final String KEY_ADMIN_NUEVA_PROPIEDAD = "pref_admin_notif_nuevas_propiedades";
    public static final String KEY_ADMIN_CAMBIO_PRECIO = "pref_admin_notif_cambio_precios";
    public static final String KEY_ADMIN_BAJA_PROPIEDAD = "pref_admin_notif_baja_propiedades";

    // Preferencias de Notificaciones de Cliente
    public static final String KEY_CLI_PRECIOS_FAV = "pref_cli_notif_precios_fav";
    public static final String KEY_CLI_ESTADO_FAV = "pref_cli_notif_estado_fav";
    public static final String KEY_CLI_RECOMENDACIONES = "pref_cli_notif_recomendaciones";
    public static final String KEY_CLI_CONSULTAS = "pref_cli_notif_consultas";

    private static final String KEY_ACTIVIDADES = "key_actividades_list";
    private static final String KEY_VERSION = "key_actividades_version";
    private static final int CURRENT_VERSION = 5;

    private static volatile ActividadRepository INSTANCE;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;

    private ActividadRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = this.appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static ActividadRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ActividadRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ActividadRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public synchronized List<ActividadAdmin> getActividades() {
        // Devuelve el historial completo de actividades para que siempre se puedan consultar.
        // Las preferencias de la tuerca gobiernan el envío de alertas activas al dispositivo.
        return obtenerListaCompletaInterna();
    }

    public boolean debeAlertarActividadAdmin(ActividadAdmin act) {
        return debeMostrarActividadAdmin(act);
    }

    private boolean debeMostrarActividadAdmin(ActividadAdmin act) {
        if (act == null || act.getTipo() == null) return true;
        switch (act.getTipo()) {
            case NUEVO_CONTRATO:
                return isAdminPrefEnabled(KEY_ADMIN_NUEVO_CONTRATO);
            case MODIFICACION_CONTRATO:
                return isAdminPrefEnabled(KEY_ADMIN_MODIF_CONTRATO);
            case NUEVO_MENSAJE:
                return isAdminPrefEnabled(KEY_ADMIN_NUEVO_LEAD);
            case MENSAJE_VISTO:
                return isAdminPrefEnabled(KEY_ADMIN_MENSAJE_VISTO);
            case NUEVA_PROPIEDAD:
                return isAdminPrefEnabled(KEY_ADMIN_NUEVA_PROPIEDAD);
            case CAMBIO_PRECIO:
                return isAdminPrefEnabled(KEY_ADMIN_CAMBIO_PRECIO);
            case DADA_DE_BAJA:
                return isAdminPrefEnabled(KEY_ADMIN_BAJA_PROPIEDAD);
            default:
                return true;
        }
    }

    private synchronized List<ActividadAdmin> obtenerListaCompletaInterna() {
        int savedVersion = prefs.getInt(KEY_VERSION, 0);
        if (savedVersion >= CURRENT_VERSION) {
            String json = prefs.getString(KEY_ACTIVIDADES, null);
            if (json != null && !json.trim().isEmpty()) {
                try {
                    Type listType = new TypeToken<List<ActividadAdmin>>() {}.getType();
                    List<ActividadAdmin> guardadas = gson.fromJson(json, listType);
                    if (guardadas != null && !guardadas.isEmpty()) {
                        return guardadas;
                    }
                } catch (Exception ignored) {}
            }
        }

        List<ActividadAdmin> iniciales = construirActividadesDesdeBaseRealSync();
        guardarEnPrefs(iniciales);
        prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply();
        return iniciales;
    }

    public void sincronizarNotificacionesYActividades(Runnable onFinishMainThread) {
        SupabaseClient.obtenerNotificaciones(new SupabaseClient.SupabaseCallback<List<Notificacion>>() {
            @Override
            public void onSuccess(List<Notificacion> remotas) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(appContext);
                    if (remotas != null && !remotas.isEmpty()) {
                        db.notificacionDao().insertarTodas(remotas);
                    }
                    sembrarNotificacionesRealesSiVacio(db);
                    reconstruirActividadesSiVacio(db);
                    if (onFinishMainThread != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(onFinishMainThread);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(appContext);
                    sembrarNotificacionesRealesSiVacio(db);
                    reconstruirActividadesSiVacio(db);
                    if (onFinishMainThread != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(onFinishMainThread);
                    }
                });
            }
        });
    }

    private void sembrarNotificacionesRealesSiVacio(AppDatabase db) {
        try {
            List<Notificacion> actuales = db.notificacionDao().obtenerTodasSync();
            if (actuales != null && !actuales.isEmpty()) {
                return;
            }
            List<Contrato> contratos = db.contratoDao().obtenerTodosSync();
            if (contratos != null) {
                int idx = 0;
                for (Contrato c : contratos) {
                    String idNotif = "notif_contrato_" + (c.id != null ? c.id : UUID.randomUUID().toString());
                    String cliente = ((c.clienteNombre != null ? c.clienteNombre : "") + " " + (c.clienteApellido != null ? c.clienteApellido : "")).trim();
                    if (cliente.isEmpty()) cliente = "Cliente";
                    String prop = (c.propiedadTitulo != null && !c.propiedadTitulo.trim().isEmpty())
                            ? c.propiedadTitulo.trim()
                            : (c.propiedadId != null ? c.propiedadId : "Inmueble");
                    String estado = c.estado != null ? c.estado : "Activo";
                    double monto = c.montoTotal != null ? c.montoTotal : 0;

                    Notificacion n = new Notificacion(
                            idNotif,
                            null,
                            "Contrato (" + estado + "): " + cliente,
                            "Contrato de compraventa para '" + prop + "' por USD " + String.format(Locale.US, "%,.0f", monto) + " (" + (c.metodoPago != null ? c.metodoPago : "Efectivo") + ").",
                            System.currentTimeMillis() - (idx * 60000L),
                            false,
                            "CONTRATOS",
                            c.id
                    );
                    db.notificacionDao().insertar(n);
                    SupabaseClient.guardarNotificacion(n, null);
                    idx++;
                }
            }

            List<MensajeContacto> mensajes = db.mensajeContactoDao().getAllMensajesSync();
            if (mensajes != null) {
                int idx = 1;
                for (MensajeContacto m : mensajes) {
                    String idNotif = "notif_msg_" + (m.remoteId != null && !m.remoteId.isEmpty() ? m.remoteId : m.id);
                    String cliente = m.clienteNombre != null && !m.clienteNombre.isEmpty() ? m.clienteNombre : "Cliente";
                    String prop = m.propiedadTitulo != null && !m.propiedadTitulo.isEmpty() ? m.propiedadTitulo : "Consulta general";
                    Notificacion n = new Notificacion(
                            idNotif,
                            null,
                            "Consulta de " + cliente,
                            "Sobre '" + prop + "': \"" + (m.mensaje != null ? m.mensaje : "") + "\"",
                            System.currentTimeMillis() - (idx * 120000L),
                            m.leido,
                            "LEADS",
                            m.propiedadId
                    );
                    db.notificacionDao().insertar(n);
                    SupabaseClient.guardarNotificacion(n, null);
                    idx++;
                }
            }
        } catch (Exception ignored) {}
    }

    private void reconstruirActividadesSiVacio(AppDatabase db) {
        List<ActividadAdmin> guardadas = null;
        String json = prefs.getString(KEY_ACTIVIDADES, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Type listType = new TypeToken<List<ActividadAdmin>>() {}.getType();
                guardadas = gson.fromJson(json, listType);
            } catch (Exception ignored) {}
        }
        if (guardadas == null || guardadas.isEmpty()) {
            List<ActividadAdmin> reales = construirActividadesDesdeDb(db);
            guardarEnPrefs(reales);
            prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply();
        }
    }

    private List<ActividadAdmin> construirActividadesDesdeBaseRealSync() {
        final List<ActividadAdmin> resultado = new ArrayList<>();
        Thread t = new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                resultado.addAll(construirActividadesDesdeDb(db));
            } catch (Exception ignored) {}
        });
        t.start();
        try {
            t.join(1500);
        } catch (InterruptedException ignored) {}
        return resultado;
    }

    private List<ActividadAdmin> construirActividadesDesdeDb(AppDatabase db) {
        List<ActividadAdmin> lista = new ArrayList<>();
        try {
            List<Contrato> contratos = db.contratoDao().obtenerTodosSync();
            if (contratos != null) {
                for (Contrato c : contratos) {
                    String cliente = ((c.clienteNombre != null ? c.clienteNombre : "") + " " + (c.clienteApellido != null ? c.clienteApellido : "")).trim();
                    if (cliente.isEmpty()) cliente = "Cliente";
                    String prop = (c.propiedadTitulo != null && !c.propiedadTitulo.trim().isEmpty())
                            ? c.propiedadTitulo.trim()
                            : (c.propiedadId != null ? c.propiedadId : "Inmueble");
                    String montoStr = formatearMonto(String.format(Locale.US, "%,.0f", c.montoTotal != null ? c.montoTotal : 0.0));
                    String estado = c.estado != null ? c.estado : "Activo";
                    String fechaTxt = c.fechaInicio != null && !c.fechaInicio.isEmpty() ? c.fechaInicio : "Registrado";

                    lista.add(new ActividadAdmin(
                            "act_c_" + c.id,
                            "Contrato " + cliente + " • " + prop,
                            ActividadAdmin.TipoActividad.NUEVO_CONTRATO,
                            fechaTxt,
                            montoStr,
                            "• Contrato registrado (Situación: " + estado + ")"
                    ));
                }
            }

            List<MensajeContacto> mensajes = db.mensajeContactoDao().getAllMensajesSync();
            if (mensajes != null) {
                for (MensajeContacto m : mensajes) {
                    String cliente = m.clienteNombre != null ? m.clienteNombre : "Cliente";
                    String prop = m.propiedadTitulo != null && !m.propiedadTitulo.isEmpty() ? m.propiedadTitulo : "Inmueble";
                    String snippet = m.mensaje != null ? m.mensaje.trim() : "Consulta recibida";
                    if (snippet.length() > 50) snippet = snippet.substring(0, 47) + "...";
                    String fechaTxt = (m.fecha != null ? m.fecha : "") + (m.hora != null && !m.hora.isEmpty() ? " " + m.hora : "");

                    lista.add(new ActividadAdmin(
                            "act_m_" + m.id,
                            (m.leido ? "Visto: " : "Consulta: ") + cliente + " • " + prop,
                            m.leido ? ActividadAdmin.TipoActividad.MENSAJE_VISTO : ActividadAdmin.TipoActividad.NUEVO_MENSAJE,
                            fechaTxt.trim().isEmpty() ? "Reciente" : fechaTxt.trim(),
                            m.leido ? "Visto" : "Mensaje Nuevo",
                            "• \"" + snippet + "\""
                    ));
                }
            }
        } catch (Exception ignored) {}
        return lista;
    }

    private boolean isAdminPrefEnabled(String prefKey) {
        SharedPreferences notifPrefs = appContext.getSharedPreferences(PREF_NOTIF_SETTINGS, Context.MODE_PRIVATE);
        return notifPrefs.getBoolean(prefKey, true);
    }

    public static boolean debeMostrarNotificacionAdmin(Context context, Notificacion n) {
        if (context == null || n == null) return true;
        SharedPreferences notifPrefs = context.getSharedPreferences(PREF_NOTIF_SETTINGS, Context.MODE_PRIVATE);
        String t = (n.titulo != null ? n.titulo : "").toLowerCase(Locale.ROOT);
        String m = (n.mensaje != null ? n.mensaje : "").toLowerCase(Locale.ROOT);
        String c = (n.categoria != null ? n.categoria : "").toUpperCase(Locale.ROOT);

        if (c.contains("CONTRATO") || t.contains("contrato")) {
            if (t.contains("actualizado") || m.contains("cambió su situación") || m.contains("modific")) {
                return notifPrefs.getBoolean(KEY_ADMIN_MODIF_CONTRATO, true);
            }
            return notifPrefs.getBoolean(KEY_ADMIN_NUEVO_CONTRATO, true);
        }
        if (c.contains("LEAD") || t.contains("consulta")) {
            if (t.contains("revisada") || m.contains("marcada como leída")) {
                return notifPrefs.getBoolean(KEY_ADMIN_MENSAJE_VISTO, true);
            }
            return notifPrefs.getBoolean(KEY_ADMIN_NUEVO_LEAD, true);
        }
        if (t.contains("precio") || m.contains("precio")) {
            return notifPrefs.getBoolean(KEY_ADMIN_CAMBIO_PRECIO, true);
        }
        if (t.contains("cambio de estado") || m.contains("eliminada") || m.contains("pausada") || m.contains("baja")) {
            return notifPrefs.getBoolean(KEY_ADMIN_BAJA_PROPIEDAD, true);
        }
        if (t.contains("nueva propiedad") || m.contains("ingresó")) {
            return notifPrefs.getBoolean(KEY_ADMIN_NUEVA_PROPIEDAD, true);
        }
        return true;
    }

    private void guardarNotificacionPersistente(String prefKey, String titulo, String mensaje, String categoria, String referenciaId) {
        if (prefKey != null && !isAdminPrefEnabled(prefKey)) {
            return;
        }
        Notificacion notif = new Notificacion(
                UUID.randomUUID().toString(),
                null,
                titulo,
                mensaje,
                System.currentTimeMillis(),
                false,
                categoria,
                referenciaId
        );
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase.getInstance(appContext).notificacionDao().insertar(notif);
            } catch (Exception ignored) {}
        });
        SupabaseClient.guardarNotificacion(notif, null);
    }

    public synchronized void registrarNuevoContrato(String cliente, String propiedad, String monto, String situacion) {
        String ref = "Contrato " + (cliente != null ? cliente : "Cliente") + " • " + (propiedad != null ? propiedad : "Inmueble");
        String finalMonto = formatearMonto(monto);
        String estadoTxt = situacion != null && !situacion.isEmpty() ? situacion : "Activo";
        String detalle = "• Contrato generado (Situación: " + estadoTxt + ")";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.NUEVO_CONTRATO,
                 obtenerHoraActualTexto(),
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_NUEVO_CONTRATO,
                "Nuevo Contrato: " + (cliente != null ? cliente : "Cliente"),
                "Se registró el contrato para '" + (propiedad != null ? propiedad : "Inmueble") + "' por " + finalMonto + " (Situación: " + estadoTxt + ").",
                "CONTRATOS",
                propiedad
        );
    }

    public synchronized void registrarModificacionContrato(String cliente, String propiedad, String monto, String nuevaSituacion) {
        String ref = "Contrato " + (cliente != null ? cliente : "Cliente") + " • " + (propiedad != null ? propiedad : "Inmueble");
        String finalMonto = formatearMonto(monto);
        String estadoTxt = nuevaSituacion != null && !nuevaSituacion.isEmpty() ? nuevaSituacion : "Modificado";
        String detalle = "• Situación actualizada a: " + estadoTxt;

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.MODIFICACION_CONTRATO,
                obtenerHoraActualTexto(),
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_MODIF_CONTRATO,
                "Contrato actualizado: " + (cliente != null ? cliente : "Cliente"),
                "El contrato de '" + (propiedad != null ? propiedad : "Inmueble") + "' (" + finalMonto + ") cambió su situación a: " + estadoTxt + ".",
                "CONTRATOS",
                propiedad
        );
    }

    public synchronized void registrarNuevoMensaje(String cliente, String propiedad, String mensaje) {
        String cleanCliente = cliente != null && !cliente.trim().isEmpty() ? cliente.trim() : "Cliente";
        String cleanProp = propiedad != null && !propiedad.trim().isEmpty() ? propiedad.trim() : "Inmueble";
        String ref = "Consulta: " + cleanCliente + " • " + cleanProp;
        String snippet = mensaje != null && !mensaje.trim().isEmpty() ? mensaje.trim() : "Consulta recibida";
        if (snippet.length() > 50) {
            snippet = snippet.substring(0, 47) + "...";
        }
        String detalle = "• \"" + snippet + "\"";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.NUEVO_MENSAJE,
                obtenerHoraActualTexto(),
                "Mensaje Nuevo",
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_NUEVO_LEAD,
                "Nueva consulta de " + cleanCliente,
                "Interesado en '" + cleanProp + "': \"" + (mensaje != null ? mensaje.trim() : "") + "\"",
                "LEADS",
                cleanProp
        );
    }

    public synchronized void registrarMensajeVisto(String cliente, String propiedad) {
        String cleanCliente = cliente != null && !cliente.trim().isEmpty() ? cliente.trim() : "Cliente";
        String cleanProp = propiedad != null && !propiedad.trim().isEmpty() ? propiedad.trim() : "Inmueble";
        String ref = "Visto: " + cleanCliente + " • " + cleanProp;
        String detalle = "• Consulta marcada como leída por el asesor";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.MENSAJE_VISTO,
                obtenerHoraActualTexto(),
                "Visto",
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_MENSAJE_VISTO,
                "Consulta revisada: " + cleanCliente,
                "La consulta sobre '" + cleanProp + "' fue marcada como leída por el asesor inmobiliario.",
                "LEADS",
                cleanProp
        );
    }

    public synchronized void registrarNuevaPropiedad(String titulo, double precio) {
        String ref = titulo != null && !titulo.trim().isEmpty() ? titulo.trim() : "Nueva Propiedad";
        String finalMonto = formatearMonto(String.format(Locale.US, "%,d", (long) precio));
        String detalle = "• Publicación ingresada al catálogo";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.NUEVA_PROPIEDAD,
                obtenerHoraActualTexto(),
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_NUEVA_PROPIEDAD,
                "Nueva propiedad publicada: " + ref,
                "Se ingresó '" + ref + "' al catálogo con valor de " + finalMonto + ".",
                "PROPIEDADES",
                ref
        );
    }

    public synchronized void registrarCambioPrecio(String titulo, double precioAnterior, double precioNuevo) {
        String ref = titulo != null && !titulo.trim().isEmpty() ? titulo.trim() : "Propiedad";
        String finalMonto = formatearMonto(String.format(Locale.US, "%,d", (long) precioNuevo));
        String montoAnt = formatearMonto(String.format(Locale.US, "%,d", (long) precioAnterior));
        String detalle = "• Antes " + montoAnt;

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.CAMBIO_PRECIO,
                obtenerHoraActualTexto(),
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_CAMBIO_PRECIO,
                "Actualización de precio: " + ref,
                "El precio de '" + ref + "' se actualizó de " + montoAnt + " a " + finalMonto + ".",
                "PROPIEDADES",
                ref
        );
    }

    public synchronized void registrarBajaPropiedad(String titulo, double precio, String motivo) {
        String ref = titulo != null && !titulo.trim().isEmpty() ? titulo.trim() : "Propiedad";
        String finalMonto = formatearMonto(String.format(Locale.US, "%,d", (long) precio));
        String motivoTxt = motivo != null && !motivo.isEmpty() ? motivo : "Eliminada del catálogo";
        String detalle = "• " + motivoTxt;

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.DADA_DE_BAJA,
                obtenerHoraActualTexto(),
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);

        guardarNotificacionPersistente(
                KEY_ADMIN_BAJA_PROPIEDAD,
                "Cambio de estado en catálogo: " + ref,
                "La propiedad '" + ref + "' (" + finalMonto + ") cambió de estado: " + motivoTxt + ".",
                "PROPIEDADES",
                ref
        );
    }

    private String obtenerHoraActualTexto() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
    }

    private synchronized void agregarAlInicio(ActividadAdmin actividad) {
        List<ActividadAdmin> actual = new ArrayList<>(obtenerListaCompletaInterna());
        actual.add(0, actividad);
        if (actual.size() > 30) {
            actual = actual.subList(0, 30);
        }
        guardarEnPrefs(actual);
    }

    private void guardarEnPrefs(List<ActividadAdmin> lista) {
        try {
            String json = gson.toJson(lista);
            prefs.edit().putString(KEY_ACTIVIDADES, json).apply();
        } catch (Exception ignored) {}
    }

    private String formatearMonto(String monto) {
        if (monto == null || monto.trim().isEmpty()) return "$ 0 USD";
        String clean = monto.trim();
        if (!clean.startsWith("$")) {
            clean = "$ " + clean;
        }
        if (!clean.toUpperCase().contains("USD")) {
            clean = clean + " USD";
        }
        return clean;
    }
}
