package com.example.inmobiliariaventa.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;

@Dao
public interface ConfiguracionEmpresaDao {
    @Query("SELECT * FROM configuracion_empresa WHERE id = 'empresa_principal' LIMIT 1")
    ConfiguracionEmpresa getConfiguracionDirecta();

    @Query("SELECT * FROM configuracion_empresa WHERE id = 'empresa_principal' LIMIT 1")
    LiveData<ConfiguracionEmpresa> getConfiguracionLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarOActualizar(ConfiguracionEmpresa config);
}
