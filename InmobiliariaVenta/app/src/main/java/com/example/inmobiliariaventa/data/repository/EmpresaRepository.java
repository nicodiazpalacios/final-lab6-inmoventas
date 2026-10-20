package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.ConfiguracionEmpresaDao;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;

public class EmpresaRepository {

    private static final String PREF_NAME = "empresa_config_prefs";
    private static final String KEY_NOMBRE_EMPRESA = "key_nombre_empresa";
    private static final String KEY_TELEFONO_WHATSAPP = "key_telefono_whatsapp";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_DIRECCION = "key_direccion";

    private static final String DEFAULT_NOMBRE = "Inmobiliaria Venta";
    private static final String DEFAULT_WHATSAPP = "+54 9 11 5842-1102";
    private static final String DEFAULT_EMAIL = "contacto@inmoventas.com";
    private static final String DEFAULT_DIRECCION = "Av. del Libertador 2450, Buenos Aires";

    private final ConfiguracionEmpresaDao dao;
    private final SharedPreferences prefs;

    public EmpresaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.configuracionEmpresaDao();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public LiveData<ConfiguracionEmpresa> getConfiguracionLiveData() {
        return dao.getConfiguracionLiveData();
    }

    public String getNombreEmpresaInmediato() {
        return prefs.getString(KEY_NOMBRE_EMPRESA, DEFAULT_NOMBRE);
    }

    public String getTelefonoWhatsappInmediato() {
        return prefs.getString(KEY_TELEFONO_WHATSAPP, DEFAULT_WHATSAPP);
    }

    public String getEmailInmediato() {
        return prefs.getString(KEY_EMAIL, DEFAULT_EMAIL);
    }

    public String getDireccionInmediata() {
        return prefs.getString(KEY_DIRECCION, DEFAULT_DIRECCION);
    }

    public void guardarConfiguracion(String telefono, String email, String direccion) {
        guardarConfiguracion(getNombreEmpresaInmediato(), telefono, email, direccion);
    }

    public void guardarConfiguracion(String nombre, String telefono, String email, String direccion) {
        String finalNombre = (nombre != null && !nombre.trim().isEmpty()) ? nombre : DEFAULT_NOMBRE;
        String finalTelefono = (telefono != null && !telefono.trim().isEmpty()) ? telefono : DEFAULT_WHATSAPP;
        String finalEmail = (email != null && !email.trim().isEmpty()) ? email : DEFAULT_EMAIL;
        String finalDireccion = (direccion != null && !direccion.trim().isEmpty()) ? direccion : DEFAULT_DIRECCION;

        // Guardado rápido en SharedPreferences para acceso síncrono inmediato
        prefs.edit()
                .putString(KEY_NOMBRE_EMPRESA, finalNombre)
                .putString(KEY_TELEFONO_WHATSAPP, finalTelefono)
                .putString(KEY_EMAIL, finalEmail)
                .putString(KEY_DIRECCION, finalDireccion)
                .apply();

        ConfiguracionEmpresa config = new ConfiguracionEmpresa(finalNombre, finalTelefono, finalEmail, finalDireccion);

        // Persistencia formal en Room Database
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insertarOActualizar(config);
        });

        // Sincronizar en tiempo real con Supabase
        SupabaseClient.actualizarConfiguracionEmpresa(config, null);
    }

    public interface OnConfigLoadedListener {
        void onLoaded(ConfiguracionEmpresa config);
    }

    public void cargarConfiguracionAsync(OnConfigLoadedListener listener) {
        // Consultar Supabase en segundo plano para obtener siempre los datos vigentes en la nube
        SupabaseClient.obtenerConfiguracionEmpresa(new SupabaseClient.SupabaseCallback<ConfiguracionEmpresa>() {
            @Override
            public void onSuccess(ConfiguracionEmpresa remoto) {
                if (remoto != null) {
                    prefs.edit()
                            .putString(KEY_NOMBRE_EMPRESA, remoto.nombreEmpresa)
                            .putString(KEY_TELEFONO_WHATSAPP, remoto.telefonoWhatsapp)
                            .putString(KEY_EMAIL, remoto.email)
                            .putString(KEY_DIRECCION, remoto.direccion)
                            .apply();

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        dao.insertarOActualizar(remoto);
                        if (listener != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onLoaded(remoto));
                        }
                    });
                } else {
                    cargarDesdeLocal(listener);
                }
            }

            @Override
            public void onError(Exception error) {
                cargarDesdeLocal(listener);
            }
        });
    }

    private void cargarDesdeLocal(OnConfigLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ConfiguracionEmpresa config = dao.getConfiguracionDirecta();
            if (config == null) {
                config = new ConfiguracionEmpresa(
                        getNombreEmpresaInmediato(),
                        getTelefonoWhatsappInmediato(),
                        getEmailInmediato(),
                        getDireccionInmediata()
                );
                dao.insertarOActualizar(config);
            }
            ConfiguracionEmpresa finalConfig = config;
            if (listener != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onLoaded(finalConfig));
            }
        });
    }
}
