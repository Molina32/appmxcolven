package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private EditText etScannerInput;
    private ApiService apiService;
    private final ArrayList<LinkedHashMap<String, Object>> registrosGuardados = new ArrayList<>();
    private final Handler scannerHandler = new Handler(Looper.getMainLooper());
    private Runnable scannerRunnable;
    private boolean procesandoScanner;

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
        etScannerInput = view.findViewById(R.id.etScannerInput);
        apiService = ApiClient.getClient().create(ApiService.class);

        tvGrupoSeleccionado.setText(getString(R.string.grupo_seleccionado_titulo, valorSeguro(nombreGrupo)));
        tvContenidoGrupo.setText(R.string.registros_grupo_vacio);
        configurarCapturaScanner();
        cargarRegistrosDesdeApi();
    }

    @Override
    public void onResume() {
        super.onResume();
        enfocarScanner();
    }

    private void configurarCapturaScanner() {
        if (etScannerInput == null) return;
        etScannerInput.setFocusable(true);
        etScannerInput.setFocusableInTouchMode(true);
        etScannerInput.setCursorVisible(false);
        etScannerInput.setLongClickable(false);
        etScannerInput.setTextIsSelectable(false);
        etScannerInput.setInputType(InputType.TYPE_NULL);
        etScannerInput.setShowSoftInputOnFocus(false);
        etScannerInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_NULL) {
                String codigo = v.getText() != null ? v.getText().toString() : "";
                limpiarScannerInput();
                enfocarScanner();
                procesarCodigoEscaneado(limpiarCodigoEscaneado(codigo));
                return true;
            }
            return false;
        });
        etScannerInput.setOnKeyListener((v, keyCode, event) -> {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB)) {
                String codigo = etScannerInput.getText() != null ? etScannerInput.getText().toString() : "";
                limpiarScannerInput();
                enfocarScanner();
                procesarCodigoEscaneado(limpiarCodigoEscaneado(codigo));
                return true;
            }
            return false;
        });
        etScannerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (procesandoScanner) return;
                String texto = s != null ? s.toString() : "";
                if (texto.contains("\n") || texto.contains("\r")) {
                    String codigo = limpiarCodigoEscaneado(texto);
                    limpiarScannerInput();
                    enfocarScanner();
                    procesarCodigoEscaneado(codigo);
                    return;
                }
                if (scannerRunnable != null) scannerHandler.removeCallbacks(scannerRunnable);
                scannerRunnable = () -> {
                    if (!isAdded() || etScannerInput == null || procesandoScanner) return;
                    String codigo = etScannerInput.getText() != null ? etScannerInput.getText().toString() : "";
                    codigo = limpiarCodigoEscaneado(codigo);
                    if (codigo.isEmpty()) return;
                    limpiarScannerInput();
                    enfocarScanner();
                    procesarCodigoEscaneado(codigo);
                };
                scannerHandler.postDelayed(scannerRunnable, 250);
            }
        });
        enfocarScanner();
    }

    private void enfocarScanner() {
        if (etScannerInput == null) return;
        etScannerInput.post(() -> {
            if (!isAdded() || etScannerInput == null) return;
            etScannerInput.requestFocus();
            etScannerInput.setSelection(etScannerInput.getText() != null ? etScannerInput.getText().length() : 0);
            ocultarTeclado(etScannerInput);
        });
    }

    private void ocultarTeclado(@Nullable View tokenView) {
        if (!isAdded() || tokenView == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
    }

    private void limpiarScannerInput() {
        if (etScannerInput == null) return;
        procesandoScanner = true;
        etScannerInput.setText("");
        procesandoScanner = false;
    }

    @NonNull
    private String limpiarCodigoEscaneado(@Nullable String codigo) {
        if (codigo == null) return "";
        String limpio = codigo.replace("\r", "").replace("\n", "").trim();
        if (limpio.contains("\t")) limpio = limpio.replace("\t", "").trim();
        return limpio;
    }

    private void procesarCodigoEscaneado(@Nullable String codigo) {
        if (registrosGuardados.isEmpty()) {
            Toast.makeText(requireContext(), "Cargando registros...", Toast.LENGTH_SHORT).show();
            return;
        }
        String codigoLimpio = limpiarCodigoEscaneado(codigo);
        if (codigoLimpio.isEmpty()) {
            Toast.makeText(requireContext(), "Código vacío.", Toast.LENGTH_SHORT).show();
            return;
        }
        LinkedHashMap<String, Object> registro = buscarRegistroPorCodigo(extraerCandidatosCodigo(codigoLimpio));
        if (registro == null) {
            Toast.makeText(requireContext(), "Producto no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }
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
        Bitmap imagen = obtenerImagenRegistro(registro);
        View carta = crearCartaProducto(imagen, registro, columnaStock, stockActual);
        String articuloHistorial = construirDescripcionProducto(registro);
        int stockAnterior = stockActual;
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(carta)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton("Entrada", (d, w) -> actualizarStockEnApi(registroId, 1, stockAnterior, columnaStock, articuloHistorial))
                .setNeutralButton("Salida", (d, w) -> {
                    if (stockActual <= 0) {
                        Toast.makeText(requireContext(), R.string.stock_no_disponible, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    actualizarStockEnApi(registroId, -1, stockAnterior, columnaStock, articuloHistorial);
                })
                .create();
        dialog.setOnShowListener(d -> {
            Button bPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button bNeg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button bNeu = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (bPos != null) { bPos.setFocusable(false); bPos.setFocusableInTouchMode(false); }
            if (bNeg != null) { bNeg.setFocusable(false); bNeg.setFocusableInTouchMode(false); }
            if (bNeu != null) { bNeu.setFocusable(false); bNeu.setFocusableInTouchMode(false); }
        });
        dialog.setOnDismissListener(d -> enfocarScanner());
        dialog.show();
    }

    private View crearCartaProducto(Bitmap imagen, Map<String, Object> registro, String columnaStock, int stockActual) {
        String titulo = construirDescripcionProducto(registro);
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(false);
        card.setCardElevation(0f);
        card.setRadius(0f);
        card.setCardBackgroundColor(0xFFFFFFFF);
        ViewGroup.MarginLayoutParams cardParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = dpToPx(10);
        cardParams.setMargins(m, m, m, 0);
        card.setLayoutParams(cardParams);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        card.addView(root);

        ImageView iv = new ImageView(requireContext());
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(180));
        iv.setLayoutParams(ivParams);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(0xFFEFEFEF);
        iv.setAdjustViewBounds(true);
        iv.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        if (imagen != null) iv.setImageBitmap(imagen);
        else iv.setImageResource(android.R.drawable.ic_menu_report_image);
        root.addView(iv);

        LinearLayout textos = new LinearLayout(requireContext());
        textos.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textosParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int p = dpToPx(14);
        textosParams.setMargins(p, p, p, p);
        textos.setLayoutParams(textosParams);
        root.addView(textos);

        TextView tvTitulo = new TextView(requireContext());
        tvTitulo.setText(!TextUtils.isEmpty(titulo) ? titulo : "Producto");
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitulo.setTextSize(18);
        textos.addView(tvTitulo);

        if (registro != null) {
            String stockKeyNorm = normalizarIdentificadorLocal(columnaStock);
            for (Map.Entry<String, Object> entry : registro.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String claveRaw = entry.getKey();
                String claveNorm = normalizarIdentificadorLocal(claveRaw);
                if (claveNorm.equals("id") || claveNorm.equals("idregistro") || claveNorm.equals("id_registro")) continue;
                if (esColumnaDeFoto(claveRaw)) continue;
                if (!stockKeyNorm.isEmpty() && claveNorm.equals(stockKeyNorm)) continue;
                String valor = String.valueOf(entry.getValue()).trim();
                if (valor.isEmpty() || valor.equalsIgnoreCase("null")) continue;
                TextView tvLinea = new TextView(requireContext());
                tvLinea.setText(formatearNombreColumna(claveRaw) + ": " + valor);
                tvLinea.setTextSize(14);
                textos.addView(tvLinea);
            }
        }

        TextView tvStock = new TextView(requireContext());
        tvStock.setText("Stock actual: " + stockActual);
        tvStock.setTextSize(14);
        textos.addView(tvStock);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.addView(card);
        return scrollView;
    }

    private void actualizarStockEnApi(long registroId, int delta, int stockAnterior, String columnaStock, String articuloHistorial) {
        if (apiService == null) return;
        Long usuarioId = obtenerUsuarioIdSesion();
        if (usuarioId == null) {
            resolverYGuardarUsuarioId(() -> ejecutarActualizarStockEnApi(registroId, delta));
            return;
        }
        ejecutarActualizarStockEnApi(registroId, delta);
    }

    private void ejecutarActualizarStockEnApi(long registroId, int delta) {
        if (apiService == null) return;
        Long usuarioId = obtenerUsuarioIdSesion();
        Call<LinkedHashMap<String, Object>> call = delta > 0
                ? apiService.aumentarStockRegistro(obtenerIdentificadorGrupo(), registroId, usuarioId)
                : apiService.reducirStockRegistro(obtenerIdentificadorGrupo(), registroId, usuarioId);
        call.enqueue(new Callback<LinkedHashMap<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Response<LinkedHashMap<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.stock_actualizado_exito, Toast.LENGTH_SHORT).show();
                    cargarRegistrosDesdeApi();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.stock_actualizado_error_http, response.code()), Toast.LENGTH_LONG).show();
                }
                enfocarScanner();
            }

            @Override
            public void onFailure(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
                enfocarScanner();
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

    private String obtenerUsuarioSesion() {
        try {
            SharedPreferences preferences = requireContext().getSharedPreferences("vigia_session", Context.MODE_PRIVATE);
            String username = preferences.getString("username", "");
            return username != null ? username.trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private Long obtenerUsuarioIdSesion() {
        try {
            SharedPreferences preferences = requireContext().getSharedPreferences("vigia_session", Context.MODE_PRIVATE);
            long id = preferences.getLong("user_id", -1L);
            return id > 0L ? id : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void guardarUsuarioIdSesion(Long userId) {
        try {
            SharedPreferences preferences = requireContext().getSharedPreferences("vigia_session", Context.MODE_PRIVATE);
            preferences.edit().putLong("user_id", userId != null ? userId : -1L).apply();
        } catch (Exception ignored) {
        }
    }

    private void resolverYGuardarUsuarioId(Runnable onComplete) {
        if (apiService == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        String username = obtenerUsuarioSesion();
        if (username == null || username.trim().isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        apiService.getUsuarios().enqueue(new Callback<List<com.example.vigiaapp.Archivos.Usuario>>() {
            @Override
            public void onResponse(@NonNull Call<List<com.example.vigiaapp.Archivos.Usuario>> call,
                                   @NonNull Response<List<com.example.vigiaapp.Archivos.Usuario>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (onComplete != null) onComplete.run();
                    return;
                }
                Long encontrado = null;
                for (com.example.vigiaapp.Archivos.Usuario u : response.body()) {
                    if (u == null || u.getUsuario() == null) continue;
                    if (u.getUsuario().trim().equalsIgnoreCase(username.trim())) {
                        encontrado = u.getId();
                        break;
                    }
                }
                if (encontrado != null) guardarUsuarioIdSesion(encontrado);
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(@NonNull Call<List<com.example.vigiaapp.Archivos.Usuario>> call, @NonNull Throwable t) {
                if (onComplete != null) onComplete.run();
            }
        });
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

    private String normalizarIdentificadorLocal(String valor) {
        if (valor == null) return "";
        return valor.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
    }

    @Nullable
    private LinkedHashMap<String, Object> buscarRegistroPorCodigo(@NonNull List<String> candidatos) {
        if (candidatos.isEmpty()) return null;
        for (LinkedHashMap<String, Object> registro : registrosGuardados) {
            for (Map.Entry<String, Object> entry : registro.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String clave = normalizarIdentificadorLocal(entry.getKey());
                if (!clave.contains("codigo") && !clave.contains("code") && !clave.contains("qr") && !clave.contains("barra") && !clave.contains("barcode") && !clave.contains("ean") && !clave.contains("upc") && !clave.contains("sku")) continue;
                String valor = String.valueOf(entry.getValue()).trim();
                String valorNorm = normalizarValorComparacion(valor);
                for (String candidato : candidatos) {
                    if (candidato.isEmpty()) continue;
                    if (valor.equalsIgnoreCase(candidato)) return registro;
                    String candidatoNorm = normalizarValorComparacion(candidato);
                    if (!valorNorm.isEmpty() && !candidatoNorm.isEmpty()) {
                        if (valorNorm.equals(candidatoNorm)) return registro;
                        if (valorNorm.length() >= 4 && candidatoNorm.length() >= 4) {
                            if (valorNorm.contains(candidatoNorm) || candidatoNorm.contains(valorNorm)) return registro;
                        }
                    }
                }
            }
        }
        return null;
    }

    @NonNull
    private List<String> extraerCandidatosCodigo(@NonNull String escaneo) {
        ArrayList<String> candidatos = new ArrayList<>();
        String base = limpiarCodigoEscaneado(escaneo);
        if (!base.isEmpty()) candidatos.add(base);
        String alfanumerico = base.replaceAll("[^A-Za-z0-9]+", "");
        if (!alfanumerico.isEmpty() && !alfanumerico.equalsIgnoreCase(base)) candidatos.add(alfanumerico);
        try {
            if (base.startsWith("http://") || base.startsWith("https://")) {
                Uri uri = Uri.parse(base);
                String qpCodigo = uri.getQueryParameter("codigo");
                String qpCode = uri.getQueryParameter("code");
                String qpId = uri.getQueryParameter("id");
                String last = uri.getLastPathSegment();
                if (qpCodigo != null && !qpCodigo.trim().isEmpty()) candidatos.add(qpCodigo.trim());
                if (qpCode != null && !qpCode.trim().isEmpty()) candidatos.add(qpCode.trim());
                if (qpId != null && !qpId.trim().isEmpty()) candidatos.add(qpId.trim());
                if (last != null && !last.trim().isEmpty()) candidatos.add(last.trim());
            }
        } catch (Exception ignored) {
        }
        Matcher m = Pattern.compile("(?i)(codigo|code|qr|barra|barras|barcode|ean|upc|sku)\\s*[:=]\\s*([A-Za-z0-9\\-_.]+)").matcher(base);
        while (m.find()) {
            String g = m.group(2);
            if (g != null && !g.trim().isEmpty()) candidatos.add(g.trim());
        }
        ArrayList<String> unicos = new ArrayList<>();
        for (String c : candidatos) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            boolean existe = false;
            for (String u : unicos) if (u.equalsIgnoreCase(t)) { existe = true; break; }
            if (!existe) unicos.add(t);
        }
        return unicos;
    }

    @NonNull
    private String normalizarValorComparacion(@Nullable String valor) {
        if (valor == null) return "";
        return valor.trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    @NonNull
    private String construirDescripcionProducto(@NonNull Map<String, Object> registro) {
        String codigo = "", descripcion = "";
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String clave = normalizarIdentificadorLocal(entry.getKey()), valor = String.valueOf(entry.getValue()).trim();
            if (valor.isEmpty()) continue;
            if (codigo.isEmpty() && clave.contains("codigo")) codigo = valor;
            else if (descripcion.isEmpty() && (clave.contains("descripcion") || clave.contains("nombre"))) descripcion = valor;
        }
        if (!codigo.isEmpty() && !descripcion.isEmpty()) return codigo + " - " + descripcion;
        if (!codigo.isEmpty()) return codigo;
        if (!descripcion.isEmpty()) return descripcion;
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String claveNormalizada = entry.getKey().trim().toLowerCase();
            if (claveNormalizada.equals("id") || claveNormalizada.equals("idregistro") || claveNormalizada.equals("id_registro") || esColumnaDeFoto(entry.getKey())) continue;
            String valor = String.valueOf(entry.getValue()).trim();
            if (!valor.isEmpty()) return valor;
        }
        return "Producto";
    }

    @Nullable
    private Long obtenerIdRegistro(@Nullable Map<String, Object> registro) {
        if (registro == null) return null;
        String[] claves = {"id", "idRegistro", "id_registro"};
        for (String clave : claves) {
            Object valor = registro.get(clave);
            if (valor instanceof Number) return ((Number) valor).longValue();
            if (valor != null) {
                try { return Long.valueOf(String.valueOf(valor)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    @Nullable
    private String obtenerColumnaStock(@Nullable Map<String, Object> registro) {
        String buscada = normalizarIdentificadorLocal(getString(R.string.stock_columna_predeterminada));
        if (registro == null) return null;
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null) continue;
            if (normalizarIdentificadorLocal(entry.getKey()).equals(buscada)) return entry.getKey();
        }
        return null;
    }

    @Nullable
    private Integer obtenerStockActual(@NonNull Map<String, Object> registro, @NonNull String columnaStock) {
        Object valorObj = registro.get(columnaStock);
        if (valorObj instanceof Number) return ((Number) valorObj).intValue();
        String valor = String.valueOf(valorObj != null ? valorObj : "").trim();
        if (valor.isEmpty()) return 0;
        try { return (int) Double.parseDouble(valor); }
        catch (NumberFormatException ignored) { return null; }
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
