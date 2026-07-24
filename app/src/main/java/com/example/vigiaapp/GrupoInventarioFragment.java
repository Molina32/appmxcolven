package com.example.vigiaapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GrupoInventarioFragment extends Fragment {
    private static final String ARG_NOMBRE_GRUPO = "nombre_grupo";
    private static final String ARG_NOMBRE_TABLA = "nombre_tabla";
    private static final String ARG_COLUMNAS = "columnas";

    private String nombreGrupo;
    private String nombreTabla;
    private ArrayList<String> columnas;
    private TextView tvGrupoSeleccionado;
    private TextView tvContenidoGrupo;
    private LinearLayout registrosContainer;
    private ApiService apiService;
    private final ArrayList<LinkedHashMap<String, Object>> registrosGuardados = new ArrayList<>();

    public GrupoInventarioFragment() {
    }

    public static GrupoInventarioFragment newInstance(String nombreGrupo, String nombreTabla, ArrayList<String> columnas) {
        GrupoInventarioFragment fragment = new GrupoInventarioFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOMBRE_GRUPO, nombreGrupo);
        args.putString(ARG_NOMBRE_TABLA, nombreTabla);
        args.putStringArrayList(ARG_COLUMNAS, columnas);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreGrupo = getArguments().getString(ARG_NOMBRE_GRUPO);
            nombreTabla = getArguments().getString(ARG_NOMBRE_TABLA);
            columnas = getArguments().getStringArrayList(ARG_COLUMNAS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grupo_inventario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvGrupoSeleccionado = view.findViewById(R.id.tvGrupoSeleccionado);
        tvContenidoGrupo = view.findViewById(R.id.tvContenidoGrupo);
        registrosContainer = view.findViewById(R.id.registrosContainer);
        apiService = ApiClient.getClient().create(ApiService.class);

        tvGrupoSeleccionado.setText(getString(R.string.grupo_seleccionado_titulo, valorSeguro(nombreGrupo)));
        tvContenidoGrupo.setText(R.string.registros_grupo_vacio);
        cargarRegistrosDesdeApi();
    }

    private void cargarRegistrosDesdeApi() {
        if (apiService == null) {
            return;
        }

        tvContenidoGrupo.setVisibility(View.GONE);
        apiService.getRegistrosGrupo(obtenerIdentificadorGrupo()).enqueue(new Callback<List<LinkedHashMap<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<LinkedHashMap<String, Object>>> call,
                                   @NonNull Response<List<LinkedHashMap<String, Object>>> response) {
                if (!isAdded()) {
                    return;
                }

                registrosGuardados.clear();
                if (response.isSuccessful() && response.body() != null) {
                    registrosGuardados.addAll(response.body());
                    renderizarRegistrosGuardados();
                } else {
                    tvContenidoGrupo.setVisibility(View.VISIBLE);
                    tvContenidoGrupo.setText(R.string.registros_grupo_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<LinkedHashMap<String, Object>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                tvContenidoGrupo.setVisibility(View.VISIBLE);
                tvContenidoGrupo.setText(R.string.registros_grupo_error);
                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderizarRegistrosGuardados() {
        registrosContainer.removeAllViews();

        if (registrosGuardados.isEmpty()) {
            tvContenidoGrupo.setVisibility(View.VISIBLE);
            tvContenidoGrupo.setText(R.string.registros_grupo_vacio);
            return;
        }

        tvContenidoGrupo.setVisibility(View.GONE);
        for (Map<String, Object> registro : registrosGuardados) {
            agregarRegistroVisual(registro);
        }
    }

    private void agregarRegistroVisual(Map<String, Object> valores) {
        LinearLayout bloque = new LinearLayout(requireContext());
        bloque.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bloqueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bloqueParams.topMargin = dpToPx(8);
        bloque.setLayoutParams(bloqueParams);

        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dpToPx(8), 0, dpToPx(8));

        Bitmap bitmapRegistro = obtenerImagenRegistro(valores);

        ImageView imagen = new ImageView(requireContext());
        LinearLayout.LayoutParams imagenParams = new LinearLayout.LayoutParams(
                dpToPx(84),
                dpToPx(84)
        );
        imagenParams.rightMargin = dpToPx(12);
        imagen.setLayoutParams(imagenParams);
        imagen.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagen.setBackgroundColor(0xFFEAEAEA);
        if (bitmapRegistro != null) {
            imagen.setImageBitmap(bitmapRegistro);
        } else {
            imagen.setImageResource(android.R.drawable.ic_menu_gallery);
            imagen.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        }
        item.addView(imagen);

        LinearLayout detalles = new LinearLayout(requireContext());
        detalles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detallesParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detallesParams.weight = 1f;
        detalles.setLayoutParams(detallesParams);

        for (Map.Entry<String, Object> entry : valores.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            String claveNormalizada = entry.getKey().trim().toLowerCase();
            if (claveNormalizada.equals("id")
                    || claveNormalizada.equals("idregistro")
                    || claveNormalizada.equals("id_registro")) {
                continue;
            }

            if (esColumnaDeFoto(entry.getKey())) {
                continue;
            }

            TextView linea = new TextView(requireContext());
            linea.setText(getString(
                    R.string.registro_campo_valor,
                    formatearNombreColumna(entry.getKey()),
                    String.valueOf(entry.getValue())
            ));
            linea.setTextSize(15);

            LinearLayout.LayoutParams lineaParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lineaParams.topMargin = dpToPx(4);
            linea.setLayoutParams(lineaParams);
            detalles.addView(linea);
        }

        item.addView(detalles);
        bloque.addView(item);

        View separador = new View(requireContext());
        LinearLayout.LayoutParams separadorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        separadorParams.topMargin = dpToPx(8);
        separador.setLayoutParams(separadorParams);
        separador.setBackgroundColor(0xFFDDDDDD);
        bloque.addView(separador);

        registrosContainer.addView(bloque);
    }

    private String obtenerIdentificadorGrupo() {
        String identificador = nombreTabla != null && !nombreTabla.trim().isEmpty() ? nombreTabla.trim() : nombreGrupo;
        return identificador != null ? identificador : "";
    }

    public String getTituloPantalla() {
        return valorSeguro(nombreGrupo);
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? getString(R.string.detalle_grupo_titulo)
                : valor.trim();
    }

    private boolean esColumnaDeFoto(String nombreColumna) {
        if (nombreColumna == null) {
            return false;
        }

        String normalizado = nombreColumna.trim().toLowerCase();
        return normalizado.contains("foto")
                || normalizado.contains("imagen")
                || normalizado.contains("image")
                || normalizado.contains("photo");
    }

    private String formatearNombreColumna(String nombre) {
        if (nombre == null) {
            return "";
        }

        String limpia = nombre.trim().replace('_', ' ');
        if (limpia.isEmpty()) {
            return "";
        }

        String[] palabras = limpia.split("\\s+");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (palabra.isEmpty()) {
                continue;
            }

            if (resultado.length() > 0) {
                resultado.append(' ');
            }

            resultado.append(Character.toUpperCase(palabra.charAt(0)));
            if (palabra.length() > 1) {
                resultado.append(palabra.substring(1).toLowerCase());
            }
        }

        return resultado.toString();
    }

    @Nullable
    private Bitmap obtenerImagenRegistro(Map<String, Object> valores) {
        for (Map.Entry<String, Object> entry : valores.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            if (!esColumnaDeFoto(entry.getKey())) {
                continue;
            }

            Bitmap bitmap = decodificarImagenBase64(String.valueOf(entry.getValue()));
            if (bitmap != null) {
                return bitmap;
            }
        }

        return null;
    }

    @Nullable
    private Bitmap decodificarImagenBase64(String valor) {
        try {
            byte[] bytes = Base64.decode(valor, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
