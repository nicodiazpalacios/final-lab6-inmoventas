package com.example.inmobiliariaventa.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.domain.entity.Propiedad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gestor unificado para observar y mostrar el indicador rojo (badge) de notificaciones no leídas
 * en el icono de la cabecera tanto para usuarios clientes como para administradores.
 */
public class NotificacionBadgeHelper {

    /**
     * Evalúa si existen notificaciones no leídas relevantes para el usuario actual.
     */
    public static boolean tieneNotificacionesSinLeer(Context context, List<Notificacion> notificaciones) {
        if (context == null || notificaciones == null || notificaciones.isEmpty()) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userRol = prefs.getString("user_rol", "");
        String userId = prefs.getString("user_id", "");
        boolean isAdmin = isLoggedIn && userRol != null && userRol.trim().toUpperCase(Locale.ROOT).contains("ADMIN");

        for (Notificacion n : notificaciones) {
            if (n == null || n.isLeida) continue;

            if (isAdmin) {
                // Para administradores cualquier alerta no leída en el sistema activa el punto
                return true;
            } else {
                // Para clientes o visitantes sólo notificaciones asociadas a favoritos, consultas o recomendaciones
                String cat = n.categoria != null ? n.categoria.toUpperCase(Locale.ROOT) : "";
                boolean esDeCliente = cat.contains("FAVORITO")
                        || cat.contains("CONSULTA")
                        || cat.contains("UBICACION")
                        || cat.contains("RECOMEND")
                        || (userId != null && !userId.isEmpty() && userId.equals(n.usuario_id));

                if (esDeCliente) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vincula la visibilidad de una View (puntito rojo) con el LiveData de notificaciones de la base local Room.
     */
    public static void configurarObservadorBadge(
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull Context context,
            @NonNull View badgeView) {

        Context appCtx = context.getApplicationContext();
        AppDatabase.getInstance(appCtx).notificacionDao().obtenerTodasLiveData()
                .observe(lifecycleOwner, notificaciones -> {
                    boolean haySinLeer = tieneNotificacionesSinLeer(appCtx, notificaciones);
                    badgeView.setVisibility(haySinLeer ? View.VISIBLE : View.GONE);
                });
    }

    /**
     * Asegura en segundo plano que el catálogo de notificaciones básicas del cliente esté sembrado
     * para que el indicador rojo se active inmediatamente si hay avisos nuevos.
     */
    public static void sembrarNotificacionesClienteAsync(Context context) {
        if (context == null) return;
        Context appCtx = context.getApplicationContext();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appCtx);
                SharedPreferences prefs = appCtx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                String userId = prefs.getString("user_id", "");
                String userEmail = prefs.getString("user_email", "");
                String userNombre = prefs.getString("user_nombre", "");

                List<Propiedad> misFavoritos = (!userId.isEmpty()) ? db.favoritoDao().obtenerFavoritosDeUsuario(userId) : new ArrayList<>();
                List<MensajeContacto> todosMensajes = db.mensajeContactoDao().getAllMensajesSync();
                List<Propiedad> todasPropiedades = db.propiedadDao().obtenerTodasSync();
                List<Notificacion> existentes = db.notificacionDao().obtenerTodasSync();

                // 1. Favoritos
                if (misFavoritos != null) {
                    for (Propiedad fav : misFavoritos) {
                        if (fav == null || fav.id == null) continue;
                        String idNotifFav = "cli_fav_" + fav.id;
                        boolean yaExiste = false;
                        if (existentes != null) {
                            for (Notificacion n : existentes) {
                                if (idNotifFav.equals(n.id)) {
                                    yaExiste = true;
                                    break;
                                }
                            }
                        }
                        if (!yaExiste) {
                            String tit = "Favorito guardado: " + fav.titulo;
                            String desc = String.format(Locale.US, "Ubicada en %s • USD %,.0f. Recibirás avisos si cambia su valor o estado.",
                                    fav.ciudad != null ? fav.ciudad : "Santiago del Estero", fav.precio);
                            Notificacion n = new Notificacion(idNotifFav, userId, tit, desc, System.currentTimeMillis() - 3600000L, false, "FAVORITOS", fav.id);
                            db.notificacionDao().insertar(n);
                        }
                    }
                }

                // 2. Consultas y mensajes del usuario
                if (todosMensajes != null) {
                    for (MensajeContacto m : todosMensajes) {
                        if (m == null) continue;
                        boolean esDelUsuario = (userEmail != null && !userEmail.isEmpty() && userEmail.equalsIgnoreCase(m.clienteEmail))
                                || (userNombre != null && !userNombre.isEmpty() && m.clienteNombre != null && m.clienteNombre.toLowerCase().contains(userNombre.toLowerCase()));
                        if (esDelUsuario) {
                            String idNotifMsg = "cli_msg_" + m.id;
                            boolean yaExiste = false;
                            if (existentes != null) {
                                for (Notificacion n : existentes) {
                                    if (idNotifMsg.equals(n.id)) {
                                        yaExiste = true;
                                        break;
                                    }
                                }
                            }
                            if (!yaExiste) {
                                String propTit = m.propiedadTitulo != null ? m.propiedadTitulo : "Propiedad";
                                String tit = m.leido ? "Tu consulta fue revisada" : "Consulta enviada con éxito";
                                String desc = m.leido
                                        ? "El asesor inmobiliario revisó tu mensaje sobre '" + propTit + "'. Pronto se comunicarán con vos."
                                        : "Tu mensaje sobre '" + propTit + "' fue recibido en el sistema y está en revisión.";
                                Notificacion n = new Notificacion(idNotifMsg, userId, tit, desc, System.currentTimeMillis() - 7200000L, m.leido, "CONSULTAS", m.propiedadId);
                                db.notificacionDao().insertar(n);
                            }
                        }
                    }
                }

                // 3. Recomendaciones destacadas
                if (todasPropiedades != null) {
                    int countRec = 0;
                    for (Propiedad p : todasPropiedades) {
                        if (p == null || !"Disponible".equalsIgnoreCase(p.estado)) continue;
                        boolean yaFav = false;
                        if (misFavoritos != null) {
                            for (Propiedad f : misFavoritos) {
                                if (f != null && f.id != null && f.id.equals(p.id)) {
                                    yaFav = true;
                                    break;
                                }
                            }
                        }
                        if (yaFav) continue;

                        String idNotifRec = "cli_rec_" + p.id;
                        boolean yaExiste = false;
                        if (existentes != null) {
                            for (Notificacion n : existentes) {
                                if (idNotifRec.equals(n.id)) {
                                    yaExiste = true;
                                    break;
                                }
                            }
                        }
                        if (!yaExiste) {
                            String ciudad = p.ciudad != null && !p.ciudad.isEmpty() ? p.ciudad : "Santiago del Estero";
                            String tit = "Recomendada cerca tuyo: " + p.titulo;
                            String desc = String.format(Locale.US, "📍 Ubicada en %s (%s) • USD %,.0f. Tocá para ver fotos y detalles.",
                                    ciudad, p.domicilio != null ? p.domicilio : "Zona céntrica", p.precio);
                            Notificacion n = new Notificacion(idNotifRec, userId, tit, desc, System.currentTimeMillis() - 10800000L, false, "UBICACION", p.id);
                            db.notificacionDao().insertar(n);
                            countRec++;
                            if (countRec >= 2) break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }
}
