package com.example.inmobiliariaventa.domain.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "notificaciones")
public class Notificacion {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id;

    @ColumnInfo(name = "usuario_id")
    @SerializedName("usuario_id")
    public String usuario_id;

    @SerializedName("titulo")
    public String titulo;

    @SerializedName("mensaje")
    public String mensaje;

    @SerializedName("fecha")
    public long fecha;

    @SerializedName(value = "isleida", alternate = {"isLeida"})
    public boolean isLeida;

    @SerializedName("categoria")
    public String categoria;

    @SerializedName("referencia_id")
    public String referencia_id;

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    public Notificacion() {
        this.id = "";
    }

    @Ignore
    public Notificacion(@NonNull String id, String usuarioId, String titulo, String mensaje,
                        long fecha, boolean isLeida, String categoria, String referenciaId) {
        this.id = id;
        this.usuario_id = usuarioId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.isLeida = isLeida;
        this.categoria = categoria;
        this.referencia_id = referenciaId;
        this.estadoSincronizacion = "PENDIENTE_SUBIR";
    }
}
