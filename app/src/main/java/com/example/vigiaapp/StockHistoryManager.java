package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class StockHistoryManager {
    private static final String PREFS_NAME = "vigia_stock_history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 200;

    private StockHistoryManager() {
    }

    public static void registrarMovimiento(@NonNull Context context,
                                           @NonNull String grupo,
                                           @NonNull String articulo,
                                           @NonNull String tipo,
                                           int cambio,
                                           int stockAnterior,
                                           int stockNuevo,
                                           String solicitante,
                                           String area,
                                           String motivo) {
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONArray items = new JSONArray(preferences.getString(KEY_ITEMS, "[]"));

            JSONObject item = new JSONObject();
            item.put("grupo", grupo);
            item.put("articulo", articulo);
            item.put("tipo", tipo);
            item.put("cambio", cambio);
            item.put("stockAnterior", stockAnterior);
            item.put("stockNuevo", stockNuevo);
            item.put("solicitante", solicitante != null ? solicitante : "");
            item.put("area", area != null ? area : "");
            item.put("motivo", motivo != null ? motivo : "");
            item.put("timestamp", System.currentTimeMillis());
            item.put("fechaTexto", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date()));

            JSONArray nuevos = new JSONArray();
            nuevos.put(item);
            for (int i = 0; i < items.length() && i < MAX_ITEMS - 1; i++) {
                nuevos.put(items.getJSONObject(i));
            }

            preferences.edit().putString(KEY_ITEMS, nuevos.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // Método anterior para mantener compatibilidad
    public static void registrarMovimiento(@NonNull Context context,
                                           @NonNull String grupo,
                                           @NonNull String articulo,
                                           @NonNull String tipo,
                                           int cambio,
                                           int stockAnterior,
                                           int stockNuevo) {
        registrarMovimiento(context, grupo, articulo, tipo, cambio, stockAnterior, stockNuevo, "", "", "");
    }

    @NonNull
    public static List<HistoryItem> obtenerHistorial(@NonNull Context context) {
        List<HistoryItem> resultado = new ArrayList<>();

        try {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONArray items = new JSONArray(preferences.getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                resultado.add(new HistoryItem(
                        item.optString("grupo", "-"),
                        item.optString("articulo", "-"),
                        item.optString("tipo", "-"),
                        item.optInt("cambio", 0),
                        item.optInt("stockAnterior", 0),
                        item.optInt("stockNuevo", 0),
                        item.optString("fechaTexto", "-"),
                        item.optLong("timestamp", 0L),
                        item.optString("solicitante", ""),
                        item.optString("area", ""),
                        item.optString("motivo", "")
                ));
            }
        } catch (Exception ignored) {
        }

        return resultado;
    }

    public static void borrarHistorial(@NonNull Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            preferences.edit().putString(KEY_ITEMS, "[]").apply();
        } catch (Exception ignored) {
        }
    }

    public static final class HistoryItem {
        public final String grupo;
        public final String articulo;
        public final String tipo;
        public final int cambio;
        public final int stockAnterior;
        public final int stockNuevo;
        public final String fechaTexto;
        public final long timestamp;
        public final String solicitante;
        public final String area;
        public final String motivo;

        public HistoryItem(String grupo,
                           String articulo,
                           String tipo,
                           int cambio,
                           int stockAnterior,
                           int stockNuevo,
                           String fechaTexto,
                           long timestamp,
                           String solicitante,
                           String area,
                           String motivo) {
            this.grupo = grupo;
            this.articulo = articulo;
            this.tipo = tipo;
            this.cambio = cambio;
            this.stockAnterior = stockAnterior;
            this.stockNuevo = stockNuevo;
            this.fechaTexto = fechaTexto;
            this.timestamp = timestamp;
            this.solicitante = solicitante;
            this.area = area;
            this.motivo = motivo;
        }
    }
}
