package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.ContratoDao;
import com.example.inmobiliariaventa.data.local.dao.PagoDao;
import com.example.inmobiliariaventa.data.local.dao.PropiedadDao;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.Pago;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Repositorio de Contratos con arquitectura Offline-First y política de persistencia segura:
 * - Room actúa como única fuente de verdad (Single Source of Truth).
 * - Sincronización al inicio de la app restringida exclusivamente a usuarios con rol "Admin".
 * - Política no destructiva: jamás borra ni sobrescribe contratos locales en Room si la red
 *   no está disponible, si la petición remota falla o si el registro local está en estado PENDIENTE_SUBIR.
 */
public class ContratoRepository {

    private static final String TAG = "ContratoRepository";
    private static final String PREFS_NAME = "app_prefs";
    private static volatile ContratoRepository INSTANCE;

    private final Context appContext;
    private final ContratoDao contratoDao;
    private final PagoDao pagoDao;
    private final PropiedadDao propiedadDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SyncCallback {
        void onComplete(boolean sincronizadoConNube);
        default void onError(Exception error) {}
    }

    public static ContratoRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ContratoRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ContratoRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public ContratoRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.appContext);
        this.contratoDao = db.contratoDao();
        this.pagoDao = db.pagoDao();
        this.propiedadDao = db.propiedadDao();
    }

    // ==========================================
    // CONSULTAS LOCALES (SINGLE SOURCE OF TRUTH)
    // ==========================================

    public LiveData<List<Contrato>> obtenerTodosLiveData() {
        return contratoDao.obtenerTodosLiveData();
    }

    public List<Contrato> obtenerTodosSync() {
        return contratoDao.obtenerTodosSync();
    }

    public LiveData<Contrato> obtenerPorIdLiveData(String id) {
        return contratoDao.obtenerPorId(id);
    }

    public Contrato obtenerPorIdSync(String id) {
        return contratoDao.obtenerPorIdSync(id);
    }

    // ==========================================
    // VALIDACIÓN DE ROL ADMIN Y SINCRONIZACIÓN
    // ==========================================

    /**
     * Verifica si el usuario actualmente autenticado tiene rol de "Admin".
     */
    public boolean esUsuarioActualAdmin() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String rol = prefs.getString("user_rol", "");
        return isLoggedIn && rol != null && rol.trim().toUpperCase(Locale.ROOT).contains("ADMIN");
    }

    /**
     * Ejecuta la sincronización de contratos al inicio de la aplicación
     * ÚNICAMENTE si el usuario autenticado posee rol de "Admin".
     */
    public void sincronizarAlIniciarSiEsAdmin() {
        sincronizarAlIniciarSiEsAdmin(null);
    }

    public void sincronizarAlIniciarSiEsAdmin(@Nullable SyncCallback callback) {
        if (!esUsuarioActualAdmin()) {
            Log.d(TAG, "Sincronización inicial omitida: el usuario actual no tiene rol Admin.");
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(false));
            }
            return;
        }
        Log.d(TAG, "Usuario Admin verificado al inicio: iniciando sincronización segura de contratos...");
        sincronizarConNube(callback);
    }

    public void sincronizarConNube(@Nullable Runnable onComplete) {
        sincronizarConNube(new SyncCallback() {
            @Override
            public void onComplete(boolean sincronizadoConNube) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Sincronización bidireccional segura (Push Pendientes -> Pull No Destructivo):
     * Si no hay red o falla el servidor, mantiene intactos todos los contratos locales en Room.
     */
    public void sincronizarConNube(@Nullable SyncCallback callback) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            Log.w(TAG, "Sin conexión de red: se mantienen intactos los contratos locales en Room (Offline-First).");
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(false));
            }
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Subir contratos locales pendientes de sincronización (PENDIENTE_SUBIR)
            try {
                List<Contrato> contratosPendientes = contratoDao.obtenerPendientesDeSubir();
                if (contratosPendientes != null && !contratosPendientes.isEmpty()) {
                    for (Contrato pendiente : contratosPendientes) {
                        SupabaseClient.guardarContrato(pendiente, new SupabaseClient.SupabaseCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    pendiente.estadoSincronizacion = "SINCRONIZADO";
                                    contratoDao.insertar(pendiente);
                                });
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.w(TAG, "Contrato local sigue pendiente de subir (" + pendiente.id + "): " + error.getMessage());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error procesando cola local de contratos pendientes: " + e.getMessage());
            }

            // 2. Subir pagos locales pendientes de sincronización
            try {
                List<Pago> pagosPendientes = pagoDao.obtenerPendientesDeSubir();
                if (pagosPendientes != null && !pagosPendientes.isEmpty()) {
                    for (Pago pagoPend : pagosPendientes) {
                        SupabaseClient.guardarPago(pagoPend, new SupabaseClient.SupabaseCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    pagoPend.estadoSincronizacion = "SINCRONIZADO";
                                    pagoDao.insertar(pagoPend);
                                });
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.w(TAG, "Pago local sigue pendiente de subir (" + pagoPend.id + "): " + error.getMessage());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error procesando cola local de pagos pendientes: " + e.getMessage());
            }

            // 3. Descargar contratos remotos y aplicar Upsert Seguro en Room (nunca destructivo)
            SupabaseClient.obtenerContratos(new SupabaseClient.SupabaseCallback<List<Contrato>>() {
                @Override
                public void onSuccess(List<Contrato> remotos) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            if (remotos != null && !remotos.isEmpty()) {
                                List<Propiedad> todasProps = propiedadDao.obtenerTodasSync();
                                for (Contrato c : remotos) {
                                    if (c == null) continue;
                                    if ((c.propiedadTitulo == null || c.propiedadTitulo.isEmpty())
                                            && c.propiedadId != null && todasProps != null) {
                                        for (Propiedad p : todasProps) {
                                            if (c.propiedadId.equals(p.id) && p.titulo != null) {
                                                c.propiedadTitulo = p.titulo;
                                                break;
                                            }
                                        }
                                    }
                                }
                                // Upsert seguro: no borra datos locales ni pisa registros PENDIENTE_SUBIR
                                contratoDao.upsertSeguroDesdeServidor(remotos);
                                Log.d(TAG, "Contratos sincronizados de forma segura con Room (" + remotos.size() + " registros).");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error haciendo merge seguro de contratos en Room: " + e.getMessage());
                        }

                        // 4. Descargar pagos remotos y hacer Upsert Seguro en Room
                        SupabaseClient.obtenerPagos(new SupabaseClient.SupabaseCallback<List<Pago>>() {
                            @Override
                            public void onSuccess(List<Pago> pagosRemotos) {
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    try {
                                        if (pagosRemotos != null && !pagosRemotos.isEmpty()) {
                                            pagoDao.upsertSeguroDesdeServidor(pagosRemotos);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error haciendo merge seguro de pagos en Room: " + e.getMessage());
                                    }
                                    if (callback != null) {
                                        mainHandler.post(() -> callback.onComplete(true));
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception error) {
                                Log.w(TAG, "Error obteniendo pagos remotos; se conservan pagos locales: " + error.getMessage());
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onComplete(true));
                                }
                            }
                        });
                    });
                }

                @Override
                public void onError(Exception error) {
                    // Política de persistencia segura: ante cualquier error remoto, NO tocar ni borrar Room
                    Log.w(TAG, "Fallo en llamada remota de contratos. Se mantienen intactos los contratos locales en Room: " + error.getMessage());
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onError(error);
                            callback.onComplete(false);
                        });
                    }
                }
            });
        });
    }

    // ==========================================
    // ESCRITURA OFFLINE-FIRST DE CONTRATOS
    // ==========================================

    /**
     * Guarda o actualiza un contrato primero en Room con estado PENDIENTE_SUBIR
     * y luego intenta sincronizarlo en segundo plano si hay red disponible.
     */
    public void guardarContrato(@NonNull Contrato contrato, @Nullable SyncCallback callback) {
        if (contrato.id == null || contrato.id.trim().isEmpty()) {
            contrato.id = UUID.randomUUID().toString();
        }
        if (contrato.timestamp <= 0) {
            contrato.timestamp = System.currentTimeMillis();
        }
        contrato.estadoSincronizacion = "PENDIENTE_SUBIR";

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Persistencia inmediata en Room (Offline-First)
            contratoDao.insertar(contrato);

            // 2. Si no hay conexión, finalizar con éxito local
            if (!NetworkUtils.isNetworkAvailable(appContext)) {
                Log.d(TAG, "Contrato guardado localmente en modo offline (PENDIENTE_SUBIR): " + contrato.id);
                if (callback != null) {
                    mainHandler.post(() -> callback.onComplete(false));
                }
                return;
            }

            // 3. Si hay red, subir a Supabase en segundo plano
            SupabaseClient.guardarContrato(contrato, new SupabaseClient.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        contrato.estadoSincronizacion = "SINCRONIZADO";
                        contratoDao.insertar(contrato);
                        if (callback != null) {
                            mainHandler.post(() -> callback.onComplete(true));
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Contrato preservado en Room como PENDIENTE_SUBIR tras error remoto: " + error.getMessage());
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onError(error);
                            callback.onComplete(false);
                        });
                    }
                }
            });
        });
    }
}
