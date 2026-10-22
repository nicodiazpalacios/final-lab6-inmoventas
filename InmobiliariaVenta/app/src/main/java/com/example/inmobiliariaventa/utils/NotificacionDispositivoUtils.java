package com.example.inmobiliariaventa.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.ui.MainActivity;

/**
 * Utilidad para gestionar canales y disparar notificaciones locales en la barra de estado
 * y cortina de Android sin necesidad de servicios push externos.
 */
public class NotificacionDispositivoUtils {

    public static final String CANAL_ID = "inmoventas_alertas";
    public static final String CANAL_NOMBRE = "Alertas Inmobiliarias";
    public static final String CANAL_DESCRIPCION = "Avisos de inmuebles, favoritos, contratos y mensajes";

    /**
     * Crea el canal de notificaciones requerido a partir de Android 8.0 (API 26).
     */
    public static void crearCanalNotificaciones(Context context) {
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CANAL_ID,
                    CANAL_NOMBRE,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CANAL_DESCRIPCION);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Muestra una notificación local en el dispositivo.
     *
     * @param context       Contexto de la aplicación
     * @param id            Identificador único entero de la notificación
     * @param titulo        Título visible de la notificación
     * @param mensaje       Cuerpo o descripción de la notificación
     * @param propiedadId   ID opcional de la propiedad para navegación directa
     * @param esAdmin       Indica si el destino por defecto es el panel de admin o de cliente
     */
    public static void mostrarNotificacion(
            Context context,
            int id,
            String titulo,
            String mensaje,
            String propiedadId,
            boolean esAdmin) {

        if (context == null) return;

        Context appCtx = context.getApplicationContext();

        // 1. Asegurar que el canal esté registrado
        crearCanalNotificaciones(appCtx);

        // 2. Verificar permiso de notificaciones en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // 3. Crear el Intent para abrir la aplicación al tocar la notificación
        Intent intent = new Intent(appCtx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (propiedadId != null && !propiedadId.isEmpty()) {
            intent.putExtra("extra_propiedad_id", propiedadId);
        }
        intent.putExtra("extra_destino", esAdmin ? "admin_notificaciones" : "cliente_notificaciones");

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                appCtx,
                id,
                intent,
                pendingFlags
        );

        String tit = (titulo != null && !titulo.trim().isEmpty()) ? titulo.trim() : "InmoVentas";
        String msg = (mensaje != null && !mensaje.trim().isEmpty()) ? mensaje.trim() : "";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appCtx, CANAL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(tit)
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(appCtx).notify(id, builder.build());
        } catch (SecurityException ignored) {
            // Protección contra falta de permisos en tiempo de ejecución
        } catch (Exception e) {
            android.util.Log.e("NotifDispositivo", "Error al emitir notificación", e);
        }
    }
}
