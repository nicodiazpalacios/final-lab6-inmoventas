package com.example.inmobiliariaventa.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.data.local.dao.UsuarioDao;
import com.example.inmobiliariaventa.data.local.dao.PropiedadDao;
import com.example.inmobiliariaventa.data.local.dao.FavoritoDao;
import com.example.inmobiliariaventa.data.local.dao.NotificacionDao;
import com.example.inmobiliariaventa.data.local.dao.ContratoDao;

@Database(entities = {Usuario.class, Propiedad.class, Favorito.class, Notificacion.class, Contrato.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();
    public abstract PropiedadDao propiedadDao();
    public abstract FavoritoDao favoritoDao();
    public abstract NotificacionDao notificacionDao();
    public abstract ContratoDao contratoDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final java.util.concurrent.ExecutorService databaseWriteExecutor =
            java.util.concurrent.Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "homefinder_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
