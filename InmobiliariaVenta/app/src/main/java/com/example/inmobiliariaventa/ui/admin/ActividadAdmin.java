package com.example.inmobiliariaventa.ui.admin;

public class ActividadAdmin {

    public enum TipoActividad {
        NUEVA_PROPIEDAD,
        CAMBIO_PRECIO,
        DADA_DE_BAJA
    }

    private final String id;
    private final String nombrePropiedad;
    private final TipoActividad tipo;
    private final String horario;
    private final String precio;
    private final String detalle;

    public ActividadAdmin(String id, String nombrePropiedad, TipoActividad tipo,
                          String horario, String precio, String detalle) {
        this.id = id;
        this.nombrePropiedad = nombrePropiedad;
        this.tipo = tipo;
        this.horario = horario;
        this.precio = precio;
        this.detalle = detalle;
    }

    public String getId() {
        return id;
    }

    public String getNombrePropiedad() {
        return nombrePropiedad;
    }

    public TipoActividad getTipo() {
        return tipo;
    }

    public String getHorario() {
        return horario;
    }

    public String getPrecio() {
        return precio;
    }

    public String getDetalle() {
        return detalle;
    }
}
