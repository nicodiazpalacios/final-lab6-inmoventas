package com.example.inmobiliariaventa.domain.repository;

import com.example.inmobiliariaventa.domain.entity.Contrato;
import java.util.List;

import androidx.lifecycle.LiveData;

public interface ContratoRepository {
    LiveData<Contrato> obtenerPorId(String id);
    LiveData<List<Contrato>> obtenerDeCliente(String clienteId);
    void insert(Contrato contrato);
    void update(Contrato contrato);
    void delete(Contrato contrato);
    List<Contrato> obtenerPendientesDeSubir();
}
