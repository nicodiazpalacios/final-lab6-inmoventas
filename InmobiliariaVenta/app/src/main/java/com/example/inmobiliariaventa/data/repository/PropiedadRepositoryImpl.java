package com.example.inmobiliariaventa.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.PropiedadDao;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.repository.PropiedadRepository;

import java.util.List;
import java.util.UUID;

public class PropiedadRepositoryImpl implements PropiedadRepository {

    private static final String TAG = "PropiedadRepoImpl";
    private final PropiedadDao propiedadDao;
    private final LiveData<List<Propiedad>> allPropiedades;

    private static long ultimaSincronizacion = 0;
    private static final long INTERVALO_MIN_SYNC_MS = 60_000; // 1 minuto

    public PropiedadRepositoryImpl(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        propiedadDao = database.propiedadDao();
        allPropiedades = propiedadDao.obtenerTodas();

        // Sincronización automática silenciosa al iniciar el repositorio si no se sincronizó recientemente
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaSincronizacion > INTERVALO_MIN_SYNC_MS) {
            ultimaSincronizacion = ahora;
            sincronizarConNube(null);
        }
    }

    @Override
    public LiveData<List<Propiedad>> getAllPropiedades() {
        return allPropiedades;
    }

    @Override
    public LiveData<Propiedad> getPropiedadPorId(String id) {
        return propiedadDao.obtenerPorId(id);
    }

    @Override
    public void sincronizarConNube(Runnable onComplete) {
        SupabaseClient.obtenerPropiedades(new SupabaseClient.SupabaseCallback<List<Propiedad>>() {
            @Override
            public void onSuccess(List<Propiedad> lista) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if (lista != null) {
                        for (Propiedad p : lista) {
                            p.estadoSincronizacion = "SINCRONIZADO";
                        }
                        propiedadDao.sincronizarConServidor(lista);
                    } else {
                        propiedadDao.sincronizarConServidor(new java.util.ArrayList<>());
                    }
                    Log.d(TAG, "Sincronizadas " + (lista != null ? lista.size() : 0) + " propiedades desde Supabase a Room.");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                Log.w(TAG, "No se pudo sincronizar propiedades desde Supabase (modo offline): " + error.getMessage());
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    @Override
    public void insert(Propiedad propiedad) {
        if (propiedad.id == null || propiedad.id.isEmpty()) {
            propiedad.id = UUID.randomUUID().toString();
        }
        propiedad.estadoSincronizacion = "PENDIENTE_SUBIR";

        AppDatabase.databaseWriteExecutor.execute(() -> {
            propiedadDao.insertar(propiedad);

            // Subir a Supabase en background
            SupabaseClient.insertarPropiedad(propiedad, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        propiedad.estadoSincronizacion = "SINCRONIZADO";
                        propiedadDao.actualizar(propiedad);
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Propiedad guardada localmente, pendiente de subir: " + error.getMessage());
                }
            });
        });
    }

    @Override
    public void update(Propiedad propiedad) {
        propiedad.estadoSincronizacion = "PENDIENTE_SUBIR";

        AppDatabase.databaseWriteExecutor.execute(() -> {
            propiedadDao.actualizar(propiedad);

            SupabaseClient.actualizarPropiedad(propiedad, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        propiedad.estadoSincronizacion = "SINCRONIZADO";
                        propiedadDao.actualizar(propiedad);
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Error actualizando propiedad en Supabase: " + error.getMessage());
                }
            });
        });
    }

    @Override
    public void delete(Propiedad propiedad) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            propiedadDao.eliminar(propiedad);
            SupabaseClient.eliminarPropiedad(propiedad.id, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Log.d(TAG, "Propiedad eliminada en Supabase.");
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Error eliminando propiedad en Supabase: " + error.getMessage());
                }
            });
        });
    }

    @Override
    public List<Propiedad> obtenerPendientesDeSubir() {
        return propiedadDao.obtenerPendientesDeSubir();
    }
}
