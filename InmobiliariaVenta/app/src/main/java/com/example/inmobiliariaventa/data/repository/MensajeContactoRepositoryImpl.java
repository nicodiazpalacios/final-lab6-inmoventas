package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.MensajeContactoDao;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.repository.MensajeContactoRepository;

import java.util.List;
import java.util.UUID;

public class MensajeContactoRepositoryImpl implements MensajeContactoRepository {

    private static final String TAG = "MensajeContactoRepo";
    private static volatile MensajeContactoRepositoryImpl INSTANCE;

    private final Context appContext;
    private final MensajeContactoDao dao;

    public static MensajeContactoRepositoryImpl getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MensajeContactoRepositoryImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MensajeContactoRepositoryImpl(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public MensajeContactoRepositoryImpl(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.mensajeContactoDao();
        sanearMensajesLocales();
    }

    public void sanearMensajesLocales() {
        AppDatabase.databaseWriteExecutor.execute(this::sanearMensajesLocalesSync);
    }

    public void sanearMensajesLocalesSync() {
        try {
            List<MensajeContacto> todos = dao.getAllMensajesSync();
            if (todos != null) {
                long now = System.currentTimeMillis();
                for (MensajeContacto m : todos) {
                    if (m == null) continue;
                    if (m.isDemoMessage()) {
                        if ("PENDIENTE_SUBIR".equals(m.estadoSincronizacion)) {
                            dao.actualizarEstadoSincronizacion(m.id, "DEMO");
                        }
                        // Asegurar que los mensajes demo queden siempre en el pasado (3 a 15 días atrás)
                        long minPast = now - (3L * 86400000L);
                        if (m.timestamp > minPast || m.timestamp <= 0) {
                            long offset = (3L + (Math.abs(m.id) % 15)) * 86400000L + (Math.abs(m.id) * 3600000L);
                            m.timestamp = now - offset;
                            dao.actualizarTimestamp(m.id, m.timestamp);
                        }
                    } else {
                        // Mensaje real de cliente: asegurar timestamp válido
                        if (m.timestamp <= 0) {
                            m.calcularTimestampSiEsNecesario();
                            if (m.timestamp > 0) {
                                dao.actualizarTimestamp(m.id, m.timestamp);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saneando mensajes locales: " + e.getMessage());
        }
    }

    @Override
    public void enviarMensaje(MensajeContacto mensaje) {
        if (mensaje == null) return;

        if (mensaje.remoteId == null || mensaje.remoteId.trim().isEmpty()) {
            mensaje.remoteId = UUID.randomUUID().toString();
        }
        mensaje.estadoSincronizacion = "PENDIENTE_SUBIR";
        if (mensaje.timestamp <= 0) {
            mensaje.timestamp = System.currentTimeMillis();
        }
        mensaje.calcularTimestampSiEsNecesario();

        // Notificar en Actividad Reciente del Administrador
        if (appContext != null) {
            ActividadRepository.getInstance(appContext).registrarNuevoMensaje(
                    mensaje.clienteNombre,
                    mensaje.propiedadTitulo,
                    mensaje.mensaje
            );
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            long localId = dao.insert(mensaje);
            mensaje.id = localId;

            // Subir a Supabase
            SupabaseClient.enviarMensajeContacto(mensaje, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        mensaje.estadoSincronizacion = "SINCRONIZADO";
                        dao.update(mensaje);
                        Log.d(TAG, "Mensaje guardado en Supabase con éxito: " + mensaje.remoteId);
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Mensaje guardado localmente en Room, pendiente de subir a Supabase: " + error.getMessage());
                }
            });
        });
    }

    @Override
    public void sincronizarConNube(Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Subir mensajes pendientes locales a Supabase (omitiendo mensajes de prueba/demo)
            List<MensajeContacto> pendientes = dao.getPendientesDeSubir();
            if (pendientes != null && !pendientes.isEmpty()) {
                for (MensajeContacto p : pendientes) {
                    if (p.isDemoMessage()) {
                        dao.actualizarEstadoSincronizacion(p.id, "DEMO");
                        continue;
                    }
                    SupabaseClient.enviarMensajeContacto(p, new SupabaseClient.SupabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                p.estadoSincronizacion = "SINCRONIZADO";
                                dao.update(p);
                            });
                        }

                        @Override
                        public void onError(Exception error) {
                            Log.w(TAG, "Error al subir mensaje pendiente a Supabase: " + error.getMessage());
                        }
                    });
                }
            }

