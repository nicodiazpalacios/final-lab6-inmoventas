package com.example.inmobiliariaventa.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.domain.repository.UsuarioRepository;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.UsuarioDao;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import java.util.List;
import java.util.UUID;

public class UsuarioRepositoryImpl implements UsuarioRepository {

    private UsuarioDao usuarioDao;

    public UsuarioRepositoryImpl(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        usuarioDao = database.usuarioDao();
    }

    @Override
    public LiveData<Usuario> obtenerPorId(String id) {
        return usuarioDao.obtenerPorId(id);
    }

    @Override
    public void insert(Usuario usuario) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (usuario.id == null || usuario.id.isEmpty()) {
                usuario.id = UUID.randomUUID().toString();
            }
            usuario.estadoSincronizacion = "PENDIENTE_SUBIR";
            usuarioDao.insertar(usuario);
        });
    }

    @Override
    public void update(Usuario usuario) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            usuario.estadoSincronizacion = "PENDIENTE_SUBIR";
            usuarioDao.actualizar(usuario);
        });
    }

    @Override
    public void delete(Usuario usuario) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            usuarioDao.eliminar(usuario);
        });
    }

    @Override
    public List<Usuario> obtenerPendientesDeSubir() {
        return usuarioDao.obtenerPendientesDeSubir();
    }
}
