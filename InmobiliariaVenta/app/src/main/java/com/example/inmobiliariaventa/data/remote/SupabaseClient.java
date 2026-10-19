package com.example.inmobiliariaventa.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.UUID;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    public static final String SUPABASE_URL = "https://baszmfdfkgvbuaokbwak.supabase.co";
    public static final String SUPABASE_KEY = "sb_publishable_415STRITfGaS-AV9l_Bs9Q_hYBS4T4Y";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getName().equals("estadoSincronizacion");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SupabaseCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    private static Request.Builder buildBaseRequest(String endpoint) {
        return new Request.Builder()
                .url(SUPABASE_URL + endpoint)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json");
    }

    // ==========================================
    // PROPIEDADES
    // ==========================================

    public static void obtenerPropiedades(SupabaseCallback<List<Propiedad>> callback) {
        Request request = buildBaseRequest("/rest/v1/propiedades?select=*&order=id.desc").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error al obtener propiedades", e);
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP Error " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    mainHandler.post(() -> callback.onError(new Exception(errorMsg)));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<List<Propiedad>>() {}.getType();
                List<Propiedad> lista = gson.fromJson(body, listType);
                mainHandler.post(() -> callback.onSuccess(lista));
            }
        });
    }

    public static void insertarPropiedad(Propiedad propiedad, SupabaseCallback<Boolean> callback) {
        String json = gson.toJson(propiedad);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request request = buildBaseRequest("/rest/v1/propiedades")
                .header("Prefer", "return=minimal")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String msg = "HTTP " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void actualizarPropiedad(Propiedad propiedad, SupabaseCallback<Boolean> callback) {
        String json = gson.toJson(propiedad);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request request = buildBaseRequest("/rest/v1/propiedades?id=eq." + propiedad.id)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String msg = "HTTP " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void eliminarPropiedad(String id, SupabaseCallback<Boolean> callback) {
        Request request = buildBaseRequest("/rest/v1/propiedades?id=eq." + id)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code())));
                }
            }
        });
    }

    // ==========================================
    // USUARIOS
    // ==========================================

    public static void obtenerUsuarios(SupabaseCallback<List<Usuario>> callback) {
        Request request = buildBaseRequest("/rest/v1/usuarios?select=*").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code())));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<List<Usuario>>() {}.getType();
                List<Usuario> lista = gson.fromJson(body, listType);
                mainHandler.post(() -> callback.onSuccess(lista));
            }
        });
    }

    public static void obtenerUsuarioPorEmail(String email, SupabaseCallback<Usuario> callback) {
        Request request = buildBaseRequest("/rest/v1/usuarios?email=eq." + email + "&select=*&limit=1").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code())));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<List<Usuario>>() {}.getType();
                List<Usuario> lista = gson.fromJson(body, listType);
                if (lista != null && !lista.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(lista.get(0)));
                } else {
                    mainHandler.post(() -> callback.onSuccess(null));
                }
            }
        });
    }

    public static void registrarUsuario(Usuario usuario, SupabaseCallback<Boolean> callback) {
        String json = gson.toJson(usuario);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request request = buildBaseRequest("/rest/v1/usuarios")
                .header("Prefer", "return=minimal")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al registrar usuario en Supabase: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Usuario registrado exitosamente en Supabase: " + usuario.email);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error registrando usuario en Supabase: " + msg);
                    mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    // ==========================================
    // CONFIGURACIÓN DE EMPRESA
    // ==========================================

    public static void obtenerConfiguracionEmpresa(SupabaseCallback<ConfiguracionEmpresa> callback) {
        Request request = buildBaseRequest("/rest/v1/configuracion_empresa?select=*&limit=1").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code())));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<List<ConfiguracionEmpresa>>() {}.getType();
                List<ConfiguracionEmpresa> lista = gson.fromJson(body, listType);
                if (lista != null && !lista.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(lista.get(0)));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ConfiguracionEmpresa()));
                }
            }
        });
    }

    // ==========================================
    // FAVORITOS
    // ==========================================

    public static void obtenerFavoritosPorUsuario(String usuarioId, SupabaseCallback<List<Favorito>> callback) {
        Request request = buildBaseRequest("/rest/v1/favoritos?usuarioid=eq." + usuarioId + "&select=*").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al obtener favoritos en Supabase", e);
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP Error " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    Log.e(TAG, "Error al obtener favoritos: " + errorMsg);
                    mainHandler.post(() -> callback.onError(new Exception(errorMsg)));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<List<Favorito>>() {}.getType();
                List<Favorito> lista = gson.fromJson(body, listType);
                mainHandler.post(() -> callback.onSuccess(lista));
            }
        });
    }

    public static void agregarFavorito(Favorito fav, SupabaseCallback<Boolean> callback) {
        if (fav == null || fav.usuarioId == null || fav.propiedadId == null) {
            mainHandler.post(() -> callback.onError(new IllegalArgumentException("Favorito o IDs inválidos")));
            return;
        }
        if (fav.fechaGuardado <= 0) {
            fav.fechaGuardado = System.currentTimeMillis();
        }

        String json = gson.toJson(fav);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request request = buildBaseRequest("/rest/v1/favoritos")
                .header("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al agregar favorito en Supabase: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Favorito guardado en Supabase con éxito: " + fav.usuarioId + " -> " + fav.propiedadId);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error al agregar favorito en Supabase: " + msg);
                    mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void agregarFavorito(String usuarioId, String propiedadId, SupabaseCallback<Boolean> callback) {
        Favorito fav = new Favorito();
        fav.usuarioId = usuarioId;
        fav.propiedadId = propiedadId;
        fav.fechaGuardado = System.currentTimeMillis();
        agregarFavorito(fav, callback);
    }

    public static void eliminarFavorito(String usuarioId, String propiedadId, SupabaseCallback<Boolean> callback) {
        Request request = buildBaseRequest("/rest/v1/favoritos?usuarioid=eq." + usuarioId + "&propiedadid=eq." + propiedadId)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al eliminar favorito en Supabase: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Favorito eliminado en Supabase con éxito: " + usuarioId + " -> " + propiedadId);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error al eliminar favorito en Supabase: " + msg);
                    mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    // ==========================================
    // MENSAJES DE CONTACTO
    // ==========================================

    public static void enviarMensajeContacto(MensajeContacto mensaje, SupabaseCallback<Boolean> callback) {
        if (mensaje == null) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("Mensaje nulo")));
            return;
        }

        if (mensaje.remoteId == null || mensaje.remoteId.trim().isEmpty()) {
            mensaje.remoteId = UUID.randomUUID().toString();
        }

        JsonObject json = new JsonObject();
        json.addProperty("id", mensaje.remoteId);
        json.addProperty("clientenombre", mensaje.clienteNombre != null ? mensaje.clienteNombre : "");
        json.addProperty("clientetelefono", mensaje.clienteTelefono != null ? mensaje.clienteTelefono : "");
        json.addProperty("clienteemail", mensaje.clienteEmail != null ? mensaje.clienteEmail : "");
        if (mensaje.propiedadId != null && !mensaje.propiedadId.isEmpty()) {
            json.addProperty("propiedadid", mensaje.propiedadId);
        } else {
            json.add("propiedadid", com.google.gson.JsonNull.INSTANCE);
        }
        json.addProperty("propiedadtitulo", mensaje.propiedadTitulo != null ? mensaje.propiedadTitulo : "");
        json.addProperty("mensaje", mensaje.mensaje != null ? mensaje.mensaje : "");
        json.addProperty("fecha", mensaje.fecha != null ? mensaje.fecha : "");
        json.addProperty("hora", mensaje.hora != null ? mensaje.hora : "");
        json.addProperty("isleido", mensaje.leido);

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/mensajes")
                .header("Prefer", "return=minimal")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al enviar mensaje a Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Mensaje enviado exitosamente a Supabase: " + mensaje.remoteId);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error al enviar mensaje a Supabase: " + msg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void obtenerMensajes(SupabaseCallback<List<MensajeContacto>> callback) {
        Request request = buildBaseRequest("/rest/v1/mensajes?select=*&order=created_at.desc").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al obtener mensajes desde Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP Error " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    Log.e(TAG, "Error al obtener mensajes de Supabase: " + errorMsg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(errorMsg)));
                    return;
                }
                try {
                    String body = response.body() != null ? response.body().string() : "[]";
                    Type listType = new TypeToken<List<MensajeContacto>>() {}.getType();
                    List<MensajeContacto> lista = gson.fromJson(body, listType);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(lista));
                } catch (Exception e) {
                    Log.e(TAG, "Error al deserializar mensajes de Supabase: " + e.getMessage(), e);
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    public static void marcarMensajeComoLeido(String remoteId, SupabaseCallback<Boolean> callback) {
        if (remoteId == null || remoteId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("isleido", true);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/mensajes?id=eq." + remoteId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al marcar mensaje leído en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Mensaje marcado como leído en Supabase: " + remoteId);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String msg = "HTTP " + response.code();
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }
}
