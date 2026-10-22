package com.example.inmobiliariaventa.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
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

    public static String formatIsoUtc(long millis) {
        if (millis <= 0) millis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    public static long parseTimestampFlexible(JsonElement element) {
        if (element == null || element.isJsonNull()) return System.currentTimeMillis();
        try {
            if (element.isJsonPrimitive()) {
                JsonPrimitive prim = element.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    return prim.getAsLong();
                }
                String val = prim.getAsString().trim();
                if (val.isEmpty()) return System.currentTimeMillis();
                try {
                    return Long.parseLong(val);
                } catch (NumberFormatException ignored) {}

                // Truncar microsegundos si existen (.569428 -> .569) para SimpleDateFormat
                if (val.contains(".")) {
                    int dotIdx = val.indexOf('.');
                    int tzIdx = val.indexOf('+', dotIdx);
                    if (tzIdx == -1) tzIdx = val.indexOf('-', dotIdx);
                    if (tzIdx == -1) tzIdx = val.indexOf('Z', dotIdx);
                    if (tzIdx != -1) {
                        String millisPart = val.substring(dotIdx + 1, tzIdx);
                        if (millisPart.length() > 3) {
                            val = val.substring(0, dotIdx + 1) + millisPart.substring(0, 3) + val.substring(tzIdx);
                        }
                    } else {
                        String millisPart = val.substring(dotIdx + 1);
                        if (millisPart.length() > 3) {
                            val = val.substring(0, dotIdx + 1) + millisPart.substring(0, 3);
                        }
                    }
                }

                String[] formats = new String[] {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
                };

                for (String format : formats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date d = sdf.parse(val);
                        if (d != null) return d.getTime();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return System.currentTimeMillis();
    }

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
            .registerTypeAdapter(Notificacion.class, new JsonDeserializer<Notificacion>() {
                @Override
                public Notificacion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    JsonObject obj = json.getAsJsonObject();
                    Notificacion n = new Notificacion();
                    if (obj.has("id") && !obj.get("id").isJsonNull()) {
                        n.id = obj.get("id").getAsString();
                    }
                    if (obj.has("usuario_id") && !obj.get("usuario_id").isJsonNull()) {
                        n.usuario_id = obj.get("usuario_id").getAsString();
                    }
                    if (obj.has("titulo") && !obj.get("titulo").isJsonNull()) {
                        n.titulo = obj.get("titulo").getAsString();
                    }
                    if (obj.has("mensaje") && !obj.get("mensaje").isJsonNull()) {
                        n.mensaje = obj.get("mensaje").getAsString();
                    }
                    if (obj.has("fecha") && !obj.get("fecha").isJsonNull()) {
                        n.fecha = parseTimestampFlexible(obj.get("fecha"));
                    }
                    if (obj.has("isleida") && !obj.get("isleida").isJsonNull()) {
                        n.isLeida = obj.get("isleida").getAsBoolean();
                    }
                    if (obj.has("categoria") && !obj.get("categoria").isJsonNull()) {
                        n.categoria = obj.get("categoria").getAsString();
                    }
                    if (obj.has("referencia_id") && !obj.get("referencia_id").isJsonNull()) {
                        n.referencia_id = obj.get("referencia_id").getAsString();
                    }
                    return n;
                }
            })
            .registerTypeAdapter(Favorito.class, new JsonDeserializer<Favorito>() {
                @Override
                public Favorito deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    JsonObject obj = json.getAsJsonObject();
                    Favorito fav = new Favorito();
                    if (obj.has("usuarioid") && !obj.get("usuarioid").isJsonNull()) {
                        fav.usuarioId = obj.get("usuarioid").getAsString();
                    } else if (obj.has("usuarioId") && !obj.get("usuarioId").isJsonNull()) {
                        fav.usuarioId = obj.get("usuarioId").getAsString();
                    }
                    if (obj.has("propiedadid") && !obj.get("propiedadid").isJsonNull()) {
                        fav.propiedadId = obj.get("propiedadid").getAsString();
                    } else if (obj.has("propiedadId") && !obj.get("propiedadId").isJsonNull()) {
                        fav.propiedadId = obj.get("propiedadId").getAsString();
                    }
                    if (obj.has("fechaguardado") && !obj.get("fechaguardado").isJsonNull()) {
                        fav.fechaGuardado = parseTimestampFlexible(obj.get("fechaguardado"));
                    } else if (obj.has("fechaGuardado") && !obj.get("fechaGuardado").isJsonNull()) {
                        fav.fechaGuardado = parseTimestampFlexible(obj.get("fechaGuardado"));
                    }
                    return fav;
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
        JsonObject json = new JsonObject();
        if (usuario.id != null) json.addProperty("id", usuario.id);
        if (usuario.nombre != null) json.addProperty("nombre", usuario.nombre);
        if (usuario.email != null) json.addProperty("email", usuario.email);
        if (usuario.telefono != null) json.addProperty("telefono", usuario.telefono);
        if (usuario.rol != null) json.addProperty("rol", usuario.rol);
        if (usuario.fotoPerfil != null && !usuario.fotoPerfil.isEmpty()) {
            json.addProperty("fotoperfil", usuario.fotoPerfil);
        }
        if (usuario.password != null && !usuario.password.isEmpty()) {
            json.addProperty("password", usuario.password);
            json.addProperty("tokensesion", usuario.password);
        }

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

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

    public static void actualizarFotoPerfil(String userId, String fotoBase64, SupabaseCallback<Boolean> callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("User ID inválido")));
            return;
        }

        JsonObject json = new JsonObject();
        if (fotoBase64 != null && !fotoBase64.isEmpty()) {
            json.addProperty("fotoperfil", fotoBase64);
        } else {
            json.add("fotoperfil", com.google.gson.JsonNull.INSTANCE);
        }

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/usuarios?id=eq." + userId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al actualizar foto de perfil en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Foto de perfil actualizada en Supabase para: " + userId);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.w(TAG, "Aviso Supabase al actualizar foto de perfil: " + response.code() + " - " + errorBody);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(errorBody)));
                }
            }
        });
    }

    public static void actualizarDatosUsuario(String userId, String nombre, String email, String telefono, SupabaseCallback<Boolean> callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("User ID inválido")));
            return;
        }

        JsonObject json = new JsonObject();
        if (nombre != null) json.addProperty("nombre", nombre);
        if (email != null) json.addProperty("email", email);
        if (telefono != null) json.addProperty("telefono", telefono);

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/usuarios?id=eq." + userId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al actualizar datos de usuario en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Datos de usuario actualizados en Supabase para: " + userId);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.w(TAG, "Aviso Supabase al actualizar datos de usuario: " + response.code() + " - " + errorBody);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(errorBody)));
                }
            }
        });
    }

    public static void actualizarPassword(String userId, String passwordHash, SupabaseCallback<Boolean> callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("User ID inválido")));
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("password", passwordHash);
        json.addProperty("tokensesion", passwordHash);

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/usuarios?id=eq." + userId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al actualizar contraseña en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Contraseña actualizada exitosamente en Supabase para: " + userId);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error al actualizar contraseña en Supabase: " + msg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void actualizarRolUsuario(String userId, String rol, SupabaseCallback<Boolean> callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("User ID inválido")));
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("rol", rol);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/usuarios?id=eq." + userId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al actualizar rol de usuario en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code() + ": " + errorBody)));
                }
            }
        });
    }

    public static void eliminarUsuario(String userId, SupabaseCallback<Boolean> callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("User ID inválido")));
            return;
        }

        Request request = buildBaseRequest("/rest/v1/usuarios?id=eq." + userId)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al eliminar usuario en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code())));
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

    public static void actualizarConfiguracionEmpresa(ConfiguracionEmpresa config, SupabaseCallback<Boolean> callback) {
        if (config == null) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("Config inválido")));
            return;
        }

        JsonObject json = new JsonObject();
        if (config.nombreEmpresa != null) json.addProperty("nombreempresa", config.nombreEmpresa);
        if (config.telefonoWhatsapp != null) json.addProperty("telefonowhatsapp", config.telefonoWhatsapp);
        if (config.email != null) json.addProperty("email", config.email);
        if (config.direccion != null) json.addProperty("direccion", config.direccion);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String isoDate = sdf.format(new java.util.Date());
        json.addProperty("updated_at", isoDate);
        config.updatedAt = isoDate;

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/configuracion_empresa?id=eq.empresa_principal")
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al actualizar configuracion de empresa: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Configuración de empresa actualizada en Supabase");
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.w(TAG, "Aviso Supabase al actualizar configuracion empresa: " + response.code() + " - " + errorBody);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(errorBody)));
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

        JsonObject json = new JsonObject();
        json.addProperty("usuarioid", fav.usuarioId);
        json.addProperty("propiedadid", fav.propiedadId);
        json.addProperty("fechaguardado", formatIsoUtc(fav.fechaGuardado));
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

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

    public static void marcarMensajeComoNoLeido(String remoteId, SupabaseCallback<Boolean> callback) {
        if (remoteId == null || remoteId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("isleido", false);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/mensajes?id=eq." + remoteId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al marcar mensaje como no leído en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Mensaje marcado como no leído en Supabase: " + remoteId);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String msg = "HTTP " + response.code();
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    // ==========================================
    // CONTRATOS
    // ==========================================

    public static void obtenerContratos(SupabaseCallback<List<Contrato>> callback) {
        Request request = buildBaseRequest("/rest/v1/contratos?select=*&order=created_at.desc").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al obtener contratos desde Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP Error " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    Log.e(TAG, "Error al obtener contratos de Supabase: " + errorMsg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(errorMsg)));
                    return;
                }
                try {
                    String body = response.body() != null ? response.body().string() : "[]";
                    Type listType = new TypeToken<List<Contrato>>() {}.getType();
                    List<Contrato> lista = gson.fromJson(body, listType);
                    if (lista != null) {
                        for (Contrato c : lista) {
                            c.normalizarDesdeSupabase();
                        }
                    }
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(lista));
                } catch (Exception e) {
                    Log.e(TAG, "Error al deserializar contratos de Supabase: " + e.getMessage(), e);
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    private static JsonObject buildContratoPayload(Contrato contrato, boolean includePropiedadId) {
        JsonObject json = new JsonObject();
        json.addProperty("id", contrato.id != null && !contrato.id.isEmpty() ? contrato.id : UUID.randomUUID().toString());

        boolean propIdPareceValido = includePropiedadId
                && contrato.propiedadId != null
                && !contrato.propiedadId.trim().isEmpty()
                && (contrato.propiedadTitulo == null || !contrato.propiedadId.trim().equalsIgnoreCase(contrato.propiedadTitulo.trim()));

        if (propIdPareceValido) {
            json.addProperty("propiedadid", contrato.propiedadId.trim());
        } else {
            json.add("propiedadid", com.google.gson.JsonNull.INSTANCE);
        }

        json.addProperty("propiedadtitulo", contrato.propiedadTitulo != null ? contrato.propiedadTitulo : "");
        json.addProperty("clientenombre", contrato.clienteNombre != null ? contrato.clienteNombre : "");
        json.addProperty("clienteapellido", contrato.clienteApellido != null ? contrato.clienteApellido : "");
        json.addProperty("clientedni", contrato.clienteDni != null ? contrato.clienteDni : "");
        json.addProperty("clientetelefono", contrato.clienteTelefono != null ? contrato.clienteTelefono : "");
        json.addProperty("clienteemail", contrato.clienteEmail != null ? contrato.clienteEmail : "");
        json.addProperty("montototal", contrato.montoTotal != null ? contrato.montoTotal : 0.0);
        json.addProperty("seniamonto", contrato.seniaMonto != null ? contrato.seniaMonto : 0.0);
        json.addProperty("metodopago", contrato.metodoPago != null ? contrato.metodoPago : "Efectivo");
        json.addProperty("cuotas", contrato.cuotas != null ? contrato.cuotas : 1);
        json.addProperty("fechainicio", contrato.fechaInicio != null ? contrato.fechaInicio : "");
        json.addProperty("fechaventa", contrato.fechaVencimiento != null && !contrato.fechaVencimiento.isEmpty()
                ? contrato.fechaVencimiento
                : (contrato.fechaInicio != null ? contrato.fechaInicio : ""));
        json.addProperty("estado", contrato.estado != null ? contrato.estado : "Activo");
        json.addProperty("descripcion", contrato.descripcion != null ? contrato.descripcion : "");

        return json;
    }

    public static void guardarContrato(Contrato contrato, SupabaseCallback<Boolean> callback) {
        if (contrato == null) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("Contrato nulo")));
            return;
        }
        enviarContratoInterno(contrato, true, callback);
    }

    private static void enviarContratoInterno(Contrato contrato, boolean includePropiedadId, SupabaseCallback<Boolean> callback) {
        JsonObject json = buildContratoPayload(contrato, includePropiedadId);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        // Usamos resolution=merge-duplicates para que sirva tanto al crear como al modificar un contrato
        Request request = buildBaseRequest("/rest/v1/contratos")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al guardar contrato en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Contrato guardado/actualizado en Supabase exitosamente: " + contrato.id);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    if (includePropiedadId && errorBody.contains("23503")) {
                        // Si la propiedad era solo local y no existe en la tabla remota propiedades, reintentar con propiedadid = null
                        enviarContratoInterno(contrato, false, callback);
                        return;
                    }
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error al guardar contrato en Supabase: " + msg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void actualizarEstadoContrato(String contratoId, String nuevoEstado, SupabaseCallback<Boolean> callback) {
        if (contratoId == null || contratoId.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("ID inválido")));
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("estado", nuevoEstado);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/contratos?id=eq." + contratoId)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al actualizar estado de contrato en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Estado de contrato actualizado en Supabase: " + contratoId + " -> " + nuevoEstado);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception("HTTP " + response.code())));
                }
            }
        });
    }

    // ==========================================
    // NOTIFICACIONES
    // ==========================================

    public static void obtenerNotificaciones(SupabaseCallback<List<Notificacion>> callback) {
        Request request = buildBaseRequest("/rest/v1/notificaciones?select=*&order=fecha.desc").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al obtener notificaciones desde Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP Error " + response.code() + ": " + (response.body() != null ? response.body().string() : "");
                    Log.e(TAG, "Error al obtener notificaciones de Supabase: " + errorMsg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(errorMsg)));
                    return;
                }
                try {
                    String body = response.body() != null ? response.body().string() : "[]";
                    Type listType = new TypeToken<List<Notificacion>>() {}.getType();
                    List<Notificacion> lista = gson.fromJson(body, listType);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(lista));
                } catch (Exception e) {
                    Log.e(TAG, "Error al deserializar notificaciones de Supabase: " + e.getMessage(), e);
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    public static void guardarNotificacion(Notificacion notif, SupabaseCallback<Boolean> callback) {
        if (notif == null) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("Notificación nula")));
            return;
        }
        enviarNotificacionInterno(notif, true, callback);
    }

    private static void enviarNotificacionInterno(Notificacion notif, boolean includeUsuarioId, SupabaseCallback<Boolean> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("id", notif.id != null && !notif.id.isEmpty() ? notif.id : UUID.randomUUID().toString());
        if (includeUsuarioId && notif.usuario_id != null && !notif.usuario_id.trim().isEmpty()
                && !"ADMIN".equalsIgnoreCase(notif.usuario_id) && !"CLIENTE".equalsIgnoreCase(notif.usuario_id)) {
            json.addProperty("usuario_id", notif.usuario_id.trim());
        } else {
            json.add("usuario_id", com.google.gson.JsonNull.INSTANCE);
        }
        json.addProperty("titulo", notif.titulo != null ? notif.titulo : "Notificación");
        json.addProperty("mensaje", notif.mensaje != null ? notif.mensaje : "");
        json.addProperty("fecha", formatIsoUtc(notif.fecha > 0 ? notif.fecha : System.currentTimeMillis()));
        json.addProperty("isleida", notif.isLeida);
        json.addProperty("categoria", notif.categoria != null ? notif.categoria : "GENERAL");
        if (notif.referencia_id != null && !notif.referencia_id.isEmpty()) {
            json.addProperty("referencia_id", notif.referencia_id);
        } else {
            json.add("referencia_id", com.google.gson.JsonNull.INSTANCE);
        }

        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/notificaciones")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error de red al guardar notificación en Supabase: " + e.getMessage());
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notificación guardada en Supabase exitosamente: " + notif.id);
                    if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    if (includeUsuarioId && errorBody.contains("23503")) {
                        enviarNotificacionInterno(notif, false, callback);
                        return;
                    }
                    String msg = "HTTP " + response.code() + ": " + errorBody;
                    Log.e(TAG, "Error al guardar notificación en Supabase: " + msg);
                    if (callback != null) mainHandler.post(() -> callback.onError(new Exception(msg)));
                }
            }
        });
    }

    public static void marcarNotificacionLeida(String id, SupabaseCallback<Boolean> callback) {
        if (id == null || id.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("isleida", true);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/notificaciones?id=eq." + id)
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (callback != null) mainHandler.post(() -> callback.onSuccess(response.isSuccessful()));
            }
        });
    }

    public static void marcarTodasNotificacionesLeidas(SupabaseCallback<Boolean> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("isleida", true);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        Request request = buildBaseRequest("/rest/v1/notificaciones?isleida=eq.false")
                .header("Prefer", "return=minimal")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (callback != null) mainHandler.post(() -> callback.onSuccess(response.isSuccessful()));
            }
        });
    }

    public static void eliminarNotificacion(String id, SupabaseCallback<Boolean> callback) {
        if (id == null || id.isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onSuccess(true));
            return;
        }
        Request request = buildBaseRequest("/rest/v1/notificaciones?id=eq." + id)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (callback != null) mainHandler.post(() -> callback.onSuccess(response.isSuccessful()));
            }
        });
    }
}
