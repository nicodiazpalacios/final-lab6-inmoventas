package com.example.inmobiliariaventa.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import java.util.List;

public interface MensajeContactoRepository {
    void enviarMensaje(MensajeContacto mensaje);
    void sincronizarConNube(Runnable onComplete);
    LiveData<List<MensajeContacto>> getAllMensajesLiveData();
    LiveData<Integer> getMensajesNoLeidosCount();
    void marcarComoLeido(MensajeContacto mensaje);
}
