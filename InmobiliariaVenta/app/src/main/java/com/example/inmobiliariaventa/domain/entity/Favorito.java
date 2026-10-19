package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

@Entity(
    tableName = "favoritos",
    primaryKeys = {"usuarioId", "propiedadId"},
    foreignKeys = {
        @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "usuarioId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Propiedad.class, parentColumns = "id", childColumns = "propiedadId", onDelete = ForeignKey.CASCADE)
    },
    indices = {@Index("propiedadId")}
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
}
