package com.example.inmobiliariaventa.domain.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

/**
 * Entidad Room para el caché local permanente de ciudades/localidades de Georef
 * indexadas por provincia.
 */
@Entity(
    tableName = "ciudades_georef",
    primaryKeys = {"provinciaKey", "nombreCiudad"},
    indices = {@Index(value = "provinciaKey", name = "index_ciudades_georef_provinciaKey")}
)
public class CiudadGeoref {

    @NonNull
    @ColumnInfo(name = "provinciaKey")
    public String provinciaKey = "";

    @NonNull
    @ColumnInfo(name = "nombreCiudad")
    public String nombreCiudad = "";

    @ColumnInfo(name = "provinciaNombre")
    public String provinciaNombre;

    @ColumnInfo(name = "timestampCache")
    public long timestampCache;

    public CiudadGeoref() {
    }

    @Ignore
    public CiudadGeoref(@NonNull String provinciaKey, @NonNull String nombreCiudad, String provinciaNombre, long timestampCache) {
        this.provinciaKey = provinciaKey;
        this.nombreCiudad = nombreCiudad;
        this.provinciaNombre = provinciaNombre;
        this.timestampCache = timestampCache;
    }
}
