package com.example.inmobiliariaventa.ui.contratos;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.repository.ContratoRepository;
import com.example.inmobiliariaventa.data.repository.ContratoRepositoryImpl;
import com.example.inmobiliariaventa.data.local.AppDatabase;

public class ContratoViewModel extends AndroidViewModel {

    private ContratoRepository repository;

    public ContratoViewModel(@NonNull Application application) {
        super(application);
        repository = new ContratoRepositoryImpl(application);
    }

    public LiveData<Contrato> getContratoPorId(String id) {
        return repository.obtenerPorId(id);
    }

    public LiveData<List<Contrato>> getContratosDeCliente(String clienteId) {
        return repository.obtenerDeCliente(clienteId);
    }

    public void insert(Contrato contrato) {
        repository.insert(contrato);
    }

    public void update(Contrato contrato) {
        repository.update(contrato);
    }

    public void delete(Contrato contrato) {
        repository.delete(contrato);
    }

    public CompletableFuture<List<Contrato>> getPendientesDeSubir() {
        return CompletableFuture.supplyAsync(() -> 
            repository.obtenerPendientesDeSubir(), 
            AppDatabase.databaseWriteExecutor
        );
    }
}
