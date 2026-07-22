package com.example.inmobiliariaventa.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.domain.repository.PropiedadRepository;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.PropiedadDao;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import java.util.List;

public class PropiedadRepositoryImpl implements PropiedadRepository {

    private PropiedadDao propiedadDao;
    private LiveData<List<Propiedad>> allPropiedades;

    public PropiedadRepositoryImpl(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        propiedadDao = database.propiedadDao();
        allPropiedades = propiedadDao.obtenerTodas();
    }

    @Override
    public LiveData<List<Propiedad>> getAllPropiedades() {
        return allPropiedades;
    }

    @Override
    public void insert(Propiedad propiedad) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (propiedad.id == null || propiedad.id.isEmpty()) {
                propiedad.id = java.util.UUID.randomUUID().toString();
            }
            propiedad.estadoSincronizacion = "PENDIENTE_SUBIR";
            propiedadDao.insertar(propiedad);
        });
    }

    @Override
    public void update(Propiedad propiedad) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            propiedad.estadoSincronizacion = "PENDIENTE_SUBIR";
            propiedadDao.actualizar(propiedad);
        });
    }

    @Override
    public void delete(Propiedad propiedad) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            propiedadDao.eliminar(propiedad);
        });
    }

    @Override
    public List<Propiedad> obtenerPendientesDeSubir() {
        return propiedadDao.obtenerPendientesDeSubir();
    }
}
