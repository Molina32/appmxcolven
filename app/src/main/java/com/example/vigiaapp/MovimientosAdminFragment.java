package com.example.vigiaapp;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.Archivos.CampoGrupoRequest;
import com.example.vigiaapp.Archivos.CrearGrupoRequest;
import com.example.vigiaapp.Archivos.Grupo;
import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovimientosAdminFragment extends Fragment {

    private static final int MAX_COLUMNAS = 8;

    // Tipo de movimiento
    private enum TipoMovimiento {
        ENTRADA,
        SALIDA
    }

    // Clase para almacenar productos seleccionados
    public static class ProductoSeleccionado {
        String grupo;
        String grupoTabla;
        String articulo;
        int cantidad;
        int stockAnterior;
        LinkedHashMap<String, Object> registroOriginal;
        boolean esNuevoRegistro;
        List<CampoGrupoRequest> columnasGrupoNuevo;

        public ProductoSeleccionado(String grupo, String grupoTabla, String articulo, int cantidad, int stockAnterior, LinkedHashMap<String, Object> registroOriginal, boolean esNuevoRegistro) {
            this.grupo = grupo;
            this.grupoTabla = grupoTabla;
            this.articulo = articulo;
            this.cantidad = cantidad;
            this.stockAnterior = stockAnterior;
            this.registroOriginal = registroOriginal;
            this.esNuevoRegistro = esNuevoRegistro;
        }
    }

    private ApiService apiService;
    private List<ProductoSeleccionado> productosSeleccionados = new ArrayList<>();
    private List<ProductoSeleccionado> productosSeleccionadosEntrada = new ArrayList<>();
    private TextView tvProductosSeleccionados;
    private TextView tvProductosSeleccionadosEntrada;

    public MovimientosAdminFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movimientos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class);

        Button btnGenerarSalida = view.findViewById(R.id.btnGenerarSalida);
        btnGenerarSalida.setOnClickListener(v -> mostrarDialogoGenerarMovimiento(TipoMovimiento.SALIDA));

        Button btnGenerarEntrada = view.findViewById(R.id.btnGenerarEntrada);
        btnGenerarEntrada.setOnClickListener(v -> mostrarDialogoGenerarMovimiento(TipoMovimiento.ENTRADA));
    }

    private void mostrarDialogoGenerarMovimiento(TipoMovimiento tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        if (tipo == TipoMovimiento.SALIDA) {
            View dialogView = inflater.inflate(R.layout.dialog_generar_movimiento, null);
            builder.setView(dialogView);

            EditText etSolicitante = dialogView.findViewById(R.id.etSolicitante);
            EditText etArea = dialogView.findViewById(R.id.etArea);
            EditText etMotivo = dialogView.findViewById(R.id.etMotivo);
            Button btnEscanearCodigo = dialogView.findViewById(R.id.btnEscanearCodigo);
            Button btnBuscarArticulo = dialogView.findViewById(R.id.btnBuscarArticulo);
            Button btnVerResumen = dialogView.findViewById(R.id.btnVerResumen);
            Button btnCancelarMovimiento = dialogView.findViewById(R.id.btnCancelarMovimiento);
            Button btnConfirmarMovimiento = dialogView.findViewById(R.id.btnConfirmarMovimiento);
            tvProductosSeleccionados = dialogView.findViewById(R.id.tvProductosSeleccionados);

            actualizarTextoProductosSeleccionados(TipoMovimiento.SALIDA);

            AlertDialog dialog = builder.create();

            btnEscanearCodigo.setOnClickListener(v -> {
                // TODO: Implementar escaneo de código
                Toast.makeText(requireContext(), "Funcionalidad de escaneo pendiente", Toast.LENGTH_SHORT).show();
            });

            btnBuscarArticulo.setOnClickListener(v -> mostrarSeleccionGrupo(TipoMovimiento.SALIDA));

            btnVerResumen.setOnClickListener(v -> {
                String solicitante = etSolicitante.getText().toString().trim();
                String area = etArea.getText().toString().trim();
                String motivo = etMotivo.getText().toString().trim();

                if (productosSeleccionados.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona al menos un producto", Toast.LENGTH_SHORT).show();
                    return;
                }

                mostrarResumenMovimiento(solicitante, area, motivo, dialog, TipoMovimiento.SALIDA);
            });

            btnCancelarMovimiento.setOnClickListener(v -> {
                productosSeleccionados.clear();
                dialog.dismiss();
            });

            btnConfirmarMovimiento.setOnClickListener(v -> {
                String solicitante = etSolicitante.getText().toString().trim();
                String area = etArea.getText().toString().trim();
                String motivo = etMotivo.getText().toString().trim();

                if (productosSeleccionados.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona al menos un producto", Toast.LENGTH_SHORT).show();
                    return;
                }

                mostrarConfirmacionFinal(solicitante, area, motivo, dialog, TipoMovimiento.SALIDA);
            });

            dialog.show();
        } else {
            View dialogView = inflater.inflate(R.layout.dialog_generar_entrada, null);
            builder.setView(dialogView);

            EditText etProveedor = dialogView.findViewById(R.id.etProveedor);
            Button btnSeleccionarGrupo = dialogView.findViewById(R.id.btnSeleccionarGrupo);
            Button btnNuevoGrupo = dialogView.findViewById(R.id.btnNuevoGrupo);
            Button btnVerResumen = dialogView.findViewById(R.id.btnVerResumenEntrada);
            Button btnCancelarEntrada = dialogView.findViewById(R.id.btnCancelarEntrada);
            Button btnConfirmarEntrada = dialogView.findViewById(R.id.btnConfirmarEntrada);
            tvProductosSeleccionadosEntrada = dialogView.findViewById(R.id.tvProductosSeleccionadosEntrada);

            actualizarTextoProductosSeleccionados(TipoMovimiento.ENTRADA);

            AlertDialog dialog = builder.create();

            btnSeleccionarGrupo.setOnClickListener(v -> mostrarSeleccionGrupo(TipoMovimiento.ENTRADA));
            btnNuevoGrupo.setOnClickListener(v -> showCreateGroupDialogForEntrada(dialog));

            btnVerResumen.setOnClickListener(v -> {
                String proveedor = etProveedor.getText().toString().trim();

                if (productosSeleccionadosEntrada.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona al menos un producto", Toast.LENGTH_SHORT).show();
                    return;
                }

                mostrarResumenMovimiento(proveedor, "", "", dialog, TipoMovimiento.ENTRADA);
            });

            btnCancelarEntrada.setOnClickListener(v -> {
                productosSeleccionadosEntrada.clear();
                dialog.dismiss();
            });

            btnConfirmarEntrada.setOnClickListener(v -> {
                String proveedor = etProveedor.getText().toString().trim();

                if (productosSeleccionadosEntrada.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona al menos un producto", Toast.LENGTH_SHORT).show();
                    return;
                }

                mostrarConfirmacionFinal(proveedor, "", "", dialog, TipoMovimiento.ENTRADA);
            });

            dialog.show();
        }
    }

    private void showCreateGroupDialogForEntrada(AlertDialog mainDialog) {
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

                    crearGrupoYAgregarRegistro(nombreGrupo, columnas, dialog, mainDialog);
                }));

        dialog.show();
    }

    private void crearGrupoYAgregarRegistro(String nombreGrupo, List<CampoGrupoRequest> columnas, AlertDialog createGroupDialog, AlertDialog mainDialog) {
        apiService.crearGrupo(new CrearGrupoRequest(nombreGrupo, columnas)).enqueue(new Callback<Grupo>() {
            @Override
            public void onResponse(@NonNull Call<Grupo> call, @NonNull Response<Grupo> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Grupo grupoCreado = response.body();
                    createGroupDialog.dismiss();
                    Toast.makeText(requireContext(), getString(R.string.grupo_creado_exito, nombreGrupo), Toast.LENGTH_SHORT).show();
                    showAddRecordDialogForNewGroup(grupoCreado, columnas, mainDialog);
                } else {
                    Toast.makeText(requireContext(), getString(R.string.grupo_error_creacion), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Grupo> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddRecordDialogForNewGroup(Grupo grupo, List<CampoGrupoRequest> columnas, AlertDialog mainDialog) {
        if (getContext() == null || columnas == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int marginHorizontal = dpToPx(18);
        int paddingTop = dpToPx(8);
        container.setPadding(marginHorizontal, paddingTop, marginHorizontal, 0);

        List<EditText> inputCampos = new ArrayList<>();
        for (CampoGrupoRequest columna : columnas) {
            if (columna == null || columna.getNombreCampo() == null || esColumnaStock(columna.getNombreCampo())) {
                continue;
            }

            TextView label = new TextView(requireContext());
            label.setText(formatearNombreColumna(columna.getNombreCampo()));
            label.setTextSize(14);
            label.setPadding(0, dpToPx(8), 0, 0);
            container.addView(label);

            EditText input = new EditText(requireContext());
            input.setHint(R.string.escanea_o_escribe_texto);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dpToPx(4);
            input.setLayoutParams(params);
            input.setTag(columna.getNombreCampo());
            inputCampos.add(input);
            container.addView(input);
        }

        // Campo para la cantidad
        TextView labelCantidad = new TextView(requireContext());
        labelCantidad.setText("Cantidad");
        labelCantidad.setTextSize(14);
        labelCantidad.setPadding(0, dpToPx(12), 0, 0);
        container.addView(labelCantidad);

        EditText inputCantidad = new EditText(requireContext());
        inputCantidad.setHint("Ingresa la cantidad");
        inputCantidad.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams paramsCantidad = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        paramsCantidad.topMargin = dpToPx(4);
        inputCantidad.setLayoutParams(paramsCantidad);
        container.addView(inputCantidad);

        builder.setView(container)
                .setTitle("Agregar producto al grupo " + grupo.getNombreMostrado())
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, (dialog, which) -> {
                    int cantidad;
                    try {
                        cantidad = Integer.parseInt(inputCantidad.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (cantidad <= 0) {
                        Toast.makeText(requireContext(), "La cantidad debe ser mayor a cero", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LinkedHashMap<String, Object> registro = new LinkedHashMap<>();
                    StringBuilder articuloDesc = new StringBuilder();
                    for (EditText input : inputCampos) {
                        String nombreCampo = (String) input.getTag();
                        String valor = input.getText() != null ? input.getText().toString().trim() : "";
                        if (!valor.isEmpty()) {
                            if (articuloDesc.length() > 0) {
                                articuloDesc.append(" - ");
                            }
                            articuloDesc.append(valor);
                        }
                        registro.put(nombreCampo, valor);
                    }
                    registro.put(getString(R.string.stock_columna_predeterminada), String.valueOf(cantidad));

                    ProductoSeleccionado nuevoProducto = new ProductoSeleccionado(
                            valorSeguro(grupo.getNombreMostrado()),
                            grupo.getNombreTabla() != null ? grupo.getNombreTabla() : grupo.getNombreGrupo(),
                            articuloDesc.length() > 0 ? articuloDesc.toString() : "Producto sin nombre",
                            cantidad,
                            0,
                            registro,
                            true
                    );
                    nuevoProducto.columnasGrupoNuevo = columnas;
                    productosSeleccionadosEntrada.add(nuevoProducto);
                    actualizarTextoProductosSeleccionados(TipoMovimiento.ENTRADA);
                    dialog.dismiss();
                });

        builder.show();
    }

    private void mostrarConfirmacionFinal(String solicitante, String area, String motivo, AlertDialog dialogAnterior, TipoMovimiento tipo) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirmar_movimiento)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> {
                    dialogAnterior.dismiss();
                    registrarMovimiento(solicitante, area, motivo, tipo);
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void mostrarSeleccionGrupo(TipoMovimiento tipo) {
        apiService.getGrupos().enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Grupo>> call, @NonNull Response<List<Grupo>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Grupo> grupos = response.body();
                    if (grupos.isEmpty()) {
                        Toast.makeText(requireContext(), "No hay grupos disponibles", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Seleccionar grupo");
                    String[] nombresGrupos = new String[grupos.size()];
                    for (int i = 0; i < grupos.size(); i++) {
                        nombresGrupos[i] = valorSeguro(grupos.get(i).getNombreMostrado());
                    }

                    builder.setItems(nombresGrupos, (dialog, which) -> {
                        Grupo grupoSeleccionado = grupos.get(which);
                        mostrarSeleccionArticulo(grupoSeleccionado, tipo);
                    });
                    builder.setNegativeButton("Cancelar", null);
                    builder.show();
                } else {
                    Toast.makeText(requireContext(), "Error al cargar grupos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Grupo>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarSeleccionArticulo(Grupo grupo, TipoMovimiento tipo) {
        String identificador = grupo.getNombreTabla() != null && !grupo.getNombreTabla().trim().isEmpty() ? grupo.getNombreTabla() : grupo.getNombreGrupo();

        apiService.getRegistrosGrupo(identificador).enqueue(new Callback<List<LinkedHashMap<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<LinkedHashMap<String, Object>>> call, @NonNull Response<List<LinkedHashMap<String, Object>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<LinkedHashMap<String, Object>> registros = response.body();
                    if (registros.isEmpty()) {
                        Toast.makeText(requireContext(), "No hay artículos en este grupo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Seleccionar artículo");

                    ListView listView = new ListView(requireContext());
                    listView.setDividerHeight(dpToPx(1));
                    listView.setDivider(requireContext().getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
                    ArticuloAdapter adapter = new ArticuloAdapter(registros);
                    listView.setAdapter(adapter);
                    builder.setView(listView);

                    AlertDialog dialog = builder.create();

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        LinkedHashMap<String, Object> registroSeleccionado = registros.get(position);
                        dialog.dismiss();
                        mostrarDialogoCantidad(grupo, registroSeleccionado, tipo);
                    });

                    builder.setNegativeButton("Cancelar", null);
                    dialog.show();
                } else {
                    Toast.makeText(requireContext(), "Error al cargar artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<LinkedHashMap<String, Object>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ArticuloAdapter extends BaseAdapter {
        private List<LinkedHashMap<String, Object>> registros;

        public ArticuloAdapter(List<LinkedHashMap<String, Object>> registros) {
            this.registros = registros;
        }

        @Override
        public int getCount() {
            return registros.size();
        }

        @Override
        public Object getItem(int position) {
            return registros.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(R.layout.item_articulo_seleccion, parent, false);
            }

            ImageView imgArticulo = convertView.findViewById(R.id.imgArticulo);
            TextView txtCodigoArticulo = convertView.findViewById(R.id.txtCodigoArticulo);

            LinkedHashMap<String, Object> registro = registros.get(position);

            // Cargar imagen
            String columnaFoto = obtenerColumnaFoto(registro);
            if (columnaFoto != null) {
                Object valorFoto = registro.get(columnaFoto);
                if (valorFoto != null && !valorFoto.toString().trim().isEmpty()) {
                    Bitmap bitmap = decodificarImagenBase64(valorFoto.toString());
                    if (bitmap != null) {
                        imgArticulo.setImageBitmap(bitmap);
                    } else {
                        imgArticulo.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    imgArticulo.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                imgArticulo.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Buscar campo código
            String codigo = obtenerCodigoArticulo(registro);
            txtCodigoArticulo.setText(codigo);

            return convertView;
        }
    }

    private String obtenerColumnaFoto(LinkedHashMap<String, Object> registro) {
        for (String clave : registro.keySet()) {
            if (esColumnaDeFoto(clave)) {
                return clave;
            }
        }
        return null;
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

    private String obtenerCodigoArticulo(LinkedHashMap<String, Object> registro) {
        String codigo = "";
        String descripcion = "";

        // Primero buscar código y descripción, como en el historial
        for (String clave : registro.keySet()) {
            if (clave == null || registro.get(clave) == null) {
                continue;
            }
            String claveNormalizada = normalizarIdentificadorLocal(clave);
            String valor = String.valueOf(registro.get(clave)).trim();

            if (valor.isEmpty()) {
                continue;
            }

            if (codigo.isEmpty() && claveNormalizada.contains("codigo")) {
                codigo = valor;
            } else if (descripcion.isEmpty() && (claveNormalizada.contains("descripcion") || claveNormalizada.contains("nombre"))) {
                descripcion = valor;
            }
        }

        if (!codigo.isEmpty()) {
            return "Código: " + codigo;
        }
        if (!descripcion.isEmpty()) {
            return descripcion;
        }

        // Si no hay código ni descripción, usar la primera columna no técnica
        for (String clave : registro.keySet()) {
            if (clave == null || registro.get(clave) == null) {
                continue;
            }
            String claveNormalizada = clave.trim().toLowerCase();
            if (claveNormalizada.equals("id")
                    || claveNormalizada.equals("idregistro")
                    || claveNormalizada.equals("id_registro")
                    || esColumnaDeFoto(clave)
                    || esColumnaStock(clave)) {
                continue;
            }
            Object valor = registro.get(clave);
            if (valor != null && !valor.toString().trim().isEmpty()) {
                return valor.toString();
            }
        }
        return "Artículo sin datos";
    }

    private Bitmap decodificarImagenBase64(String base64Str) {
        try {
            if (base64Str.startsWith("data:image")) {
                base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
            }
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarDialogoCantidad(Grupo grupo, LinkedHashMap<String, Object> registro, TipoMovimiento tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ingresar cantidad");

        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (tipo == TipoMovimiento.SALIDA) {
            input.setHint("Cantidad a retirar");
        } else {
            input.setHint("Cantidad a agregar");
        }
        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String cantidadStr = input.getText().toString().trim();
            if (cantidadStr.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show();
                return;
            }

            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Cantidad inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cantidad <= 0) {
                Toast.makeText(requireContext(), "La cantidad debe ser mayor a cero", Toast.LENGTH_SHORT).show();
                return;
            }

            String columnaStock = obtenerColumnaStock(registro);
            Integer stockActual = obtenerStockActual(registro, columnaStock);
            if (tipo == TipoMovimiento.SALIDA) {
                if (stockActual == null) {
                    Toast.makeText(requireContext(), "No se puede determinar el stock del artículo", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (cantidad > stockActual) {
                    Toast.makeText(requireContext(), "No hay suficiente stock disponible", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                if (stockActual == null) {
                    stockActual = 0;
                }
            }

            String articuloDesc = construirDescripcionArticulo(registro);
            ProductoSeleccionado nuevoProducto = new ProductoSeleccionado(
                    valorSeguro(grupo.getNombreMostrado()),
                    grupo.getNombreTabla() != null ? grupo.getNombreTabla() : grupo.getNombreGrupo(),
                    articuloDesc,
                    cantidad,
                    stockActual,
                    new LinkedHashMap<>(registro),
                    false
            );

            if (tipo == TipoMovimiento.SALIDA) {
                productosSeleccionados.add(nuevoProducto);
            } else {
                productosSeleccionadosEntrada.add(nuevoProducto);
            }

            actualizarTextoProductosSeleccionados(tipo);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void actualizarTextoProductosSeleccionados(TipoMovimiento tipo) {
        List<ProductoSeleccionado> listaProductos = tipo == TipoMovimiento.SALIDA ? productosSeleccionados : productosSeleccionadosEntrada;
        TextView tv = tipo == TipoMovimiento.SALIDA ? tvProductosSeleccionados : tvProductosSeleccionadosEntrada;

        if (listaProductos.isEmpty()) {
            tv.setText("No hay productos seleccionados");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < listaProductos.size(); i++) {
                ProductoSeleccionado p = listaProductos.get(i);
                sb.append((i+1)).append(". ").append(p.articulo)
                        .append(" (").append(p.cantidad).append(" unidades)\n");
            }
            tv.setText(sb.toString().trim());
        }
    }

    private void mostrarResumenMovimiento(String solicitante, String area, String motivo, AlertDialog dialogAnterior, TipoMovimiento tipo) {
        List<ProductoSeleccionado> listaProductos = tipo == TipoMovimiento.SALIDA ? productosSeleccionados : productosSeleccionadosEntrada;

        StringBuilder resumen = new StringBuilder();
        if (tipo == TipoMovimiento.SALIDA) {
            resumen.append("Solicitante: ").append(solicitante).append("\n");
            resumen.append("Área: ").append(area).append("\n");
            resumen.append("Motivo: ").append(motivo).append("\n\n");
            resumen.append("Productos a retirar:\n");
        } else {
            resumen.append("Proveedor: ").append(solicitante).append("\n\n");
            resumen.append("Productos a agregar:\n");
        }

        for (ProductoSeleccionado p : listaProductos) {
            resumen.append("- ").append(p.articulo).append(" (").append(p.cantidad).append(")\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.resumen_movimiento)
                .setMessage(resumen.toString())
                .setPositiveButton(R.string.confirmar, (dialog, which) -> {
                    mostrarConfirmacionFinal(solicitante, area, motivo, dialogAnterior, tipo);
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void registrarMovimiento(String solicitante, String area, String motivo, TipoMovimiento tipo) {
        List<ProductoSeleccionado> listaProductos = tipo == TipoMovimiento.SALIDA ? productosSeleccionados : productosSeleccionadosEntrada;

        for (ProductoSeleccionado producto : listaProductos) {
            if (tipo == TipoMovimiento.SALIDA) {
                reducirStockProducto(producto, solicitante, area, motivo);
            } else {
                if (producto.esNuevoRegistro) {
                    // Crear el registro primero
                    guardarNuevoRegistroYRegistrarMovimiento(producto, solicitante, area, motivo);
                } else {
                    aumentarStockProducto(producto, solicitante, area, motivo);
                }
            }
        }

        Toast.makeText(requireContext(), R.string.movimiento_registrado_exito, Toast.LENGTH_LONG).show();
        listaProductos.clear();
    }

    private void guardarNuevoRegistroYRegistrarMovimiento(ProductoSeleccionado producto, String solicitante, String area, String motivo) {
        apiService.guardarRegistroGrupo(producto.grupoTabla, producto.registroOriginal).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    // Ahora registra el movimiento en StockHistoryManager para cada unidad
                    for (int i = 0; i < producto.cantidad; i++) {
                        final int stockAnterior = i;
                        final int stockNuevo = i + 1;
                        StockHistoryManager.registrarMovimiento(
                                requireContext(),
                                producto.grupo,
                                producto.articulo,
                                getString(R.string.historial_tipo_aumento),
                                1,
                                stockAnterior,
                                stockNuevo,
                                solicitante,
                                area,
                                motivo
                        );
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al guardar el nuevo registro", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.error_conexion_detalle, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reducirStockProducto(ProductoSeleccionado producto, String solicitante, String area, String motivo) {
        Long registroId = obtenerIdRegistro(producto.registroOriginal);
        if (registroId == null) {
            return;
        }

        for (int i = 0; i < producto.cantidad; i++) {
            final int delta = -1;
            final int stockAnteriorIter = producto.stockAnterior - i;
            final int stockNuevoIter = stockAnteriorIter + delta;

            apiService.reducirStockRegistro(producto.grupoTabla, registroId)
                    .enqueue(new Callback<LinkedHashMap<String, Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Response<LinkedHashMap<String, Object>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                StockHistoryManager.registrarMovimiento(
                                        requireContext(),
                                        producto.grupo,
                                        producto.articulo,
                                        getString(R.string.historial_tipo_reduccion),
                                        1,
                                        stockAnteriorIter,
                                        stockNuevoIter,
                                        solicitante,
                                        area,
                                        motivo
                                );
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Throwable t) {
                            // Ignorar errores individuales
                        }
                    });
        }
    }

    private void aumentarStockProducto(ProductoSeleccionado producto, String solicitante, String area, String motivo) {
        Long registroId = obtenerIdRegistro(producto.registroOriginal);
        if (registroId == null) {
            return;
        }

        for (int i = 0; i < producto.cantidad; i++) {
            final int delta = 1;
            final int stockAnteriorIter = producto.stockAnterior + i;
            final int stockNuevoIter = stockAnteriorIter + delta;

            apiService.aumentarStockRegistro(producto.grupoTabla, registroId)
                    .enqueue(new Callback<LinkedHashMap<String, Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Response<LinkedHashMap<String, Object>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                StockHistoryManager.registrarMovimiento(
                                        requireContext(),
                                        producto.grupo,
                                        producto.articulo,
                                        getString(R.string.historial_tipo_aumento),
                                        1,
                                        stockAnteriorIter,
                                        stockNuevoIter,
                                        solicitante,
                                        area,
                                        motivo
                                );
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<LinkedHashMap<String, Object>> call, @NonNull Throwable t) {
                            // Ignorar errores individuales
                        }
                    });
        }
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    private String construirDescripcionArticulo(LinkedHashMap<String, Object> registro) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String clave : registro.keySet()) {
            String claveNormalizada = clave.trim().toLowerCase();
            if (claveNormalizada.equals("id") || claveNormalizada.equals("idregistro") || claveNormalizada.equals("id_registro")) {
                continue;
            }
            if (esColumnaDeFoto(clave) || esColumnaStock(clave)) {
                continue;
            }
            Object valor = registro.get(clave);
            if (valor != null && !valor.toString().trim().isEmpty()) {
                if (!first) {
                    sb.append(" - ");
                }
                sb.append(formatearNombreColumna(clave)).append(": ").append(valor);
                first = false;
            }
        }
        if (sb.length() == 0) {
            return getString(R.string.historial_articulo_generico);
        }
        return sb.toString();
    }

    private String formatearNombreColumna(String nombre) {
        if (nombre == null) return "";
        String limpia = nombre.trim().replace('_', ' ');
        if (limpia.isEmpty()) return "";
        String[] palabras = limpia.split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.isEmpty()) continue;
            if (resultado.length() > 0) resultado.append(' ');
            resultado.append(Character.toUpperCase(palabra.charAt(0)));
            if (palabra.length() > 1) {
                resultado.append(palabra.substring(1).toLowerCase());
            }
        }
        return resultado.toString();
    }

    private boolean esColumnaStock(String nombreColumna) {
        if (nombreColumna == null) return false;
        String normalizado = normalizarIdentificadorLocal(nombreColumna);
        String stockNormalizado = normalizarIdentificadorLocal(getString(R.string.stock_columna_predeterminada));
        return normalizado.equals(stockNormalizado);
    }

    private boolean esColumnaDeFoto(String nombreColumna) {
        if (nombreColumna == null) return false;
        String normalizado = nombreColumna.trim().toLowerCase();
        return normalizado.contains("foto") || normalizado.contains("imagen") || normalizado.contains("image") || normalizado.contains("photo");
    }

    private String obtenerColumnaStock(LinkedHashMap<String, Object> registro) {
        for (String clave : registro.keySet()) {
            if (esColumnaStock(clave)) {
                return clave;
            }
        }
        return null;
    }

    private Integer obtenerStockActual(LinkedHashMap<String, Object> registro, String columnaStock) {
        Object valorObj = registro.get(columnaStock);
        if (valorObj instanceof Number) {
            return ((Number) valorObj).intValue();
        }
        if (valorObj instanceof String) {
            try {
                return (int) Double.parseDouble((String) valorObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long obtenerIdRegistro(LinkedHashMap<String, Object> registro) {
        Object idObj = null;
        if (registro.containsKey("id")) {
            idObj = registro.get("id");
        } else if (registro.containsKey("id_registro")) {
            idObj = registro.get("id_registro");
        } else if (registro.containsKey("idregistro")) {
            idObj = registro.get("idregistro");
        }

        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        } else if (idObj instanceof String) {
            try {
                return Long.parseLong((String) idObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
}
