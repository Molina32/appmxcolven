package com.example.vigiaapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.Archivos.ActualizarColumnasGrupoRequest;
import com.example.vigiaapp.Archivos.CampoGrupoRequest;
import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GrupoInventarioAdminFragment extends Fragment {
    private static final String ARG_NOMBRE_GRUPO = "nombre_grupo";
    private static final String ARG_NOMBRE_TABLA = "nombre_tabla";
    private static final String ARG_COLUMNAS = "columnas";
    private static final int MAX_COLUMNAS = 8;
    private static final String PREFS_GROUP_COLUMNS = "vigia_group_columns";
    private static final String KEY_TABLE_PREFIX = "table_";
    private static final String KEY_GROUP_PREFIX = "group_";
    private static final String COLUMN_SEPARATOR = "\u001F";
    private static final int STOCK_INICIAL = 0;

    private String nombreGrupo;
    private String nombreTabla;
    private ArrayList<String> columnas;
    private TextView tvGrupoSeleccionado;
    private TextView tvContenidoGrupo;
    private LinearLayout registrosContainer;
    private Button btnAgregarRegistro;
    private Button btnEditarColumnas;
    private ApiService apiService;
    private final ArrayList<LinkedHashMap<String, Object>> registrosGuardados = new ArrayList<>();
    private ActivityResultLauncher<String> seleccionarImagenLauncher;
    private FotoInput fotoInputPendiente;

    public GrupoInventarioAdminFragment() {
    }

    public static GrupoInventarioAdminFragment newInstance(String nombreGrupo, String nombreTabla, ArrayList<String> columnas) {
        GrupoInventarioAdminFragment fragment = new GrupoInventarioAdminFragment();
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
        seleccionarImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::procesarImagenSeleccionada
        );
        if (getArguments() != null) {
            nombreGrupo = getArguments().getString(ARG_NOMBRE_GRUPO);
            nombreTabla = getArguments().getString(ARG_NOMBRE_TABLA);
            columnas = getArguments().getStringArrayList(ARG_COLUMNAS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grupo_inventario_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGrupoSeleccionado = view.findViewById(R.id.tvGrupoSeleccionado);
        tvContenidoGrupo = view.findViewById(R.id.tvContenidoGrupo);
        registrosContainer = view.findViewById(R.id.registrosContainer);
        btnAgregarRegistro = view.findViewById(R.id.btnAgregarRegistro);
        btnEditarColumnas = view.findViewById(R.id.btnEditarColumnas);
        apiService = ApiClient.getClient().create(ApiService.class);

        String nombreMostrado = valorSeguro(nombreGrupo);
        tvGrupoSeleccionado.setText(getString(R.string.grupo_seleccionado_titulo, nombreMostrado));
        tvContenidoGrupo.setText(R.string.registros_grupo_vacio);
        actualizarEstadoBotones();
        cargarColumnasDesdeApi();

        btnAgregarRegistro.setOnClickListener(v -> showAgregarRegistroDialog());
        btnEditarColumnas.setOnClickListener(v -> showEditarColumnasDialog());
    }

    private void showAgregarRegistroDialog() {
        showRegistroDialog(null);
    }

    private void showRegistroDialog(@Nullable LinkedHashMap<String, Object> registroExistente) {
        if (getContext() == null) {
            return;
        }

        if (columnas == null || columnas.isEmpty()) {
            Toast.makeText(requireContext(), R.string.grupo_sin_columnas, Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, 0);

        List<RegistroInput> inputs = new ArrayList<>();
        FotoInput fotoInput = crearVistaFotoPorDefecto(container, obtenerBase64Imagen(registroExistente));
        for (String columna : columnas) {
            if (esColumnaDeFoto(columna) || esColumnaStock(columna)) {
                continue;
            }

            container.addView(crearEtiquetaCampo(formatearNombreColumna(columna)));

            EditText input = new EditText(requireContext());
            input.setHint(R.string.escanea_o_escribe_texto);
            if (registroExistente != null) {
                input.setText(obtenerValorRegistro(registroExistente, columna));
            }
            input.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            inputs.add(RegistroInput.desdeTexto(columna, input));
            container.addView(input);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(registroExistente == null ? R.string.agregar : R.string.editar_registro)
                .setView(container)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    Map<String, Object> valores = obtenerValores(inputs);
                    boolean tieneFoto = fotoInput.base64 != null && !fotoInput.base64.trim().isEmpty();
                    if (valores == null || (valores.isEmpty() && !tieneFoto)) {
                        Toast.makeText(requireContext(), R.string.campos_registro_requeridos, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Long registroId = obtenerIdRegistro(registroExistente);
                    if (registroExistente != null && registroId == null) {
                        Toast.makeText(requireContext(), R.string.registro_actualizado_error_id, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    aplicarValorStock(valores, registroExistente);
                    prepararGuardadoRegistro(valores, fotoInput, dialog, registroId);
                }));

        dialog.show();
    }

    private void showEditarColumnasDialog() {
        if (getContext() == null) {
            return;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, 0);

        Button btnAgregarCampo = new Button(requireContext());
        btnAgregarCampo.setText(R.string.agregar_campo);
        container.addView(btnAgregarCampo);

        LinearLayout camposContainer = new LinearLayout(requireContext());
        camposContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(camposContainer);

        List<EditText> inputsCampos = new ArrayList<>();
        if (columnas != null && !columnas.isEmpty()) {
            for (String columna : columnas) {
                if (esColumnaDeFoto(columna) || esColumnaStock(columna)) {
                    continue;
                }
                agregarInputColumna(camposContainer, inputsCampos, columna);
            }
        } else {
            agregarInputColumna(camposContainer, inputsCampos, null);
        }

        btnAgregarCampo.setOnClickListener(v -> agregarInputColumna(camposContainer, inputsCampos, null));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.editar_columnas_titulo)
                .setView(container)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    List<CampoGrupoRequest> columnasActualizadas;
                    try {
                        columnasActualizadas = obtenerColumnasConfiguradas(inputsCampos);
                    } catch (IllegalArgumentException ex) {
                        Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (columnasActualizadas.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.minimo_un_campo_requerido, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    agregarColumnasNoEditables(columnasActualizadas);
                    actualizarColumnasEnApi(columnasActualizadas, dialog);
                }));

        dialog.show();
    }



    private Map<String, Object> obtenerValores(List<RegistroInput> inputs) {
        Map<String, Object> valores = new LinkedHashMap<>();

        for (RegistroInput input : inputs) {
            String valor = input.obtenerValor();
            String columna = input.nombreColumna;

            if (valor.isEmpty()) {
                return null;
            }

            valores.put(columna, valor);
        }

        return valores;
    }

    private FotoInput crearVistaFotoPorDefecto(LinearLayout containerPadre, @Nullable String imagenBase64Inicial) {
        View vistaFoto = crearVistaFoto(getString(R.string.foto_columna_predeterminada), imagenBase64Inicial);
        containerPadre.addView(vistaFoto);
        Object tag = vistaFoto.getTag();
        return tag instanceof FotoInput ? (FotoInput) tag : new FotoInput(new ImageView(requireContext()));
    }

    private View crearVistaFoto(String columna, @Nullable String imagenBase64Inicial) {
        LinearLayout fotoContainer = new LinearLayout(requireContext());
        fotoContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.topMargin = dpToPx(8);
        fotoContainer.setLayoutParams(containerParams);

        TextView titulo = new TextView(requireContext());
        titulo.setText(columna);
        fotoContainer.addView(titulo);

        ImageView preview = new ImageView(requireContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(120)
        );
        previewParams.topMargin = dpToPx(8);
        preview.setLayoutParams(previewParams);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(0xFFEAEAEA);
        fotoContainer.addView(preview);

        Button btnSeleccionarFoto = new Button(requireContext());
        btnSeleccionarFoto.setText(R.string.seleccionar_foto);
        LinearLayout.LayoutParams botonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        botonParams.topMargin = dpToPx(8);
        btnSeleccionarFoto.setLayoutParams(botonParams);
        fotoContainer.addView(btnSeleccionarFoto);

        FotoInput fotoInput = new FotoInput(preview);
        if (imagenBase64Inicial != null && !imagenBase64Inicial.trim().isEmpty()) {
            fotoInput.base64 = imagenBase64Inicial.trim();
            Bitmap bitmapInicial = decodificarImagenBase64(imagenBase64Inicial);
            if (bitmapInicial != null) {
                preview.setImageBitmap(bitmapInicial);
            } else {
                preview.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            preview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        fotoContainer.setTag(fotoInput);

        btnSeleccionarFoto.setOnClickListener(v -> {
            fotoInputPendiente = fotoInput;
            seleccionarImagenLauncher.launch("image/*");
        });



        return fotoContainer;
    }

    private TextView crearEtiquetaCampo(String texto) {
        TextView etiqueta = new TextView(requireContext());
        etiqueta.setText(texto);
        etiqueta.setTextSize(12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(8);
        etiqueta.setLayoutParams(params);
        return etiqueta;
    }

    private void agregarInputColumna(LinearLayout camposContainer, List<EditText> inputsCampos, @Nullable String valorInicial) {
        if (inputsCampos.size() >= MAX_COLUMNAS) {
            Toast.makeText(requireContext(), R.string.maximo_campos_alcanzado, Toast.LENGTH_SHORT).show();
            return;
        }

        String textoEtiqueta = valorInicial != null && !valorInicial.trim().isEmpty()
                ? formatearNombreColumna(valorInicial)
                : getString(R.string.nombre_campo_hint, inputsCampos.size() + 1);
        camposContainer.addView(crearEtiquetaCampo(textoEtiqueta));

        EditText inputCampo = new EditText(requireContext());
        inputCampo.setHint(R.string.escanea_o_escribe_texto);
        inputCampo.setInputType(InputType.TYPE_CLASS_TEXT);
        if (valorInicial != null) {
            inputCampo.setText(valorInicial);
            inputCampo.setSelection(valorInicial.length());
        }

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
        List<CampoGrupoRequest> columnasConfiguradas = new ArrayList<>();
        Set<String> nombresNormalizados = new HashSet<>();

        for (EditText inputCampo : inputsCampos) {
            String nombreCampo = inputCampo.getText() != null
                    ? inputCampo.getText().toString().trim()
                    : "";

            if (nombreCampo.isEmpty()) {
                throw new IllegalArgumentException(getString(R.string.columnas_requieren_nombre));
            }

            String nombreNormalizado = normalizarNombre(nombreCampo);
            if (!nombresNormalizados.add(nombreNormalizado)) {
                throw new IllegalArgumentException(getString(R.string.columnas_repetidas));
            }

            columnasConfiguradas.add(new CampoGrupoRequest(nombreCampo, resolverTipoCampo(nombreCampo)));
        }

        return columnasConfiguradas;
    }

    private void agregarColumnasNoEditables(List<CampoGrupoRequest> columnasConfiguradas) {
        if (columnas == null || columnas.isEmpty()) {
            return;
        }

        Set<String> nombresActuales = new HashSet<>();
        for (CampoGrupoRequest columna : columnasConfiguradas) {
            if (columna != null && columna.getNombreCampo() != null) {
                nombresActuales.add(normalizarNombre(columna.getNombreCampo()));
            }
        }

        for (String columna : columnas) {
            if (!esColumnaNoEditable(columna)) {
                continue;
            }

            String nombreNormalizado = normalizarNombre(columna);
            if (nombresActuales.contains(nombreNormalizado)) {
                continue;
            }

            columnasConfiguradas.add(new CampoGrupoRequest(columna, resolverTipoCampo(columna)));
        }
    }

    private void guardarRegistroEnApi(Map<String, Object> valores, AlertDialog dialog) {
        if (apiService == null) {
            return;
        }

        apiService.guardarRegistroGrupo(obtenerIdentificadorGrupo(), valores).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.registro_guardado_exito, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cargarRegistrosDesdeApi();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.registro_guardado_error_http, response.code()), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void prepararGuardadoRegistro(Map<String, Object> valores,
                                          FotoInput fotoInput,
                                          AlertDialog dialog,
                                          @Nullable Long registroId) {
        String fotoBase64 = fotoInput.base64 != null ? fotoInput.base64.trim() : "";
        asegurarColumnasSistemaYGuardar(valores, fotoBase64, dialog, registroId);
    }

    private void asegurarColumnasSistemaYGuardar(Map<String, Object> valores,
                                                 String fotoBase64,
                                                 AlertDialog dialog,
                                                 @Nullable Long registroId) {
        if (apiService == null) {
            return;
        }

        ArrayList<String> columnasActuales = columnas != null ? new ArrayList<>(columnas) : new ArrayList<>();
        String nuevaColumnaFoto = getString(R.string.foto_columna_predeterminada);
        String nuevaColumnaStock = getString(R.string.stock_columna_predeterminada);
        if (!fotoBase64.isEmpty() && obtenerColumnaFotoExistente() == null) {
            columnasActuales.add(nuevaColumnaFoto);
        }
        if (obtenerColumnaStockExistente() == null) {
            columnasActuales.add(nuevaColumnaStock);
        }

        List<CampoGrupoRequest> columnasActualizadas = new ArrayList<>();
        for (String columna : columnasActuales) {
            columnasActualizadas.add(new CampoGrupoRequest(columna, resolverTipoCampo(columna)));
        }

        apiService.actualizarColumnasGrupo(
                obtenerIdentificadorGrupo(),
                new ActualizarColumnasGrupoRequest(columnasActualizadas)
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    columnas = new ArrayList<>(columnasActuales);
                    guardarColumnasCache(columnas);
                    actualizarEstadoBotones();
                    if (!fotoBase64.isEmpty()) {
                        String columnaFoto = obtenerColumnaFotoExistente();
                        if (columnaFoto == null || columnaFoto.trim().isEmpty()) {
                            columnaFoto = nuevaColumnaFoto;
                        }
                        valores.put(columnaFoto, fotoBase64);
                    }
                    String columnaStock = obtenerColumnaStockExistente();
                    if (columnaStock == null || columnaStock.trim().isEmpty()) {
                        columnaStock = nuevaColumnaStock;
                    }
                    if (!valores.containsKey(columnaStock)) {
                        valores.put(columnaStock, String.valueOf(STOCK_INICIAL));
                    }
                    guardarRegistroEnApi(valores, dialog, registroId);
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.columnas_actualizadas_error_http, response.code()),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarRegistroEnApi(Map<String, Object> valores, AlertDialog dialog, @Nullable Long registroId) {
        if (registroId == null) {
            guardarRegistroEnApi(valores, dialog);
            return;
        }

        if (apiService == null) {
            return;
        }

        apiService.actualizarRegistroGrupo(obtenerIdentificadorGrupo(), registroId, valores)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (!isAdded()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), R.string.registro_actualizado_exito, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            cargarRegistrosDesdeApi();
                        } else {
                            Toast.makeText(requireContext(),
                                    getString(R.string.registro_actualizado_error_http, response.code()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(requireContext(),
                                getString(R.string.error_conexion_detalle, t.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarColumnasEnApi(List<CampoGrupoRequest> columnasActualizadas, AlertDialog dialog) {
        if (apiService == null) {
            return;
        }

        apiService.actualizarColumnasGrupo(
                obtenerIdentificadorGrupo(),
                new ActualizarColumnasGrupoRequest(columnasActualizadas)
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    columnas = obtenerNombresColumnas(columnasActualizadas);
                    guardarColumnasCache(columnas);
                    actualizarEstadoBotones();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.columnas_actualizadas_exito, Toast.LENGTH_SHORT).show();
                    cargarRegistrosDesdeApi();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.columnas_actualizadas_error_http, response.code()),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

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
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dpToPx(8), 0, dpToPx(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(8);
        item.setLayoutParams(params);

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

        LinearLayout bloque = new LinearLayout(requireContext());
        bloque.setOrientation(LinearLayout.VERTICAL);
        bloque.addView(item);

        LinearLayout accionesContainer = new LinearLayout(requireContext());
        accionesContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams accionesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        accionesParams.topMargin = dpToPx(4);
        accionesContainer.setLayoutParams(accionesParams);
        accionesContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);

        LinearLayout accionesIzquierda = new LinearLayout(requireContext());
        accionesIzquierda.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams accionesIzquierdaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        accionesIzquierda.setLayoutParams(accionesIzquierdaParams);

        ImageButton btnEditarRegistro = new ImageButton(requireContext());
        btnEditarRegistro.setBackgroundResource(outValue.resourceId);
        btnEditarRegistro.setImageResource(R.drawable.editar);
        btnEditarRegistro.setContentDescription(getString(R.string.editar_registro));
        btnEditarRegistro.setElevation(0f);
        btnEditarRegistro.setStateListAnimator(null);
        btnEditarRegistro.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        btnEditarRegistro.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        btnEditarRegistro.setOnClickListener(v -> showRegistroDialog(new LinkedHashMap<>(valores)));
        LinearLayout.LayoutParams editarParams = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        editarParams.rightMargin = dpToPx(8);
        btnEditarRegistro.setLayoutParams(editarParams);
        accionesIzquierda.addView(btnEditarRegistro);

        ImageButton btnBorrarRegistro = new ImageButton(requireContext());
        btnBorrarRegistro.setBackgroundResource(outValue.resourceId);
        btnBorrarRegistro.setImageResource(R.drawable.borrar);
        btnBorrarRegistro.setContentDescription(getString(R.string.borrar));
        btnBorrarRegistro.setElevation(0f);
        btnBorrarRegistro.setStateListAnimator(null);
        btnBorrarRegistro.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        btnBorrarRegistro.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        btnBorrarRegistro.setOnClickListener(v -> showEliminarRegistroDialog(new LinkedHashMap<>(valores)));
        btnBorrarRegistro.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        ));
        accionesIzquierda.addView(btnBorrarRegistro);

        accionesContainer.addView(accionesIzquierda);

        View separadorFlexible = new View(requireContext());
        LinearLayout.LayoutParams separadorFlexibleParams = new LinearLayout.LayoutParams(
                0,
                0
        );
        separadorFlexibleParams.weight = 1f;
        separadorFlexible.setLayoutParams(separadorFlexibleParams);
        accionesContainer.addView(separadorFlexible);

        LinearLayout accionesDerecha = new LinearLayout(requireContext());
        accionesDerecha.setOrientation(LinearLayout.HORIZONTAL);
        accionesContainer.addView(accionesDerecha);

        Button btnReducirStock = new Button(requireContext());
        btnReducirStock.setText(R.string.stock_menos);
        btnReducirStock.setOnClickListener(v -> cambiarStockDirecto(new LinkedHashMap<>(valores), -1));
        LinearLayout.LayoutParams reducirParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        reducirParams.rightMargin = dpToPx(8);
        btnReducirStock.setLayoutParams(reducirParams);
        accionesDerecha.addView(btnReducirStock);

        Button btnAumentarStock = new Button(requireContext());
        btnAumentarStock.setText(R.string.stock_mas);
        btnAumentarStock.setOnClickListener(v -> cambiarStockDirecto(new LinkedHashMap<>(valores), 1));
        accionesDerecha.addView(btnAumentarStock);

        bloque.addView(accionesContainer);

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

    private void cambiarStockDirecto(@NonNull LinkedHashMap<String, Object> registro, int delta) {
        Long registroId = obtenerIdRegistro(registro);
        if (registroId == null) {
            Toast.makeText(requireContext(), R.string.registro_actualizado_error_id, Toast.LENGTH_SHORT).show();
            return;
        }

        String columnaStock = obtenerColumnaStock(registro);
        if (columnaStock == null) {
            Toast.makeText(requireContext(), R.string.stock_columna_no_encontrada, Toast.LENGTH_SHORT).show();
            return;
        }

        Integer stockActual = obtenerStockActual(registro, columnaStock);
        if (stockActual == null) {
            Toast.makeText(requireContext(), R.string.stock_valor_actual_invalido, Toast.LENGTH_SHORT).show();
            return;
        }

        int nuevoStock = stockActual + delta;
        if (nuevoStock < 0) {
            Toast.makeText(requireContext(), R.string.stock_no_disponible, Toast.LENGTH_SHORT).show();
            return;
        }

        String articuloHistorial = construirDescripcionArticuloHistorial(registro);
        actualizarStockEnApi(registroId, delta, stockActual, nuevoStock, articuloHistorial);
    }

    private void showEliminarRegistroDialog(@NonNull LinkedHashMap<String, Object> registro) {
        Long registroId = obtenerIdRegistro(registro);
        if (registroId == null) {
            Toast.makeText(requireContext(), R.string.registro_eliminar_error_id, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.registro_eliminar_confirmacion)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> eliminarRegistroEnApi(registroId))
                .show();
    }

    private void eliminarRegistroEnApi(long registroId) {
        if (apiService == null) {
            return;
        }

        apiService.eliminarRegistroGrupo(obtenerIdentificadorGrupo(), registroId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (!isAdded()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), R.string.registro_eliminado_exito, Toast.LENGTH_SHORT).show();
                            cargarRegistrosDesdeApi();
                        } else {
                            Toast.makeText(requireContext(),
                                    getString(R.string.registro_eliminado_error_http, response.code()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(requireContext(),
                                getString(R.string.error_conexion_detalle, t.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarStockEnApi(long registroId,
                                      int delta,
                                      int stockAnterior,
                                      int stockNuevo,
                                      @NonNull String articuloHistorial) {
        if (apiService == null) {
            return;
        }

        Call<LinkedHashMap<String, Object>> call = delta > 0
                ? apiService.aumentarStockRegistro(obtenerIdentificadorGrupo(), registroId)
                : apiService.reducirStockRegistro(obtenerIdentificadorGrupo(), registroId);

        call.enqueue(new Callback<LinkedHashMap<String, Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<LinkedHashMap<String, Object>> call,
                                           @NonNull Response<LinkedHashMap<String, Object>> response) {
                        if (!isAdded()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            StockHistoryManager.registrarMovimiento(
                                    requireContext(),
                                    valorSeguro(nombreGrupo),
                                    articuloHistorial,
                                    delta > 0 ? getString(R.string.historial_tipo_aumento) : getString(R.string.historial_tipo_reduccion),
                                    Math.abs(delta),
                                    stockAnterior,
                                    stockNuevo
                            );
                            cargarRegistrosDesdeApi();
                        } else {
                            Toast.makeText(requireContext(),
                                    getString(R.string.stock_actualizado_error_http, response.code()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Throwable t) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(requireContext(),
                                getString(R.string.error_conexion_detalle, t.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cargarRegistrosDesdeApi() {
        if (apiService == null) {
            return;
        }

        tvContenidoGrupo.setVisibility(View.GONE);

        apiService.getRegistrosGrupo(obtenerIdentificadorGrupo()).enqueue(new Callback<List<LinkedHashMap<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<LinkedHashMap<String, Object>>> call, @NonNull Response<List<LinkedHashMap<String, Object>>> response) {
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
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarColumnasDesdeApi() {
        if (apiService == null) {
            return;
        }

        actualizarEstadoBotones(false);

        apiService.getColumnasGrupo(obtenerIdentificadorGrupo()).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    columnas = formatearColumnas(response.body());
                    guardarColumnasCache(columnas);
                    if (!tieneColumnaStock()) {
                        asegurarColumnaStockEnGrupo();
                        return;
                    }
                }

                actualizarEstadoBotones(true);
                cargarRegistrosDesdeApi();
            }

            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                actualizarEstadoBotones(true);
                cargarRegistrosDesdeApi();
                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }


        });
    }

    private void asegurarColumnaStockEnGrupo() {
        if (apiService == null) {
            actualizarEstadoBotones(true);
            cargarRegistrosDesdeApi();
            return;
        }

        ArrayList<String> columnasActuales = columnas != null ? new ArrayList<>(columnas) : new ArrayList<>();
        if (!columnasActuales.contains(getString(R.string.stock_columna_predeterminada))) {
            columnasActuales.add(getString(R.string.stock_columna_predeterminada));
        }

        List<CampoGrupoRequest> columnasActualizadas = new ArrayList<>();
        for (String columna : columnasActuales) {
            columnasActualizadas.add(new CampoGrupoRequest(columna, resolverTipoCampo(columna)));
        }

        apiService.actualizarColumnasGrupo(
                obtenerIdentificadorGrupo(),
                new ActualizarColumnasGrupoRequest(columnasActualizadas)
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    columnas = new ArrayList<>(columnasActuales);
                    guardarColumnasCache(columnas);
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.stock_columna_error_http, response.code()),
                            Toast.LENGTH_LONG).show();
                }

                actualizarEstadoBotones(true);
                cargarRegistrosDesdeApi();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                actualizarEstadoBotones(true);
                cargarRegistrosDesdeApi();
                Toast.makeText(requireContext(),
                        getString(R.string.stock_columna_error_conexion, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String obtenerIdentificadorGrupo() {
        String identificador = nombreTabla != null && !nombreTabla.trim().isEmpty() ? nombreTabla.trim() : nombreGrupo;
        return identificador != null ? identificador : "";
    }

    public String getTituloPantalla() {
        return valorSeguro(nombreGrupo);
    }

    public String getNombreTabla() {
        return nombreTabla;
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? getString(R.string.detalle_grupo_titulo)
                : valor.trim();
    }

    private void actualizarEstadoBotones() {
        actualizarEstadoBotones(true);
    }

    private void actualizarEstadoBotones(boolean habilitarEdicion) {
        if (btnEditarColumnas != null) {
            btnEditarColumnas.setEnabled(habilitarEdicion);
        }

        if (btnAgregarRegistro != null) {
            btnAgregarRegistro.setEnabled(habilitarEdicion && columnas != null && !columnas.isEmpty());
        }
    }

    private ArrayList<String> formatearColumnas(List<String> columnasApi) {
        ArrayList<String> columnasFormateadas = new ArrayList<>();
        if (columnasApi == null) {
            return columnasFormateadas;
        }

        for (String columna : columnasApi) {
            String nombre = formatearNombreColumna(columna);
            if (!nombre.isEmpty() && !columnasFormateadas.contains(nombre)) {
                columnasFormateadas.add(nombre);
            }
        }

        return columnasFormateadas;
    }

    private ArrayList<String> obtenerNombresColumnas(List<CampoGrupoRequest> columnasActualizadas) {
        ArrayList<String> nombres = new ArrayList<>();
        for (CampoGrupoRequest columna : columnasActualizadas) {
            if (columna == null || columna.getNombreCampo() == null) {
                continue;
            }

            String nombre = columna.getNombreCampo().trim();
            if (!nombre.isEmpty()) {
                nombres.add(nombre);
            }
        }
        return nombres;
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

    private String normalizarNombre(String valor) {
        return valor == null
                ? ""
                : valor.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String normalizarIdentificadorLocal(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    private boolean esColumnaStock(@Nullable String nombreColumna) {
        if (nombreColumna == null) {
            return false;
        }

        String normalizado = normalizarIdentificadorLocal(nombreColumna);
        return normalizado.equals(normalizarIdentificadorLocal(getString(R.string.stock_columna_predeterminada)));
    }

    private boolean tieneColumnaStock() {
        if (columnas == null) {
            return false;
        }

        for (String columna : columnas) {
            if (esColumnaStock(columna)) {
                return true;
            }
        }

        return false;
    }

    private boolean esColumnaNoEditable(@Nullable String nombreColumna) {
        return esColumnaDeFoto(nombreColumna) || esColumnaStock(nombreColumna);
    }

    @Nullable
    private String obtenerColumnaStock(@Nullable Map<String, Object> registro) {
        if (registro != null) {
            for (Map.Entry<String, Object> entry : registro.entrySet()) {
                if (esColumnaStock(entry.getKey())) {
                    return entry.getKey();
                }
            }
        }

        if (columnas != null) {
            for (String columna : columnas) {
                if (esColumnaStock(columna)) {
                    return columna;
                }
            }
        }

        return null;
    }

    private void aplicarValorStock(@NonNull Map<String, Object> valores, @Nullable Map<String, Object> registroExistente) {
        String columnaStock = obtenerColumnaStock(registroExistente);
        if (columnaStock == null || columnaStock.trim().isEmpty()) {
            columnaStock = getString(R.string.stock_columna_predeterminada);
        }

        if (registroExistente == null) {
            valores.put(columnaStock, String.valueOf(STOCK_INICIAL));
            return;
        }

        String valorActual = obtenerValorRegistro(registroExistente, columnaStock);
        if (valorActual == null || valorActual.trim().isEmpty()) {
            valorActual = String.valueOf(STOCK_INICIAL);
        }
        valores.put(columnaStock, valorActual);
    }

    @Nullable
    private Integer obtenerStockActual(@NonNull Map<String, Object> registro, @NonNull String columnaStock) {
        Object valorObj = registro.get(columnaStock);
        if (valorObj instanceof Number) {
            return ((Number) valorObj).intValue();
        }

        String valor = obtenerValorRegistro(registro, columnaStock);
        if (valor == null || valor.trim().isEmpty()) {
            return 0;
        }

        try {
            // Try parsing as double first to handle decimal values like 0.0, 1.0
            return (int) Double.parseDouble(valor.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @NonNull
    private String construirDescripcionArticuloHistorial(@NonNull Map<String, Object> registro) {
        String codigo = "";
        String descripcion = "";

        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            String clave = normalizarIdentificadorLocal(entry.getKey());
            String valor = String.valueOf(entry.getValue()).trim();
            if (valor.isEmpty()) {
                continue;
            }

            if (codigo.isEmpty() && clave.contains("codigo")) {
                codigo = valor;
            } else if (descripcion.isEmpty() && (clave.contains("descripcion") || clave.contains("nombre"))) {
                descripcion = valor;
            }
        }

        if (!codigo.isEmpty() && !descripcion.isEmpty()) {
            return codigo + " - " + descripcion;
        }
        if (!codigo.isEmpty()) {
            return codigo;
        }
        if (!descripcion.isEmpty()) {
            return descripcion;
        }

        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            String claveNormalizada = entry.getKey().trim().toLowerCase();
            if (claveNormalizada.equals("id")
                    || claveNormalizada.equals("idregistro")
                    || claveNormalizada.equals("id_registro")
                    || esColumnaDeFoto(entry.getKey())
                    || esColumnaStock(entry.getKey())) {
                continue;
            }

            return String.valueOf(entry.getValue()).trim();
        }

        return getString(R.string.historial_articulo_generico);
    }

    private void guardarColumnasCache(ArrayList<String> nombresColumnas) {
        if (getContext() == null || nombresColumnas == null || nombresColumnas.isEmpty()) {
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

    private String construirCacheKey(String prefix, String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase().replace(' ', '_');
        return prefix + normalized;
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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

    private String resolverTipoCampo(String nombreColumna) {
        return esColumnaDeFoto(nombreColumna) ? "IMAGEN" : "TEXT";
    }

    @Nullable
    private String obtenerColumnaFotoExistente() {
        if (columnas == null) {
            return null;
        }

        for (String columna : columnas) {
            if (esColumnaDeFoto(columna)) {
                return columna;
            }
        }

        return null;
    }

    @Nullable
    private String obtenerColumnaStockExistente() {
        if (columnas == null) {
            return null;
        }

        for (String columna : columnas) {
            if (esColumnaStock(columna)) {
                return columna;
            }
        }

        return null;
    }

    @Nullable
    private Long obtenerIdRegistro(@Nullable Map<String, Object> registro) {
        if (registro == null) {
            return null;
        }

        String[] claves = {"id", "idRegistro", "id_registro"};
        for (String clave : claves) {
            Object valor = registro.get(clave);
            if (valor instanceof Number) {
                return ((Number) valor).longValue();
            }
            if (valor != null) {
                try {
                    return Long.valueOf(String.valueOf(valor));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return null;
    }

    private String obtenerValorRegistro(Map<String, Object> registro, String columna) {
        if (registro == null || columna == null) {
            return "";
        }

        String buscada = normalizarIdentificadorLocal(columna);
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (normalizarIdentificadorLocal(entry.getKey()).equals(buscada)) {
                return String.valueOf(entry.getValue());
            }
        }

        return "";
    }

    @Nullable
    private String obtenerBase64Imagen(@Nullable Map<String, Object> registro) {
        if (registro == null) {
            return null;
        }

        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (esColumnaDeFoto(entry.getKey())) {
                return String.valueOf(entry.getValue());
            }
        }

        return null;
    }

    private void procesarImagenSeleccionada(@Nullable Uri uri) {
        if (!isAdded() || fotoInputPendiente == null || uri == null) {
            return;
        }

        try {
            String imagenBase64 = convertirImagenABase64(uri);
            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.foto_error_carga, Toast.LENGTH_SHORT).show();
                return;
            }

            fotoInputPendiente.base64 = imagenBase64;
            fotoInputPendiente.preview.setImageURI(uri);
            Toast.makeText(requireContext(), R.string.foto_cargada_exito, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.foto_error_carga, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private String convertirImagenABase64(Uri uri) {
        try {
            Bitmap bitmap = decodificarBitmapReducido(uri);
            if (bitmap == null) {
                return null;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private Bitmap decodificarBitmapReducido(Uri uri) {
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream boundsStream = requireContext().getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
            }

            int sampleSize = 1;
            int maxDimension = Math.max(boundsOptions.outWidth, boundsOptions.outHeight);
            while (maxDimension / sampleSize > 1280) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = Math.max(1, sampleSize);

            try (InputStream decodeStream = requireContext().getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
            }
        } catch (Exception ignored) {
            return null;
        }
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

    private static class RegistroInput {
        private final String nombreColumna;
        @Nullable
        private final EditText textInput;
        @Nullable
        private final FotoInput fotoInput;

        private RegistroInput(String nombreColumna, @Nullable EditText textInput, @Nullable FotoInput fotoInput) {
            this.nombreColumna = nombreColumna;
            this.textInput = textInput;
            this.fotoInput = fotoInput;
        }

        static RegistroInput desdeTexto(String nombreColumna, EditText textInput) {
            return new RegistroInput(nombreColumna, textInput, null);
        }

        String obtenerValor() {
            if (textInput != null) {
                return textInput.getText() != null ? textInput.getText().toString().trim() : "";
            }
            return fotoInput != null && fotoInput.base64 != null ? fotoInput.base64.trim() : "";
        }
    }

    private static class FotoInput {
        private final ImageView preview;
        @Nullable
        private String base64;

        private FotoInput(ImageView preview) {
            this.preview = preview;
        }
    }

}
