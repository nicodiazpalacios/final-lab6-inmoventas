package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.FavoritoDao;
import com.example.inmobiliariaventa.data.local.dao.UsuarioDao;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.example.inmobiliariaventa.domain.repository.FavoritoRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritoRepositoryImpl implements FavoritoRepository {

    private static final String TAG = "FavoritoRepository";
    private static volatile FavoritoRepositoryImpl INSTANCE;

    private final FavoritoDao favoritoDao;
    private final UsuarioDao usuarioDao;

    public static FavoritoRepositoryImpl getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FavoritoRepositoryImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FavoritoRepositoryImpl(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public FavoritoRepositoryImpl(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.favoritoDao = db.favoritoDao();
        this.usuarioDao = db.usuarioDao();
    }

    @Override
    public void agregarFavorito(Favorito favorito) {
        if (favorito == null || favorito.usuarioId == null || favorito.propiedadId == null) {
            return;
        }
        if (favorito.fechaGuardado <= 0) {
            favorito.fechaGuardado = System.currentTimeMillis();
        }
        favorito.estadoSincronizacion = "PENDIENTE_SUBIR";

        AppDatabase.databaseWriteExecutor.execute(() -> {
            favoritoDao.insertar(favorito);

            // Verificar si el usuario existe y sincronizarlo en Supabase si es necesario para evitar fallo de FK
            Usuario u = usuarioDao.obtenerUsuarioDirectoPorId(favorito.usuarioId);
            if (u != null && !"SINCRONIZADO".equals(u.estadoSincronizacion)) {
                SupabaseClient.registrarUsuario(u, new SupabaseClient.SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            u.estadoSincronizacion = "SINCRONIZADO";
                            usuarioDao.actualizar(u);
                        });
                        enviarFavoritoASupabase(favorito);
                    }

                    @Override
                    public void onError(Exception error) {
                        enviarFavoritoASupabase(favorito);
                    }
                });
            } else {
                enviarFavoritoASupabase(favorito);
            }
        });
    }

    private void enviarFavoritoASupabase(Favorito favorito) {
        SupabaseClient.agregarFavorito(favorito, new SupabaseClient.SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    favorito.estadoSincronizacion = "SINCRONIZADO";
                    favoritoDao.insertar(favorito);
                    Log.d(TAG, "Favorito sincronizado con Supabase: " + favorito.usuarioId + " -> " + favorito.propiedadId);
                });
            }

            @Override
            public void onError(Exception error) {
                Log.w(TAG, "Favorito guardado localmente, pendiente de subir: " + error.getMessage());
            }
        });
    }

    @Override
    public void agregarFavorito(String usuarioId, String propiedadId) {
        Favorito fav = new Favorito();
        fav.usuarioId = usuarioId;
        fav.propiedadId = propiedadId;
        fav.fechaGuardado = System.currentTimeMillis();
        agregarFavorito(fav);
    }

    @Override
    public void eliminarFavorito(String usuarioId, String propiedadId) {
        if (usuarioId == null || propiedadId == null) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            favoritoDao.eliminarPorIds(usuarioId, propiedadId);

            SupabaseClient.eliminarFavorito(usuarioId, propiedadId, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Log.d(TAG, "Favorito eliminado con éxito de Supabase: " + usuarioId + " -> " + propiedadId);
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Error eliminando favorito de Supabase: " + error.getMessage());
                }
            });
        });
    }

    @Override
    public void sincronizarConNube(String usuarioId, Runnable onComplete) {
        if (usuarioId == null || usuarioId.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Subir favoritos pendientes locales
            List<Favorito> pendientes = favoritoDao.obtenerPendientesDeSubir();
            if (pendientes != null && !pendientes.isEmpty()) {
                for (Favorito p : pendientes) {
                    if (usuarioId.equals(p.usuarioId)) {
                        SupabaseClient.agregarFavorito(p, new SupabaseClient.SupabaseCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    p.estadoSincronizacion = "SINCRONIZADO";
                                    favoritoDao.insertar(p);
                                });
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.w(TAG, "Fallo al subir favorito pendiente: " + error.getMessage());
                            }
                        });
                    }
                }
            }

            // 2. Descargar favoritos del usuario desde Supabase
            SupabaseClient.obtenerFavoritosPorUsuario(usuarioId, new SupabaseClient.SupabaseCallback<List<Favorito>>() {
                @Override
                public void onSuccess(List<Favorito> remotos) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Favorito> locales = favoritoDao.obtenerFavoritosCompletosDeUsuario(usuarioId);
                        Set<String> remotosIds = new HashSet<>();

                        if (remotos != null) {
                            for (Favorito r : remotos) {
                                remotosIds.add(r.propiedadId);
                                r.estadoSincronizacion = "SINCRONIZADO";
                                favoritoDao.insertar(r);
                            }
                        }

                        // Eliminar en local aquellos que ya estaban sincronizados pero ya no están en Supabase
                        if (locales != null) {
                            for (Favorito l : locales) {
                                if ("SINCRONIZADO".equals(l.estadoSincronizacion) && !remotosIds.contains(l.propiedadId)) {
                                    favoritoDao.eliminarPorIds(usuarioId, l.propiedadId);
                                }
                            }
                        }

                        Log.d(TAG, "Sincronizados " + (remotos != null ? remotos.size() : 0) + " favoritos desde Supabase a Room.");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "No se pudieron obtener favoritos desde Supabase (modo offline): " + error.getMessage());
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
        });
    }

    @Override
    public LiveData<Boolean> esFavoritoLiveData(String usuarioId, String propiedadId) {
        return favoritoDao.esFavoritoLiveData(usuarioId, propiedadId);
    }

    @Override
    public boolean esFavorito(String usuarioId, String propiedadId) {
        return favoritoDao.esFavorito(usuarioId, propiedadId);
    }

    @Override
    public LiveData<List<Propiedad>> obtenerFavoritosDeUsuarioLiveData(String usuarioId) {
        return favoritoDao.obtenerFavoritosDeUsuarioLiveData(usuarioId);
    }

    @Override
    public List<Propiedad> obtenerFavoritosDeUsuario(String usuarioId) {
        return favoritoDao.obtenerFavoritosDeUsuario(usuarioId);
    }

    @Override
    public List<String> obtenerIdsFavoritosDeUsuario(String usuarioId) {
        return favoritoDao.obtenerIdsFavoritosDeUsuario(usuarioId);
    }

    @Override
    public Favorito obtenerFavorito(String usuarioId, String propiedadId) {
        return favoritoDao.obtenerFavorito(usuarioId, propiedadId);
    }

    @Override
    public Long obtenerFechaGuardado(String usuarioId, String propiedadId) {
        return favoritoDao.obtenerFechaGuardado(usuarioId, propiedadId);
    }
}
