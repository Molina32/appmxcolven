package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialUsuarioFragment extends Fragment {
    private static final String PREFS_SESSION = "vigia_session";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";

    private ApiService apiService;
    private TextView tvHistorialContenido;

    public HistorialUsuarioFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_usuario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class);
        tvHistorialContenido = view.findViewById(R.id.tvHistorialContenidoUsuario);
        cargarHistorialUsuario();
    }

    private void cargarHistorialUsuario() {
        if (apiService == null || tvHistorialContenido == null || !isAdded()) {
            return;
        }
        tvHistorialContenido.setText("Cargando historial...");
        String username = obtenerUsernameSesion();
        Long usuarioId = obtenerUsuarioIdSesion();

        apiService.getHistorialAdmin().enqueue(new Callback<List<LinkedHashMap<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<LinkedHashMap<String, Object>>> call,
                                   @NonNull Response<List<LinkedHashMap<String, Object>>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    tvHistorialContenido.setText("Error al cargar historial. HTTP " + response.code());
                    return;
                }
                List<LinkedHashMap<String, Object>> historial = response.body();
                if (historial.isEmpty()) {
                    tvHistorialContenido.setText("Sin movimientos.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                int contador = 0;
                for (LinkedHashMap<String, Object> item : historial) {
                    if (!perteneceAlUsuario(item, usuarioId, username)) {
                        continue;
                    }
                    String usuario = obtenerValor(item, "usuario", "username", "user", "usuario_nombre", "usuarionombre");
                    String nombre = obtenerNombreCompleto(item);
                    String accion = formatearAccion(obtenerValor(item, "accion", "acción", "action", "que_hizo", "quehizo"));
                    String articulo = obtenerValor(item, "articulo", "artículo", "referencia", "producto", "descripcion", "nombre_articulo", "nombrearticulo");
                    String cantidad = obtenerValor(item, "cantidad", "cambio", "qty", "cantidad_movimiento", "cantidadmovimiento");
                    String fecha = obtenerValor(item, "fecha", "fecha_evento", "fechaevento", "created_at", "createdat", "fecha_movimiento", "fechamovimiento");

                    sb.append("Usuario: ").append(usuario.isEmpty() ? "-" : usuario).append("\n")
                            .append("Nombre: ").append(nombre.isEmpty() ? "-" : nombre).append("\n")
                            .append("Que hizo: ").append(accion.isEmpty() ? "-" : accion).append("\n")
                            .append("Articulo: ").append(articulo.isEmpty() ? "-" : articulo).append("\n")
                            .append("Cantidad: ").append(cantidad.isEmpty() ? "-" : cantidad).append("\n")
                            .append("Fecha: ").append(fecha.isEmpty() ? "-" : fecha).append("\n\n");
                    contador++;
                }
                if (contador == 0) {
                    tvHistorialContenido.setText("Sin movimientos.");
                    return;
                }
                tvHistorialContenido.setText(sb.toString().trim());
            }

            @Override
            public void onFailure(@NonNull Call<List<LinkedHashMap<String, Object>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                tvHistorialContenido.setText("Error al cargar historial: " + (t.getMessage() != null ? t.getMessage() : "Error"));
            }
        });
    }

    @Nullable
    private Long obtenerUsuarioIdSesion() {
        if (getContext() == null) {
            return null;
        }
        SharedPreferences preferences = getContext().getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE);
        long id = preferences.getLong(KEY_USER_ID, -1L);
        return id > 0 ? id : null;
    }

    @NonNull
    private String obtenerUsernameSesion() {
        if (getContext() == null) {
            return "";
        }
        SharedPreferences preferences = getContext().getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE);
        String username = preferences.getString(KEY_USERNAME, "");
        return username != null ? username.trim() : "";
    }

    private boolean perteneceAlUsuario(@Nullable Map<String, Object> item, @Nullable Long usuarioId, @Nullable String username) {
        if (item == null) {
            return false;
        }
        if (usuarioId != null) {
            String idValor = obtenerValor(item, "usuario_id", "usuarioid", "user_id", "userid");
            if (!idValor.isEmpty()) {
                try {
                    long id = Long.parseLong(idValor.contains(".") ? idValor.substring(0, idValor.indexOf('.')) : idValor);
                    return id == usuarioId;
                } catch (Exception ignored) {
                }
            }
        }
        if (username != null && !username.trim().isEmpty()) {
            String usuario = obtenerValor(item, "usuario", "username", "user", "usuario_nombre", "usuarionombre");
            return usuario.equalsIgnoreCase(username.trim());
        }
        return false;
    }

    @NonNull
    private String obtenerNombreCompleto(@Nullable Map<String, Object> item) {
        String nombre = obtenerValor(item, "nombre", "name");
        String apPat = obtenerValor(item, "apellido_paterno", "apellidopaterno");
        String apMat = obtenerValor(item, "apellido_materno", "apellidomaterno");
        StringBuilder sb = new StringBuilder();
        if (!nombre.isEmpty()) sb.append(nombre);
        if (!apPat.isEmpty()) sb.append(sb.length() == 0 ? "" : " ").append(apPat);
        if (!apMat.isEmpty()) sb.append(sb.length() == 0 ? "" : " ").append(apMat);
        return sb.toString().trim();
    }

    @NonNull
    private String formatearAccion(@Nullable String accion) {
        if (accion == null) {
            return "";
        }
        String normalizada = accion.trim();
        if (normalizada.isEmpty()) {
            return "";
        }
        String key = normalizada.toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
        if (key.equals("AUMENTAR_STOCK") || key.equals("AUMENTARSTOCK")) {
            return "Aumento stock";
        }
        if (key.equals("REDUCIR_STOCK") || key.equals("REDUCIRSTOCK")) {
            return "Reducción stock";
        }
        String[] parts = key.split("_+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            String w = p.trim().toLowerCase(Locale.ROOT);
            if (w.equals("stock")) {
                sb.append(sb.length() == 0 ? "" : " ").append("stock");
                continue;
            }
            sb.append(sb.length() == 0 ? "" : " ")
                    .append(Character.toUpperCase(w.charAt(0)))
                    .append(w.length() > 1 ? w.substring(1) : "");
        }
        return sb.toString().trim();
    }

    @NonNull
    private String obtenerValor(@Nullable Map<String, Object> item, @NonNull String... claves) {
        if (item == null || claves.length == 0) {
            return "";
        }
        for (String clave : claves) {
            if (clave == null) continue;
            String buscada = normalizarClave(clave);
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                if (normalizarClave(entry.getKey()).equals(buscada)) {
                    String valor = String.valueOf(entry.getValue()).trim();
                    if (!valor.isEmpty()) {
                        return valor;
                    }
                }
            }
        }
        return "";
    }

    @NonNull
    private String normalizarClave(@NonNull String clave) {
        return clave.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
    }
}
