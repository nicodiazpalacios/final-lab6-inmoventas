package com.example.inmobiliariaventa.data.local;

import androidx.room.TypeConverter;
import java.util.Date;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;
import com.example.inmobiliariaventa.domain.entity.Disposicion;

public class Converters {
    @TypeConverter
    public String fromTipoPropiedad(TipoPropiedad tipo) {
        return tipo == null ? null : tipo.name();
    }

    @TypeConverter
    public TipoPropiedad toTipoPropiedad(String nombre) {
        return nombre == null ? null : TipoPropiedad.valueOf(nombre);
    }

    @TypeConverter
    public String fromDisposicion(Disposicion disposicion) {
        return disposicion == null ? null : disposicion.name();
    }

    @TypeConverter
    public Disposicion toDisposicion(String nombre) {
        return nombre == null ? null : Disposicion.valueOf(nombre);
    }
    
    @TypeConverter
    public Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    @TypeConverter
    public Date timestampToDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public String fromStringList(java.util.List<String> list) {
        if (list == null) return null;
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<String>>() {}.getType();
        return gson.toJson(list, type);
    }

    @TypeConverter
    public java.util.List<String> toStringList(String data) {
        if (data == null) return null;
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<String>>() {}.getType();
        return gson.fromJson(data, type);
    }
}
