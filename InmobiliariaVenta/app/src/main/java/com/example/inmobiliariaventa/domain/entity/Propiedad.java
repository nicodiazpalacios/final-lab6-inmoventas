package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@Entity(tableName = "propiedades")
public class Propiedad {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id;

    @ColumnInfo(name = "titulo")
    @SerializedName("titulo")
    public String titulo;

    @SerializedName(value = "tipoPropiedad", alternate = {"tipopropiedad"})
    public TipoPropiedad tipoPropiedad;

    @SerializedName("disposicion")
    public Disposicion disposicion;

    @SerializedName("precio")
    public double precio;

    @SerializedName("domicilio")
    public String domicilio;

    @SerializedName("descripcion")
    public String descripcion;

    @ColumnInfo(name = "ciudad")
    @SerializedName("ciudad")
    public String ciudad;

    @ColumnInfo(name = "estado_provincia")
    @SerializedName(value = "estadoProvincia", alternate = {"estado_provincia"})
    public String estadoProvincia;

    @SerializedName("ambientes")
    public int ambientes;

    @SerializedName("banos")
    public int banos;

    @ColumnInfo(name = "superficie_m2")
    @SerializedName(value = "superficieM2", alternate = {"superficie_m2"})
    public Double superficieM2;

    @SerializedName(value = "tieneCochera", alternate = {"tienecochera"})
    public boolean tieneCochera;

    @SerializedName(value = "tienePatio", alternate = {"tienepatio"})
    public boolean tienePatio;

    @SerializedName(value = "tieneBalcon", alternate = {"tienebalcon"})
    public boolean tieneBalcon;

    @SerializedName(value = "tieneAscensor", alternate = {"tieneascensor"})
    public boolean tieneAscensor;
    
    @ColumnInfo(name = "expensas")
    @SerializedName("expensas")
    public Double expensas;
    
    @ColumnInfo(name = "piso")
    @SerializedName("piso")
    public Integer piso;
    
    @ColumnInfo(name = "depto")
    @SerializedName("depto")
    public String depto;

    @ColumnInfo(name = "estado")
    @SerializedName("estado")
    public String estado; // "DISPONIBLE", "RESERVADA", "VENDIDA"

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    @ColumnInfo(name = "imagenes")
    @SerializedName("imagenes")
    public List<String> imagenes;

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    public String createdAt;
}