            // 2. Descargar mensajes remotos desde Supabase
            SupabaseClient.obtenerMensajes(new SupabaseClient.SupabaseCallback<List<MensajeContacto>>() {
                @Override
                public void onSuccess(List<MensajeContacto> remotos) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (remotos != null && !remotos.isEmpty()) {
                            List<String> remoteIds = new java.util.ArrayList<>();
                            // Calcular la hora real de llegada de cada mensaje (created_at de Supabase)
                            for (MensajeContacto r : remotos) {
                                if (r != null) r.timestamp = 0;
                            }
                            List<MensajeContacto> ordenados = new java.util.ArrayList<>();
                            for (MensajeContacto r : remotos) {
                                if (r == null) continue;
                                MensajeContacto local = r.remoteId != null ? dao.getMensajePorRemoteId(r.remoteId) : null;
                                boolean ok = r.recalcularTimestampDesdeOrigen();
                                if (!ok && local != null && local.timestamp > 0) {
                                    r.timestamp = local.timestamp;
                                }
                                ordenados.add(r);
                            }
                            // Insertar del más antiguo al más nuevo: así el último en llegar recibe el ID local más alto
                            java.util.Collections.sort(ordenados, (a, b) -> Long.compare(a.timestamp, b.timestamp));
                            for (MensajeContacto r : ordenados) {
                                MensajeContacto local = r.remoteId != null ? dao.getMensajePorRemoteId(r.remoteId) : null;
                                r.estadoSincronizacion = "SINCRONIZADO";

                                if (local != null) {
                                    r.id = local.id;
                                    dao.update(r);
                                } else {
                                    r.id = 0; // Para que Room autogenere el ID local
                                    dao.insert(r);
                                }
                                if (r.remoteId != null) {
                                    remoteIds.add(r.remoteId);
                                }
                            }

                            // Sanear y consolidar timestamps
                            sanearMensajesLocalesSync();

                            if (!remoteIds.isEmpty()) {
                                dao.eliminarNoEnServidor(remoteIds);
                            }
                            Log.d(TAG, "Sincronizados " + remotos.size() + " mensajes desde Supabase a Room.");
                        } else {
                            dao.eliminarTodosSincronizados();
                            sanearMensajesLocalesSync();
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "No se pudieron obtener mensajes desde Supabase (modo offline): " + error.getMessage());
                    sanearMensajesLocalesSync();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
        });
    }

    @Override
    public LiveData<List<MensajeContacto>> getAllMensajesLiveData() {
        return dao.getAllMensajesLiveData();
    }

    @Override
    public LiveData<Integer> getMensajesNoLeidosCount() {
        return dao.getMensajesNoLeidosCount();
    }

    @Override
    public void marcarComoLeido(MensajeContacto mensaje) {
        if (mensaje == null) return;
        mensaje.leido = true;

        // Notificar en Actividad Reciente del Administrador
        if (appContext != null) {
            ActividadRepository.getInstance(appContext).registrarMensajeVisto(
                    mensaje.clienteNombre,
                    mensaje.propiedadTitulo
            );
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.marcarComoLeido(mensaje.id);
            if (mensaje.remoteId != null && !mensaje.remoteId.isEmpty()) {
                SupabaseClient.marcarMensajeComoLeido(mensaje.remoteId, null);
            }
        });
    }

    @Override
    public void marcarComoNoLeido(MensajeContacto mensaje) {
        if (mensaje == null) return;
        mensaje.leido = false;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.marcarComoNoLeido(mensaje.id);
            if (mensaje.remoteId != null && !mensaje.remoteId.isEmpty()) {
                SupabaseClient.marcarMensajeComoNoLeido(mensaje.remoteId, null);
            }
        });
    }
}
