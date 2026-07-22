package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "contratos",
    foreignKeys = {
        @ForeignKey(entity = Propiedad.class, parentColumns = "id", childColumns = "propiedadId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "clienteId", onDelete = ForeignKey.CASCADE)
    },
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
