package com.example.inmobiliariaventa.ui.propiedades;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.repository.PropiedadRepository;
import com.example.inmobiliariaventa.data.repository.PropiedadRepositoryImpl;

public class PropiedadViewModel extends AndroidViewModel {

    private PropiedadRepository repository;
    private LiveData<List<Propiedad>> allPropiedades;

    public PropiedadViewModel(@NonNull Application application) {
        super(application);
        // Utilizamos la implementación concreta del repositorio
        repository = new PropiedadRepositoryImpl(application);
        allPropiedades = repository.getAllPropiedades();
    }

    public LiveData<List<Propiedad>> getAllPropiedades() {
        return allPropiedades;
    }

    public void insert(Propiedad propiedad) {
        repository.insert(propiedad);
    }

    public void update(Propiedad propiedad) {
        repository.update(propiedad);
    }

    public void delete(Propiedad propiedad) {
        repository.delete(propiedad);
    }

    public java.util.concurrent.CompletableFuture<List<Propiedad>> getPendientesDeSubir() {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> 
            repository.obtenerPendientesDeSubir(), 
            com.example.inmobiliariaventa.data.local.AppDatabase.databaseWriteExecutor
        );
    }
}
