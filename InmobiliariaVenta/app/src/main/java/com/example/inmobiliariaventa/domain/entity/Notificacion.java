package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;

@Entity(
    tableName = "notificaciones",
    foreignKeys = @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "usuario_id", onDelete = ForeignKey.CASCADE),
    indices = {@Index("usuario_id")}
)
public class Notificacion {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "usuario_id")
    public int usuario_id;

    public String titulo;
    public String mensaje;
    public long fecha;
    public boolean isLeida;
}
