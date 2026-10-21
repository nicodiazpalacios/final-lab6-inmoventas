package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.inmobiliariaventa.ui.admin.ActividadAdmin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActividadRepository {

    private static final String PREF_NAME = "actividades_admin_prefs";
    private static final String KEY_ACTIVIDADES = "key_actividades_list";
    private static final String KEY_VERSION = "key_actividades_version";
    private static final int CURRENT_VERSION = 3;

    private static volatile ActividadRepository INSTANCE;
    private final SharedPreferences prefs;
    private final Gson gson;

    private ActividadRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

        List<ActividadAdmin> iniciales = obtenerIniciales();
        guardarEnPrefs(iniciales);
        prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply();
        return iniciales;
    }

    public synchronized void registrarNuevoContrato(String cliente, String propiedad, String monto, String situacion) {
        String ref = "Contrato " + (cliente != null ? cliente : "Cliente") + " • " + (propiedad != null ? propiedad : "Inmueble");
        String finalMonto = formatearMonto(monto);
        String detalle = "• Contrato generado (Situación: " + (situacion != null && !situacion.isEmpty() ? situacion : "Activo") + ")";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.NUEVO_CONTRATO,
                "Hace unos momentos",
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);
    }

    public synchronized void registrarModificacionContrato(String cliente, String propiedad, String monto, String nuevaSituacion) {
        String ref = "Contrato " + (cliente != null ? cliente : "Cliente") + " • " + (propiedad != null ? propiedad : "Inmueble");
        String finalMonto = formatearMonto(monto);
        String detalle = "• Situación actualizada a: " + (nuevaSituacion != null && !nuevaSituacion.isEmpty() ? nuevaSituacion : "Modificado");

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.MODIFICACION_CONTRATO,
                "Hace unos momentos",
                finalMonto,
                detalle
        );
        agregarAlInicio(actividad);
    }

    public synchronized void registrarNuevoMensaje(String cliente, String propiedad, String mensaje) {
        String ref = "Consulta: " + (cliente != null && !cliente.trim().isEmpty() ? cliente.trim() : "Cliente") +
                " • " + (propiedad != null && !propiedad.trim().isEmpty() ? propiedad.trim() : "Inmueble");
        String snippet = mensaje != null && !mensaje.trim().isEmpty() ? mensaje.trim() : "Consulta recibida";
        if (snippet.length() > 50) {
            snippet = snippet.substring(0, 47) + "...";
        }
        String detalle = "• \"" + snippet + "\"";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.NUEVO_MENSAJE,
                "Hace unos momentos",
                "Mensaje Nuevo",
                detalle
        );
        agregarAlInicio(actividad);
    }

    public synchronized void registrarMensajeVisto(String cliente, String propiedad) {
        String ref = "Visto: " + (cliente != null && !cliente.trim().isEmpty() ? cliente.trim() : "Cliente") +
                " • " + (propiedad != null && !propiedad.trim().isEmpty() ? propiedad.trim() : "Inmueble");
        String detalle = "• Consulta marcada como leída por el asesor";

        ActividadAdmin actividad = new ActividadAdmin(
                UUID.randomUUID().toString(),
                ref,
                ActividadAdmin.TipoActividad.MENSAJE_VISTO,
                "Hace unos momentos",
                "Visto",
                detalle
        );
        agregarAlInicio(actividad);
    }

    private synchronized void agregarAlInicio(ActividadAdmin actividad) {
        List<ActividadAdmin> actual = new ArrayList<>(getActividades());
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

    private List<ActividadAdmin> obtenerIniciales() {
        List<ActividadAdmin> lista = new ArrayList<>();

        // 1. Mensaje nuevo recibido de un cliente
        lista.add(new ActividadAdmin(
                "init_m1",
                "Consulta: Lucas Martínez • Penthouse Torre Altagracia",
                ActividadAdmin.TipoActividad.NUEVO_MENSAJE,
                "Hace 2 min",
                "Mensaje Nuevo",
                "• \"Hola, me interesa coordinar una visita para este fin de semana...\""
        ));

        // 2. Mensaje marcado como visto por el admin
        lista.add(new ActividadAdmin(
                "init_m2",
                "Visto: Sofía Benítez • Skyline Luxury Residences",
                ActividadAdmin.TipoActividad.MENSAJE_VISTO,
                "Hace 12 min",
                "Visto",
                "• Consulta marcada como leída por el asesor"
        ));

        // 3. Nuevo Contrato subido
        lista.add(new ActividadAdmin(
                "init_c1",
                "Contrato Juan Pérez • Skyline Luxury",
                ActividadAdmin.TipoActividad.NUEVO_CONTRATO,
                "Hace 25 min",
                "$ 150,000 USD",
                "• Contrato generado (Situación: Activo)"
        ));

        // 4. Modificación de Situación de Contrato
        lista.add(new ActividadAdmin(
                "init_c2",
                "Contrato Elena Rodríguez • Torre Altavista",
                ActividadAdmin.TipoActividad.MODIFICACION_CONTRATO,
                "Hace 40 min",
                "$ 85,000 USD",
                "• Situación modificada a: Finalizado"
        ));

        // 3. Cambio de precio
        lista.add(new ActividadAdmin(
                "init_1",
                "Apartamento Loft Skywalk",
                ActividadAdmin.TipoActividad.CAMBIO_PRECIO,
                "Hace 1 hora",
                "$ 450,000 USD",
                "• Antes $485,000 USD (-7.2%)"
        ));

        // 4. Nueva propiedad subida
        lista.add(new ActividadAdmin(
                "init_2",
                "Residencia Los Olivos - San Benito",
                ActividadAdmin.TipoActividad.NUEVA_PROPIEDAD,
                "Hoy, 14:20 hs",
                "$ 420,000 USD",
                "• Publicación ingresada al catálogo"
        ));

        // 5. Propiedad dada de baja
        lista.add(new ActividadAdmin(
                "init_3",
                "Penthouse Torre Altagracia",
                ActividadAdmin.TipoActividad.DADA_DE_BAJA,
                "Ayer, 18:30 hs",
                "$ 1,750,000 USD",
                "• Retirada de catálogo (Pausada)"
        ));

        // 6. Modificación de situación de contrato
        lista.add(new ActividadAdmin(
                "init_c3",
                "Contrato Roberto Gómez • Residencial El Bosque",
                ActividadAdmin.TipoActividad.MODIFICACION_CONTRATO,
                "Ayer, 11:15 hs",
                "$ 240,000 USD",
                "• Situación modificada a: Pendiente"
        ));

        // 7. Cambio de precio
        lista.add(new ActividadAdmin(
                "init_4",
                "Villa Los Laureles - Santa Elena",
                ActividadAdmin.TipoActividad.CAMBIO_PRECIO,
                "19 Oct, 11:10 hs",
                "$ 850,000 USD",
                "• Antes $890,000 USD (Ajuste comercial)"
        ));

        // 8. Nuevo Contrato subido
        lista.add(new ActividadAdmin(
                "init_c4",
                "Contrato Ana Martínez • Condominio Los Pinos",
                ActividadAdmin.TipoActividad.NUEVO_CONTRATO,
                "18 Oct, 16:00 hs",
                "$ 95,000 USD",
                "• Contrato generado (Situación: Activo)"
        ));

        // 9. Propiedad dada de baja
        lista.add(new ActividadAdmin(
                "init_6",
                "Terreno Campestre San José",
                ActividadAdmin.TipoActividad.DADA_DE_BAJA,
                "17 Oct, 10:15 hs",
                "$ 310,000 USD",
                "• Baja por contrato finalizado"
        ));

        return lista;
    }
}
