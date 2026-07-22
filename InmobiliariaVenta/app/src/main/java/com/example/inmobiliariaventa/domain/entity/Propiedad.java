package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "propiedades")
public class Propiedad {
    @PrimaryKey(autoGenerate = true)
    public int id;

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
}
