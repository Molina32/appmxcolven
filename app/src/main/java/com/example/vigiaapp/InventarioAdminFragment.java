package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.Archivos.ActualizarNombreGrupoRequest;
import com.example.vigiaapp.Archivos.CampoGrupoRequest;
import com.example.vigiaapp.Archivos.CrearGrupoRequest;
import com.example.vigiaapp.Archivos.Grupo;
import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InventarioAdminFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InventarioAdminFragment extends Fragment {
    private static final int MAX_COLUMNAS = 8;
    private static final String PREFS_GROUP_COLUMNS = "vigia_group_columns";
    private static final String KEY_TABLE_PREFIX = "table_";
    private static final String KEY_GROUP_PREFIX = "group_";
    private static final String COLUMN_SEPARATOR = "\u001F";

    // public class InventarioFrag add fragment extends Fragment
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private ApiService apiService;
    private TextView tvEstadoGrupos;
    private ProgressBar progressGrupos;
    private LinearLayout gruposContainer;

    public InventarioAdminFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InventarioAdminFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InventarioAdminFragment newInstance(String param1, String param2) {
        InventarioAdminFragment fragment = new InventarioAdminFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventario_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class);
        tvEstadoGrupos = view.findViewById(R.id.tvEstadoGrupos);
        progressGrupos = view.findViewById(R.id.progressGrupos);
        gruposContainer = view.findViewById(R.id.gruposContainer);

        Button btnCrearGrupo = view.findViewById(R.id.btnCrearGrupo);
        btnCrearGrupo.setOnClickListener(v -> showCreateGroupDialog());

        cargarGrupos();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            cargarGrupos();
        }
    }

    private void showCreateGroupDialog() {
        if (getContext() == null) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_crear_grupo, null);
        EditText inputNombreGrupo = dialogView.findViewById(R.id.etNombreGrupoDialog);
        Button btnAgregarCampo = dialogView.findViewById(R.id.btnAgregarCampoDialog);
        LinearLayout camposContainer = dialogView.findViewById(R.id.camposContainerDialog);
        List<EditText> inputsCampos = new ArrayList<>();

        btnAgregarCampo.setOnClickListener(v -> agregarCampoDinamico(camposContainer, inputsCampos));
        agregarCampoDinamico(camposContainer, inputsCampos);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.crear_grupo)
                .setView(dialogView)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String nombreGrupo = inputNombreGrupo.getText() != null
                            ? inputNombreGrupo.getText().toString().trim()
                            : "";

                    if (nombreGrupo.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.nombre_grupo_requerido, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<CampoGrupoRequest> columnas = obtenerColumnasConfiguradas(inputsCampos);
                    if (columnas == null) {
                        Toast.makeText(requireContext(), R.string.campos_nombre_requerido, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (columnas.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.minimo_un_campo_requerido, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    crearGrupo(nombreGrupo, columnas);
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private void agregarCampoDinamico(LinearLayout camposContainer, List<EditText> inputsCampos) {
        if (inputsCampos.size() >= MAX_COLUMNAS) {
            Toast.makeText(requireContext(), R.string.maximo_campos_alcanzado, Toast.LENGTH_SHORT).show();
            return;
        }

        EditText inputCampo = new EditText(requireContext());
        inputCampo.setHint(getString(R.string.nombre_campo_hint, inputsCampos.size() + 1));
        inputCampo.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(8);
        inputCampo.setLayoutParams(params);

        inputsCampos.add(inputCampo);
        camposContainer.addView(inputCampo);
    }

    private List<CampoGrupoRequest> obtenerColumnasConfiguradas(List<EditText> inputsCampos) {
        List<CampoGrupoRequest> columnas = new ArrayList<>();

        for (EditText inputCampo : inputsCampos) {
            String nombreCampo = inputCampo.getText() != null
                    ? inputCampo.getText().toString().trim()
                    : "";

            if (nombreCampo.isEmpty()) {
                return null;
            }

            columnas.add(new CampoGrupoRequest(nombreCampo, resolverTipoCampo(nombreCampo)));
        }

        agregarColumnaStockSiFalta(columnas);

        return columnas;
    }

    private void agregarColumnaStockSiFalta(List<CampoGrupoRequest> columnas) {
        if (columnas == null) {
            return;
        }

        for (CampoGrupoRequest columna : columnas) {
            if (columna != null && esColumnaStock(columna.getNombreCampo())) {
                return;
            }
        }

        columnas.add(new CampoGrupoRequest(getString(R.string.stock_columna_predeterminada), "TEXT"));
    }

    private String resolverTipoCampo(String nombreCampo) {
        if (nombreCampo == null) {
            return "TEXT";
        }

        String normalizado = nombreCampo.trim().toLowerCase();
        if (normalizado.contains("foto")
                || normalizado.contains("imagen")
                || normalizado.contains("image")
                || normalizado.contains("photo")) {
            return "IMAGEN";
        }

        return "TEXT";
    }

    private boolean esColumnaStock(String nombreCampo) {
        if (nombreCampo == null) {
            return false;
        }

        String normalizado = nombreCampo.trim().toLowerCase();
        return normalizado.equals("stock");
    }

    private String obtenerMensajeErrorCreacion(Response<Grupo> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody != null && !errorBody.trim().isEmpty()) {
                    JSONObject errorJson = new JSONObject(errorBody);
                    String mensaje = errorJson.optString("message");
                    if (!mensaje.trim().isEmpty()) {
                        return mensaje.trim();
                    }
                    String error = errorJson.optString("error");
                    if (!error.trim().isEmpty()) {
                        return error.trim();
                    }
                    return errorBody.trim();
                }
            }
        } catch (Exception ignored) {
        }

        return getString(R.string.grupo_error_creacion);
    }

    private void crearGrupo(String nombreGrupo, List<CampoGrupoRequest> columnas) {
        progressGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setText(R.string.grupo_creando);

        apiService.crearGrupo(new CrearGrupoRequest(nombreGrupo, columnas)).enqueue(new Callback<Grupo>() {
            @Override
            public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    cacheConfiguredColumns(nombreGrupo, response.body() != null ? response.body().getNombreTabla() : null, columnas);
                    Toast.makeText(requireContext(), getString(R.string.grupo_creado_exito, nombreGrupo), Toast.LENGTH_SHORT).show();
                    cargarGrupos();
                } else {
                    progressGrupos.setVisibility(View.GONE);
                    tvEstadoGrupos.setVisibility(View.VISIBLE);
                    tvEstadoGrupos.setText(R.string.grupos_error_carga);
                    Toast.makeText(requireContext(), obtenerMensajeErrorCreacion(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                progressGrupos.setVisibility(View.GONE);
                tvEstadoGrupos.setVisibility(View.VISIBLE);
                tvEstadoGrupos.setText(R.string.grupos_error_carga);
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
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
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
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
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dpToPx(4), 0, dpToPx(4));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(12);
        item.setLayoutParams(params);

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

        LinearLayout.LayoutParams botonParams = new LinearLayout.LayoutParams(
                0,
                dpToPx(40)
        );
        botonParams.weight = 1f;
        botonParams.rightMargin = dpToPx(8);
        btnGrupo.setLayoutParams(botonParams);

        ImageButton btnEditar = new ImageButton(requireContext());
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        btnEditar.setBackgroundResource(outValue.resourceId);
        btnEditar.setImageResource(R.drawable.editar);
        btnEditar.setContentDescription(getString(R.string.editar_grupo));
        btnEditar.setElevation(0f);
        btnEditar.setStateListAnimator(null);
        btnEditar.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        btnEditar.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams editarParams = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        editarParams.rightMargin = dpToPx(8);
        btnEditar.setLayoutParams(editarParams);
        btnEditar.setOnClickListener(v -> showEditGroupNameDialog(grupo));

        ImageButton btnEliminar = new ImageButton(requireContext());
        btnEliminar.setBackgroundResource(outValue.resourceId);
        btnEliminar.setImageResource(R.drawable.borrar);
        btnEliminar.setContentDescription(getString(R.string.borrar));
        btnEliminar.setElevation(0f);
        btnEliminar.setStateListAnimator(null);
        btnEliminar.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        btnEliminar.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        btnEliminar.setOnClickListener(v -> showDeleteGroupConfirmation(grupo));
        btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        ));

        item.addView(btnGrupo);
        item.addView(btnEditar);
        item.addView(btnEliminar);

        return item;
    }

    private void showEditGroupNameDialog(Grupo grupo) {
        if (getContext() == null || grupo == null) {
            return;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int marginHorizontal = dpToPx(18);
        int paddingTop = dpToPx(8);
        container.setPadding(marginHorizontal, paddingTop, marginHorizontal, 0);

        TextView labelNombreGrupo = new TextView(requireContext());
        labelNombreGrupo.setText(R.string.nombre_grupo_hint);
        labelNombreGrupo.setTextSize(12);
        container.addView(labelNombreGrupo);

        EditText inputNombreGrupo = new EditText(requireContext());
        inputNombreGrupo.setInputType(InputType.TYPE_CLASS_TEXT);
        inputNombreGrupo.setHint(R.string.escanea_o_escribe_texto);
        inputNombreGrupo.setText(valorSeguro(grupo.getNombreMostrado()));
        inputNombreGrupo.setSelection(inputNombreGrupo.getText().length());

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = dpToPx(6);
        inputNombreGrupo.setLayoutParams(inputParams);
        container.addView(inputNombreGrupo);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.editar_grupo_titulo)
                .setView(container)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String nombreNuevo = inputNombreGrupo.getText() != null
                            ? inputNombreGrupo.getText().toString().trim()
                            : "";

                    if (nombreNuevo.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.nombre_grupo_requerido, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    actualizarNombreGrupo(grupo, nombreNuevo, dialog);
                }));

        dialog.show();
    }

    private void showDeleteGroupConfirmation(Grupo grupo) {
        if (getContext() == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.borrar_grupo_confirmacion, valorSeguro(grupo.getNombreMostrado())))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> eliminarGrupo(grupo))
                .show();
    }

    private void eliminarGrupo(Grupo grupo) {
        Long idGrupo = grupo.getId();
        if (idGrupo == null) {
            Toast.makeText(requireContext(), R.string.grupo_error_eliminacion, Toast.LENGTH_SHORT).show();
            return;
        }

        progressGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setText(R.string.grupos_cargando);

        apiService.eliminarGrupo(idGrupo).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }

                progressGrupos.setVisibility(View.GONE);
                tvEstadoGrupos.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.grupo_eliminado_exito, Toast.LENGTH_SHORT).show();
                    cargarGrupos();
                } else {
                    Toast.makeText(requireContext(), obtenerMensajeErrorEliminacion(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                progressGrupos.setVisibility(View.GONE);
                tvEstadoGrupos.setVisibility(View.GONE);
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarNombreGrupo(Grupo grupo, String nombreNuevo, AlertDialog dialog) {
        String identificador = grupo.getId() != null
                ? String.valueOf(grupo.getId())
                : (grupo.getNombreTabla() != null ? grupo.getNombreTabla() : grupo.getNombreGrupo());

        progressGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setVisibility(View.VISIBLE);
        tvEstadoGrupos.setText(R.string.grupos_cargando);

        apiService.actualizarNombreGrupo(identificador, new ActualizarNombreGrupoRequest(nombreNuevo))
                .enqueue(new Callback<Grupo>() {
                    @Override
                    public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                        if (!isAdded()) {
                            return;
                        }

                        progressGrupos.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(requireContext(), R.string.grupo_actualizado_exito, Toast.LENGTH_SHORT).show();
                            cargarGrupos();
                        } else {
                            tvEstadoGrupos.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), obtenerMensajeErrorActualizacion(response), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                        if (!isAdded()) {
                            return;
                        }

                        progressGrupos.setVisibility(View.GONE);
                        tvEstadoGrupos.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String obtenerMensajeErrorActualizacion(Response<Grupo> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody != null && !errorBody.trim().isEmpty()) {
                    JSONObject errorJson = new JSONObject(errorBody);
                    String mensaje = errorJson.optString("message");
                    if (!mensaje.trim().isEmpty()) {
                        return mensaje.trim();
                    }
                    return errorBody.trim();
                }
            }
        } catch (Exception ignored) {
        }

        return getString(R.string.grupo_actualizado_error);
    }

    private String obtenerMensajeErrorEliminacion(Response<Void> response) {
        String detalle = "HTTP " + response.code();

        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody != null && !errorBody.trim().isEmpty()) {
                    detalle = detalle + ": " + errorBody.trim();
                }
            } else if (response.message() != null && !response.message().trim().isEmpty()) {
                detalle = detalle + ": " + response.message().trim();
            }
        } catch (IOException ignored) {
        }

        return getString(R.string.grupo_error_eliminacion_detalle, detalle);
    }

    private void abrirGrupo(Grupo grupo) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        GrupoInventarioAdminFragment.newInstance(
                                valorSeguro(grupo.getNombreMostrado()),
                                grupo.getNombreTabla(),
                                grupo.getColumnasMostradas()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void cacheConfiguredColumns(String nombreGrupo, String nombreTabla, List<CampoGrupoRequest> columnas) {
        if (getContext() == null || columnas == null || columnas.isEmpty()) {
            return;
        }

        ArrayList<String> nombresColumnas = new ArrayList<>();
        for (CampoGrupoRequest columna : columnas) {
            if (columna == null || columna.getNombreCampo() == null) {
                continue;
            }

            String nombreCampo = columna.getNombreCampo().trim();
            if (!nombreCampo.isEmpty()) {
                nombresColumnas.add(nombreCampo);
            }
        }

        if (nombresColumnas.isEmpty()) {
            return;
        }

        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS_GROUP_COLUMNS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(construirCacheKey(KEY_GROUP_PREFIX, nombreGrupo), String.join(COLUMN_SEPARATOR, nombresColumnas));

        if (nombreTabla != null && !nombreTabla.trim().isEmpty()) {
            editor.putString(construirCacheKey(KEY_TABLE_PREFIX, nombreTabla), String.join(COLUMN_SEPARATOR, nombresColumnas));
        }

        editor.apply();
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
}
