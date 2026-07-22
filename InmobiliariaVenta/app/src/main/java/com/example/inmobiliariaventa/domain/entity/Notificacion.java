package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "notificaciones",
    foreignKeys = @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "usuario_id", onDelete = ForeignKey.CASCADE),
    indices = {@Index("usuario_id")}
)
public class Notificacion {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "usuario_id")
    @NonNull
    public String usuario_id;

    public String titulo;
    public String mensaje;
    public long fecha;
    public boolean isLeida;
    
    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;
}
