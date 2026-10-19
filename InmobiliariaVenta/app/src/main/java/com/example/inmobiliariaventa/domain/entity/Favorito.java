package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

import androidx.room.Ignore;

@Entity(
    tableName = "favoritos",
    primaryKeys = {"usuarioId", "propiedadId"},
    indices = {@Index("propiedadId"), @Index("usuarioId")}
)
public class Favorito {
    @NonNull
    @SerializedName(value = "usuarioid", alternate = {"usuarioId"})
    public String usuarioId;

    @NonNull
    @SerializedName(value = "propiedadid", alternate = {"propiedadId"})
    public String propiedadId;

    @SerializedName(value = "fechaguardado", alternate = {"fechaGuardado"})
    public long fechaGuardado;
    
    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    public Favorito() {}

    @Ignore
    public Favorito(@NonNull String usuarioId, @NonNull String propiedadId, long fechaGuardado, String estadoSincronizacion) {
        this.usuarioId = usuarioId;
        this.propiedadId = propiedadId;
        this.fechaGuardado = fechaGuardado;
        this.estadoSincronizacion = estadoSincronizacion;
    }
}
