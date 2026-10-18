package com.example.inmobiliariaventa.domain.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "mensajes_contacto")
public class MensajeContacto {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "cliente_nombre")
    @SerializedName("cliente_nombre")
    public String clienteNombre;

    @ColumnInfo(name = "cliente_telefono")
    @SerializedName("cliente_telefono")
    public String clienteTelefono;

    @ColumnInfo(name = "cliente_email")
    @SerializedName("cliente_email")
    public String clienteEmail;

    @ColumnInfo(name = "propiedad_id")
    @SerializedName("propiedad_id")
    public String propiedadId;

    @ColumnInfo(name = "propiedad_titulo")
    @SerializedName("propiedad_titulo")
    public String propiedadTitulo;

    @ColumnInfo(name = "propiedad_direccion")
    @SerializedName("propiedad_direccion")
    public String propiedadDireccion;

    @ColumnInfo(name = "mensaje")
    @SerializedName("mensaje")
    public String mensaje;

    @ColumnInfo(name = "fecha")
    @SerializedName("fecha")
    public String fecha; // "dd/MM/yyyy"

    @ColumnInfo(name = "hora")
    @SerializedName("hora")
    public String hora;  // "11:45 AM"

    @ColumnInfo(name = "timestamp")
    @SerializedName("timestamp")
    public long timestamp;

    @ColumnInfo(name = "leido")
    @SerializedName("leido")
    public boolean leido;

    @ColumnInfo(name = "origen")
    @SerializedName("origen")
    public String origen; // "App Home Finder"

    public MensajeContacto() {
    }

    @androidx.room.Ignore
    public MensajeContacto(String clienteNombre, String clienteTelefono, String clienteEmail,
                           String propiedadId, String propiedadTitulo, String propiedadDireccion,
                           String mensaje, String fecha, String hora, long timestamp, boolean leido) {
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.propiedadId = propiedadId;
        this.propiedadTitulo = propiedadTitulo;
        this.propiedadDireccion = propiedadDireccion;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.hora = hora;
        this.timestamp = timestamp;
        this.leido = leido;
        this.origen = "App Home Finder";
    }
}
