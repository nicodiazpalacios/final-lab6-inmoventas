package com.example.inmobiliariaventa.domain.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "configuracion_empresa")
public class ConfiguracionEmpresa {
    @PrimaryKey
    @NonNull
    public String id = "empresa_principal";

    @ColumnInfo(name = "nombreEmpresa")
    public String nombreEmpresa = "Inmobiliaria Venta";

    @ColumnInfo(name = "telefonoWhatsapp")
    public String telefonoWhatsapp = "+54 9 11 5842-1102";

    @ColumnInfo(name = "email")
    public String email = "contacto@inmoventas.com";

    @ColumnInfo(name = "direccion")
    public String direccion = "Av. del Libertador 2450, Buenos Aires";

    public ConfiguracionEmpresa() {
        this.id = "empresa_principal";
    }

    @androidx.room.Ignore
    public ConfiguracionEmpresa(String telefonoWhatsapp, String email, String direccion) {
        this.id = "empresa_principal";
        this.nombreEmpresa = "Inmobiliaria Venta";
        this.telefonoWhatsapp = telefonoWhatsapp;
        this.email = email;
        this.direccion = direccion;
    }
}
