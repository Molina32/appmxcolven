package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.Archivos.Grupo;
import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventarioFragment extends Fragment {
    private static final String PREFS_GROUP_COLUMNS = "vigia_group_columns";
    private static final String KEY_TABLE_PREFIX = "table_";
    private static final String KEY_GROUP_PREFIX = "group_";
    private static final String COLUMN_SEPARATOR = "\u001F";

    private ApiService apiService;
    private TextView tvEstadoGrupos;
    private ProgressBar progressGrupos;
    private LinearLayout gruposContainer;

    public InventarioFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class);
        tvEstadoGrupos = view.findViewById(R.id.tvEstadoGruposUsuario);
        progressGrupos = view.findViewById(R.id.progressGruposUsuario);
        gruposContainer = view.findViewById(R.id.gruposContainerUsuario);
        cargarGrupos();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            cargarGrupos();
        }
    }

    private void cargarGrupos() {
        if (apiService == null || tvEstadoGrupos == null || progressGrupos == null || gruposContainer == null) {
            return;
        }

        tvEstadoGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setText(R.string.grupos_cargando);
        progressGrupos.setVisibility(View.VISIBLE);
        gruposContainer.removeAllViews();

        apiService.getGrupos().enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Grupo>> call, @NonNull Response<List<Grupo>> response) {
                if (!isAdded()) {
                    return;
                }

                progressGrupos.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    mostrarGrupos(response.body());
                } else {
                    mostrarErrorGrupos();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Grupo>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                progressGrupos.setVisibility(View.GONE);
                mostrarErrorGrupos();
                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarGrupos(List<Grupo> grupos) {
        gruposContainer.removeAllViews();

        if (grupos.isEmpty()) {
            tvEstadoGrupos.setVisibility(View.VISIBLE);
            tvEstadoGrupos.setText(R.string.grupos_sin_resultados);
            return;
        }

        tvEstadoGrupos.setVisibility(View.GONE);
        for (Grupo grupo : grupos) {
            if (grupo == null) {
                continue;
            }
            aplicarColumnasCache(grupo);
            gruposContainer.addView(crearVistaGrupo(grupo));
        }
    }

    private void mostrarErrorGrupos() {
        tvEstadoGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setText(R.string.grupos_error_carga);
    }

    private View crearVistaGrupo(Grupo grupo) {
        Button btnGrupo = new Button(requireContext());
        btnGrupo.setAllCaps(true);
        btnGrupo.setText(valorSeguro(grupo.getNombreMostrado()));
        btnGrupo.setBackgroundColor(0xFFE0E0E0);
        btnGrupo.setTextColor(Color.BLACK);
        btnGrupo.setElevation(0f);
        btnGrupo.setStateListAnimator(null);
        btnGrupo.setMinHeight(0);
        btnGrupo.setMinimumHeight(0);
        btnGrupo.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        btnGrupo.setOnClickListener(v -> abrirGrupo(grupo));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(40)
        );
        params.bottomMargin = dpToPx(12);
        btnGrupo.setLayoutParams(params);
        return btnGrupo;
    }

    private void abrirGrupo(Grupo grupo) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        GrupoInventarioFragment.newInstance(
                                valorSeguro(grupo.getNombreMostrado()),
                                grupo.getNombreTabla(),
                                grupo.getColumnasMostradas()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    private void aplicarColumnasCache(Grupo grupo) {
        if (grupo == null || !grupo.getColumnasMostradas().isEmpty() || getContext() == null) {
            return;
        }

        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS_GROUP_COLUMNS, Context.MODE_PRIVATE);
        ArrayList<String> columnasGuardadas = obtenerColumnasCache(preferences, construirCacheKey(KEY_TABLE_PREFIX, grupo.getNombreTabla()));

        if (columnasGuardadas.isEmpty()) {
            columnasGuardadas = obtenerColumnasCache(preferences, construirCacheKey(KEY_GROUP_PREFIX, grupo.getNombreGrupo()));
        }

        if (!columnasGuardadas.isEmpty()) {
            grupo.setNombresColumnas(columnasGuardadas);
        }
    }

    private ArrayList<String> obtenerColumnasCache(SharedPreferences preferences, String key) {
        ArrayList<String> columnas = new ArrayList<>();
        if (key == null || key.trim().isEmpty()) {
            return columnas;
        }

        String guardado = preferences.getString(key, "");
        if (guardado == null || guardado.trim().isEmpty()) {
            return columnas;
        }

        String[] partes = guardado.split(COLUMN_SEPARATOR);
        for (String parte : partes) {
            if (parte == null) {
                continue;
            }

            String nombre = parte.trim();
            if (!nombre.isEmpty()) {
                columnas.add(nombre);
            }
        }

        return columnas;
    }

    private String construirCacheKey(String prefix, String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase().replace(' ', '_');
        return prefix + normalized;
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
