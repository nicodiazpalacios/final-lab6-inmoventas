package com.example.inmobiliariaventa.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import java.util.List;

public interface FavoritoRepository {
    void agregarFavorito(Favorito favorito);
    void agregarFavorito(String usuarioId, String propiedadId);
    void eliminarFavorito(String usuarioId, String propiedadId);
    void sincronizarConNube(String usuarioId, Runnable onComplete);
    LiveData<Boolean> esFavoritoLiveData(String usuarioId, String propiedadId);
    boolean esFavorito(String usuarioId, String propiedadId);
    LiveData<List<Propiedad>> obtenerFavoritosDeUsuarioLiveData(String usuarioId);
    List<Propiedad> obtenerFavoritosDeUsuario(String usuarioId);
    List<String> obtenerIdsFavoritosDeUsuario(String usuarioId);
    Favorito obtenerFavorito(String usuarioId, String propiedadId);
    Long obtenerFechaGuardado(String usuarioId, String propiedadId);
}
