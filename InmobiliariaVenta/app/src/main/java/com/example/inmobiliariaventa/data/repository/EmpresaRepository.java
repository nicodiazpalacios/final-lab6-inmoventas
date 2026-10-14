package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.ConfiguracionEmpresaDao;
import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;

public class EmpresaRepository {

    private static final String PREF_NAME = "empresa_config_prefs";
    private static final String KEY_TELEFONO_WHATSAPP = "key_telefono_whatsapp";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_DIRECCION = "key_direccion";
    private static final String DEFAULT_WHATSAPP = "+54 9 11 5842-1102";

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

    public String getTelefonoWhatsappInmediato() {
        return prefs.getString(KEY_TELEFONO_WHATSAPP, DEFAULT_WHATSAPP);
    }

    public String getEmailInmediato() {
        return prefs.getString(KEY_EMAIL, "contacto@inmoventas.com");
    }

    public String getDireccionInmediata() {
        return prefs.getString(KEY_DIRECCION, "Av. del Libertador 2450, Buenos Aires");
    }

    public void guardarConfiguracion(String telefono, String email, String direccion) {
        // Guardado rápido en SharedPreferences para acceso síncrono inmediato
        prefs.edit()
                .putString(KEY_TELEFONO_WHATSAPP, telefono)
                .putString(KEY_EMAIL, email)
                .putString(KEY_DIRECCION, direccion)
                .apply();

        // Persistencia formal en Room Database
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ConfiguracionEmpresa config = new ConfiguracionEmpresa(telefono, email, direccion);
            dao.insertarOActualizar(config);
        });
    }

    public interface OnConfigLoadedListener {
        void onLoaded(ConfiguracionEmpresa config);
    }

    public void cargarConfiguracionAsync(OnConfigLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ConfiguracionEmpresa config = dao.getConfiguracionDirecta();
            if (config == null) {
                config = new ConfiguracionEmpresa(
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
