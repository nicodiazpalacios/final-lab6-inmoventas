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
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.mensajeContactoDao();
    }

    @Override
    public void enviarMensaje(MensajeContacto mensaje) {
        if (mensaje == null) return;

        if (mensaje.remoteId == null || mensaje.remoteId.trim().isEmpty()) {
            mensaje.remoteId = UUID.randomUUID().toString();
        }
        mensaje.estadoSincronizacion = "PENDIENTE_SUBIR";
        mensaje.calcularTimestampSiEsNecesario();

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
            // 1. Subir mensajes pendientes locales a Supabase
            List<MensajeContacto> pendientes = dao.getPendientesDeSubir();
            if (pendientes != null && !pendientes.isEmpty()) {
                for (MensajeContacto p : pendientes) {
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
                            for (MensajeContacto r : remotos) {
                                r.calcularTimestampSiEsNecesario();
                                r.estadoSincronizacion = "SINCRONIZADO";

                                MensajeContacto local = r.remoteId != null ? dao.getMensajePorRemoteId(r.remoteId) : null;
                                if (local != null) {
                                    r.id = local.id;
                                    dao.update(r);
                                } else {
                                    r.id = 0; // Para que Room autogenere el ID local
                                    dao.insert(r);
                                }
                            }
                            Log.d(TAG, "Sincronizados " + remotos.size() + " mensajes desde Supabase a Room.");
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "No se pudieron obtener mensajes desde Supabase (modo offline): " + error.getMessage());
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
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.marcarComoLeido(mensaje.id);
            if (mensaje.remoteId != null && !mensaje.remoteId.isEmpty()) {
                SupabaseClient.marcarMensajeComoLeido(mensaje.remoteId, null);
            }
        });
    }
}
