package com.example.inmobiliariaventa.ui.usuarios;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.example.inmobiliariaventa.domain.repository.UsuarioRepository;
import com.example.inmobiliariaventa.data.repository.UsuarioRepositoryImpl;
import com.example.inmobiliariaventa.data.local.AppDatabase;

public class UsuarioViewModel extends AndroidViewModel {

    private UsuarioRepository repository;

    public UsuarioViewModel(@NonNull Application application) {
        super(application);
        repository = new UsuarioRepositoryImpl(application);
    }

    public LiveData<Usuario> getUsuarioPorId(String id) {
        return repository.obtenerPorId(id);
    }

    public void insert(Usuario usuario) {
        repository.insert(usuario);
    }

    public void update(Usuario usuario) {
        repository.update(usuario);
    }

    public void delete(Usuario usuario) {
        repository.delete(usuario);
    }

    public CompletableFuture<List<Usuario>> getPendientesDeSubir() {
        return CompletableFuture.supplyAsync(() -> 
            repository.obtenerPendientesDeSubir(), 
            AppDatabase.databaseWriteExecutor
        );
    }
}
