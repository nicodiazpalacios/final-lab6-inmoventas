package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "nombre")
    public String nombre;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "tokenSesion")
    public String tokenSesion;
    
    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;
}
