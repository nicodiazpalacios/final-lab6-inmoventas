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
}
