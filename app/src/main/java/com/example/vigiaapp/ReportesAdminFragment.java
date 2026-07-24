package com.example.vigiaapp;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.Archivos.Grupo;
import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportesAdminFragment extends Fragment {
    private ApiService apiService;
    private TextView tvEstadoReportes;
    private TextView tvContenidoReporte;
    private ProgressBar progressReportes;
    private Button btnVerReporteProductos;
    private Button btnPdfProductos;
    private Button btnVerReporteHistorial;
    private Button btnPdfHistorial;

    public ReportesAdminFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reportes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class);

        tvEstadoReportes = view.findViewById(R.id.tvEstadoReportes);
        tvContenidoReporte = view.findViewById(R.id.tvContenidoReporte);
        progressReportes = view.findViewById(R.id.progressReportes);
        btnVerReporteProductos = view.findViewById(R.id.btnVerReporteProductos);
        btnPdfProductos = view.findViewById(R.id.btnPdfProductos);
        btnVerReporteHistorial = view.findViewById(R.id.btnVerReporteHistorial);
        btnPdfHistorial = view.findViewById(R.id.btnPdfHistorial);

        btnVerReporteProductos.setOnClickListener(v -> cargarReporteProductos(false));
        btnPdfProductos.setOnClickListener(v -> cargarReporteProductos(true));
        btnVerReporteHistorial.setOnClickListener(v -> mostrarReporteHistorial(false));
        btnPdfHistorial.setOnClickListener(v -> mostrarReporteHistorial(true));
    }

    private void cargarReporteProductos(boolean exportarPdf) {
        if (apiService == null || !isAdded()) {
            return;
        }

        mostrarCargando(getString(R.string.reportes_cargando_productos));
        apiService.getGrupos().enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Grupo>> call, @NonNull Response<List<Grupo>> response) {
                if (!isAdded()) {
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    mostrarError(getString(R.string.reportes_error_productos_http, response.code()));
                    return;
                }

                List<Grupo> grupos = response.body();
                if (grupos.isEmpty()) {
                    mostrarContenido(getString(R.string.reportes_productos_vacio));
                    return;
                }

                cargarRegistrosDeGrupos(grupos, 0, new StringBuilder(), exportarPdf);
            }

            @Override
            public void onFailure(@NonNull Call<List<Grupo>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                mostrarError(getString(R.string.reportes_error_conexion, t.getMessage()));
            }
        });
    }

    private void cargarRegistrosDeGrupos(@NonNull List<Grupo> grupos,
                                         int indice,
                                         @NonNull StringBuilder contenido,
                                         boolean exportarPdf) {
        if (!isAdded()) {
            return;
        }

        if (indice >= grupos.size()) {
            String reporte = contenido.length() == 0
                    ? getString(R.string.reportes_productos_vacio)
                    : contenido.toString().trim();
            mostrarContenido(reporte);
            if (exportarPdf) {
                exportarPdf(getString(R.string.reportes_pdf_productos_nombre), reporte);
            }
            return;
        }

        Grupo grupo = grupos.get(indice);
        String identificador = obtenerIdentificadorGrupo(grupo);
        if (identificador.isEmpty()) {
            cargarRegistrosDeGrupos(grupos, indice + 1, contenido, exportarPdf);
            return;
        }

        apiService.getRegistrosGrupo(identificador).enqueue(new Callback<List<LinkedHashMap<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<LinkedHashMap<String, Object>>> call,
                                   @NonNull Response<List<LinkedHashMap<String, Object>>> response) {
                if (!isAdded()) {
                    return;
                }

                contenido.append(getString(R.string.reportes_grupo_encabezado,
                        valorSeguro(grupo.getNombreMostrado())));
                contenido.append("\n");

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<LinkedHashMap<String, Object>> registros = response.body();
                    for (int i = 0; i < registros.size(); i++) {
                        contenido.append(getString(R.string.reportes_producto_item, i + 1));
                        contenido.append("\n");
                        contenido.append(formatearRegistro(registros.get(i)));
                        contenido.append("\n\n");
                    }
                } else {
                    contenido.append(getString(R.string.reportes_grupo_sin_registros));
                    contenido.append("\n\n");
                }

                cargarRegistrosDeGrupos(grupos, indice + 1, contenido, exportarPdf);
            }

            @Override
            public void onFailure(@NonNull Call<List<LinkedHashMap<String, Object>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                contenido.append(getString(R.string.reportes_grupo_encabezado,
                        valorSeguro(grupo.getNombreMostrado())));
                contenido.append("\n");
                contenido.append(getString(R.string.reportes_error_conexion, t.getMessage()));
                contenido.append("\n\n");
                cargarRegistrosDeGrupos(grupos, indice + 1, contenido, exportarPdf);
            }
        });
    }

    private void mostrarReporteHistorial(boolean exportarPdf) {
        if (!isAdded()) {
            return;
        }

        List<StockHistoryManager.HistoryItem> items = StockHistoryManager.obtenerHistorial(requireContext());
        String reporte = construirReporteHistorial(items);
        mostrarContenido(reporte);
        if (exportarPdf) {
            exportarPdf(getString(R.string.reportes_pdf_historial_nombre), reporte);
        }
    }

    @NonNull
    private String construirReporteHistorial(@NonNull List<StockHistoryManager.HistoryItem> items) {
        if (items.isEmpty()) {
            return getString(R.string.historial_vacio);
        }

        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            StockHistoryManager.HistoryItem item = items.get(i);
            contenido.append(getString(R.string.reportes_movimiento_item, i + 1));
            contenido.append("\n");
            contenido.append(getString(R.string.historial_grupo, item.grupo)).append("\n");
            contenido.append(getString(R.string.historial_articulo, item.articulo)).append("\n");
            contenido.append(getString(R.string.historial_tipo, item.tipo)).append("\n");
            contenido.append(getString(R.string.historial_cambio, item.cambio)).append("\n");
            contenido.append(getString(R.string.historial_stock_anterior, item.stockAnterior)).append("\n");
            contenido.append(getString(R.string.historial_stock_nuevo, item.stockNuevo)).append("\n");
            contenido.append(getString(R.string.historial_fecha, item.fechaTexto)).append("\n\n");
        }
        return contenido.toString().trim();
    }

    @NonNull
    private String formatearRegistro(@NonNull Map<String, Object> registro) {
        StringBuilder resultado = new StringBuilder();
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            String clave = entry.getKey();
            if (clave == null || entry.getValue() == null) {
                continue;
            }

            String claveNormalizada = clave.trim().toLowerCase(Locale.ROOT);
            if (claveNormalizada.equals("id")
                    || claveNormalizada.equals("idregistro")
                    || claveNormalizada.equals("id_registro")
                    || esColumnaDeFoto(clave)) {
                continue;
            }

            String valor = String.valueOf(entry.getValue()).trim();
            if (valor.isEmpty()) {
                continue;
            }

            resultado.append(formatearNombreCampo(clave))
                    .append(": ")
                    .append(valor)
                    .append("\n");
        }

        if (resultado.length() == 0) {
            return getString(R.string.reportes_producto_sin_detalle);
        }
        return resultado.toString().trim();
    }

    private void exportarPdf(@NonNull String prefijoNombre, @NonNull String contenido) {
        if (!isAdded()) {
            return;
        }

        try {
            PdfGuardadoResult resultado = guardarPdfEnDescargas(prefijoNombre, contenido);
            compartirPdf(resultado.uri);
            Toast.makeText(requireContext(),
                    getString(R.string.reportes_pdf_generado, resultado.nombreArchivo),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    getString(R.string.reportes_pdf_error, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private PdfGuardadoResult guardarPdfEnDescargas(@NonNull String prefijoNombre,
                                                    @NonNull String contenido) throws Exception {
        String fecha = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = prefijoNombre + "_" + fecha + ".pdf";
        byte[] pdfBytes = construirPdfBytes(contenido);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = requireContext().getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IllegalStateException(getString(R.string.reportes_pdf_error_directorio));
            }

            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                throw new IllegalStateException(getString(R.string.reportes_pdf_error_directorio));
            }

            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();

            ContentValues finalValues = new ContentValues();
            finalValues.put(MediaStore.Downloads.IS_PENDING, 0);
            requireContext().getContentResolver().update(uri, finalValues, null, null);
            return new PdfGuardadoResult(uri, nombreArchivo);
        }

        File directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (directorio == null) {
            throw new IllegalStateException(getString(R.string.reportes_pdf_error_directorio));
        }

        if (!directorio.exists() && !directorio.mkdirs()) {
            throw new IllegalStateException(getString(R.string.reportes_pdf_error_directorio));
        }

        File archivo = new File(directorio, nombreArchivo);
        FileOutputStream outputStream = new FileOutputStream(archivo);
        outputStream.write(pdfBytes);
        outputStream.flush();
        outputStream.close();

        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                archivo
        );
        return new PdfGuardadoResult(uri, nombreArchivo);
    }

    @NonNull
    private byte[] construirPdfBytes(@NonNull String contenido) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint tituloPaint = new Paint();
        tituloPaint.setTextSize(16f);
        tituloPaint.setFakeBoldText(true);

        Paint textoPaint = new Paint();
        textoPaint.setTextSize(11f);

        String[] lineas = contenido.split("\\n");
        int pageWidth = 595;
        int pageHeight = 842;
        int left = 40;
        int top = 50;
        int y = top;
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawText(getString(R.string.reportes_pdf_titulo), left, y, tituloPaint);
        y += 30;

        for (String linea : lineas) {
            List<String> lineasAjustadas = dividirLinea(linea, 75);
            for (String lineaAjustada : lineasAjustadas) {
                if (y > pageHeight - 50) {
                    document.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = top;
                }
                canvas.drawText(lineaAjustada, left, y, textoPaint);
                y += 18;
            }
        }

        document.finishPage(page);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.writeTo(outputStream);
        outputStream.flush();
        document.close();
        byte[] pdfBytes = outputStream.toByteArray();
        outputStream.close();
        return pdfBytes;
    }

    @NonNull
    private List<String> dividirLinea(@NonNull String linea, int maximo) {
        List<String> resultado = new ArrayList<>();
        if (linea.length() <= maximo) {
            resultado.add(linea);
            return resultado;
        }

        String textoRestante = linea;
        while (textoRestante.length() > maximo) {
            int corte = textoRestante.lastIndexOf(' ', maximo);
            if (corte <= 0) {
                corte = maximo;
            }
            resultado.add(textoRestante.substring(0, corte).trim());
            textoRestante = textoRestante.substring(corte).trim();
        }

        if (!textoRestante.isEmpty()) {
            resultado.add(textoRestante);
        }
        return resultado;
    }

    private void compartirPdf(@NonNull Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.reportes_pdf_compartir)));
    }

    private void mostrarCargando(@NonNull String mensaje) {
        progressReportes.setVisibility(View.VISIBLE);
        tvEstadoReportes.setText(mensaje);
        tvContenidoReporte.setText("");
        actualizarBotones(false);
    }

    private void mostrarContenido(@NonNull String contenido) {
        progressReportes.setVisibility(View.GONE);
        tvEstadoReportes.setText(R.string.reportes_estado_listo);
        tvContenidoReporte.setText(contenido);
        actualizarBotones(true);
    }

    private void mostrarError(@NonNull String mensaje) {
        progressReportes.setVisibility(View.GONE);
        tvEstadoReportes.setText(mensaje);
        tvContenidoReporte.setText("");
        actualizarBotones(true);
    }

    private void actualizarBotones(boolean habilitados) {
        btnVerReporteProductos.setEnabled(habilitados);
        btnPdfProductos.setEnabled(habilitados);
        btnVerReporteHistorial.setEnabled(habilitados);
        btnPdfHistorial.setEnabled(habilitados);
    }

    @NonNull
    private String obtenerIdentificadorGrupo(Grupo grupo) {
        if (grupo == null) {
            return "";
        }
        if (!TextUtils.isEmpty(grupo.getNombreTabla())) {
            return grupo.getNombreTabla().trim();
        }
        return grupo.getNombreGrupo() != null ? grupo.getNombreGrupo().trim() : "";
    }

    @NonNull
    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    private boolean esColumnaDeFoto(@NonNull String nombreColumna) {
        String nombreNormalizado = nombreColumna.trim().toLowerCase(Locale.ROOT);
        return nombreNormalizado.contains("foto")
                || nombreNormalizado.contains("imagen")
                || nombreNormalizado.contains("image")
                || nombreNormalizado.contains("photo");
    }

    @NonNull
    private String formatearNombreCampo(@NonNull String nombreCampo) {
        String[] palabras = nombreCampo.trim().replace('_', ' ').split("\\s+");
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
                resultado.append(palabra.substring(1));
            }
        }
        return resultado.length() == 0 ? nombreCampo : resultado.toString();
    }

    private static final class PdfGuardadoResult {
        private final Uri uri;
        private final String nombreArchivo;

        private PdfGuardadoResult(@NonNull Uri uri, @NonNull String nombreArchivo) {
            this.uri = uri;
            this.nombreArchivo = nombreArchivo;
        }
    }
}
