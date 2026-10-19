package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "contratos",
    indices = {@Index("propiedadId"), @Index("clienteId")}
)
public class Contrato {
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String propiedadId;
    
    @NonNull
    public String clienteId;

    public Double montoTotal;
    public String fechaVenta;
    public String rutaLocalPdf;
    public String urlNubePdf;

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;
}
