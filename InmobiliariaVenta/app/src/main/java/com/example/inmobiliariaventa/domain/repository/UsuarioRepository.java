package com.example.inmobiliariaventa.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import java.util.List;

public interface UsuarioRepository {
    LiveData<Usuario> obtenerPorId(String id);
    void insert(Usuario usuario);
    void update(Usuario usuario);
    void delete(Usuario usuario);
    List<Usuario> obtenerPendientesDeSubir();
}
