package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
import java.util.List;

@Entity(tableName = "propiedades")
public class Propiedad {
    @PrimaryKey
    @NonNull
    public String id;

    public TipoPropiedad tipoPropiedad;
    public Disposicion disposicion;
    public double precio;
    public String domicilio;
    public String descripcion;
    public int ambientes;
    public int banos;
    public boolean tieneCochera;
    public boolean tienePatio;
    public boolean tieneBalcon;
    public boolean tieneAscensor;
    
    @ColumnInfo(name = "expensas")
    public Double expensas;
    
    @ColumnInfo(name = "piso")
    public Integer piso;
    
    @ColumnInfo(name = "depto")
    public String depto;

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    @ColumnInfo(name = "imagenes")
    public List<String> imagenes;
}
