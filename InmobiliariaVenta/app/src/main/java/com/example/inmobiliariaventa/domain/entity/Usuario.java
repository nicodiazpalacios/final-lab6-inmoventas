package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id;

    @ColumnInfo(name = "nombre")
    @SerializedName("nombre")
    public String nombre;

    @ColumnInfo(name = "email")
    @SerializedName("email")
    public String email;

    @ColumnInfo(name = "telefono")
    @SerializedName("telefono")
    public String telefono;

    @ColumnInfo(name = "rol")
    @SerializedName("rol")
    public String rol; // "ADMIN" o "CLIENTE"

    @ColumnInfo(name = "tokenSesion")
    @SerializedName(value = "tokenSesion", alternate = {"tokensesion"})
    public String tokenSesion;
    
    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    @ColumnInfo(name = "foto_perfil")
    @SerializedName(value = "fotoperfil", alternate = {"foto_perfil", "fotoPerfil"})
    public String fotoPerfil;
}
