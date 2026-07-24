package com.example.vigiaapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class HistorialAdminFragment extends Fragment {
    private LinearLayout historialContainer;
    private TextView tvEstadoHistorial;
    private TextView tvFiltroFecha;
    private String fechaFiltroSeleccionada;

    public HistorialAdminFragment() {
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        historialContainer = view.findViewById(R.id.historialContainer);
        tvEstadoHistorial = view.findViewById(R.id.tvEstadoHistorial);
        tvFiltroFecha = view.findViewById(R.id.tvFiltroFecha);
        Button btnFiltrarFecha = view.findViewById(R.id.btnFiltrarFecha);
        Button btnLimpiarFiltroFecha = view.findViewById(R.id.btnLimpiarFiltroFecha);
        Button btnBorrarHistorial = view.findViewById(R.id.btnBorrarHistorial);

        btnFiltrarFecha.setOnClickListener(v -> mostrarSelectorFecha());
        btnLimpiarFiltroFecha.setOnClickListener(v -> {
            fechaFiltroSeleccionada = null;
            actualizarTextoFiltro();
            cargarHistorial();
        });
        btnBorrarHistorial.setOnClickListener(v -> mostrarConfirmacionBorrarHistorial());

        actualizarTextoFiltro();
        cargarHistorial();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarHistorial();
    }

    private void cargarHistorial() {
        if (!isAdded() || historialContainer == null || tvEstadoHistorial == null) {
            return;
        }

        historialContainer.removeAllViews();
        List<StockHistoryManager.HistoryItem> items = filtrarItemsPorFecha(
                StockHistoryManager.obtenerHistorial(requireContext())
        );
        if (items.isEmpty()) {
            tvEstadoHistorial.setVisibility(View.VISIBLE);
            tvEstadoHistorial.setText(fechaFiltroSeleccionada == null
                    ? getString(R.string.historial_vacio)
                    : getString(R.string.historial_vacio_fecha));
            return;
        }

        tvEstadoHistorial.setVisibility(View.GONE);
        for (StockHistoryManager.HistoryItem item : items) {
            historialContainer.addView(crearVistaHistorial(item));
        }
    }

    private View crearVistaHistorial(StockHistoryManager.HistoryItem item) {
        LinearLayout bloque = new LinearLayout(requireContext());
        bloque.setOrientation(LinearLayout.VERTICAL);
        bloque.setPadding(0, dpToPx(10), 0, dpToPx(10));

        bloque.addView(crearLinea(getString(R.string.historial_grupo, item.grupo), true));
        bloque.addView(crearLinea(getString(R.string.historial_articulo, item.articulo), false));
        bloque.addView(crearLinea(getString(R.string.historial_tipo, item.tipo), false));
        bloque.addView(crearLinea(getString(R.string.historial_cambio, item.cambio), false));
        bloque.addView(crearLinea(getString(R.string.historial_stock_anterior, item.stockAnterior), false));
        bloque.addView(crearLinea(getString(R.string.historial_stock_nuevo, item.stockNuevo), false));
        if (item.solicitante != null && !item.solicitante.trim().isEmpty()) {
            bloque.addView(crearLinea("Solicitante: " + item.solicitante, false));
        }
        if (item.area != null && !item.area.trim().isEmpty()) {
            bloque.addView(crearLinea("Área: " + item.area, false));
        }
        if (item.motivo != null && !item.motivo.trim().isEmpty()) {
            bloque.addView(crearLinea("Motivo: " + item.motivo, false));
        }
        bloque.addView(crearLinea(getString(R.string.historial_fecha, item.fechaTexto), false));

        View separador = new View(requireContext());
        LinearLayout.LayoutParams separadorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        separadorParams.topMargin = dpToPx(8);
        separador.setLayoutParams(separadorParams);
        separador.setBackgroundColor(0xFFDDDDDD);
        bloque.addView(separador);

        return bloque;
    }

    private TextView crearLinea(String texto, boolean destacado) {
        TextView textView = new TextView(requireContext());
        textView.setText(texto);
        textView.setTextSize(destacado ? 16 : 14);
        if (destacado) {
            textView.setGravity(Gravity.START);
        }
        return textView;
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void mostrarSelectorFecha() {
        Calendar calendario = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar fechaSeleccionada = Calendar.getInstance();
                    fechaSeleccionada.set(year, month, dayOfMonth);
                    fechaFiltroSeleccionada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(fechaSeleccionada.getTime());
                    actualizarTextoFiltro();
                    cargarHistorial();
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void actualizarTextoFiltro() {
        if (tvFiltroFecha == null) {
            return;
        }

        tvFiltroFecha.setText(fechaFiltroSeleccionada == null
                ? getString(R.string.historial_filtro_todo)
                : getString(R.string.historial_filtro_fecha, fechaFiltroSeleccionada));
    }

    private List<StockHistoryManager.HistoryItem> filtrarItemsPorFecha(List<StockHistoryManager.HistoryItem> items) {
        if (fechaFiltroSeleccionada == null || fechaFiltroSeleccionada.trim().isEmpty()) {
            return items;
        }

        List<StockHistoryManager.HistoryItem> filtrados = new ArrayList<>();
        for (StockHistoryManager.HistoryItem item : items) {
            if (coincideFecha(item)) {
                filtrados.add(item);
            }
        }
        return filtrados;
    }

    private boolean coincideFecha(StockHistoryManager.HistoryItem item) {
        if (fechaFiltroSeleccionada == null) {
            return true;
        }

        if (item.timestamp > 0L) {
            String fechaItem = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(item.timestamp);
            return fechaFiltroSeleccionada.equals(fechaItem);
        }

        return item.fechaTexto != null && item.fechaTexto.startsWith(fechaFiltroSeleccionada);
    }

    private void mostrarConfirmacionBorrarHistorial() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.historial_borrar_confirmacion)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> {
                    StockHistoryManager.borrarHistorial(requireContext());
                    cargarHistorial();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
