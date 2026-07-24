package com.example.vigiaapp.Archivos;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Grupo {
    @SerializedName(value = "idGrupo", alternate = {"id", "grupoId"})
    private Long id;

    @SerializedName(value = "nombreGrupo", alternate = {"nombre", "grupo"})
    private String nombreGrupo;

    @SerializedName(value = "nombreTabla", alternate = {"tabla", "tableName"})
    private String nombreTabla;

    @SerializedName(value = "columnas", alternate = {"campos"})
    private List<CampoGrupo> columnas;

    @SerializedName("nombresColumnas")
    private List<String> nombresColumnas;

    public Grupo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public String getNombreTabla() {
        return nombreTabla;
    }

    public void setNombreTabla(String nombreTabla) {
        this.nombreTabla = nombreTabla;
    }

    public List<CampoGrupo> getColumnas() {
        return columnas;
    }

    public void setColumnas(List<CampoGrupo> columnas) {
        this.columnas = columnas;
    }

    public List<String> getNombresColumnas() {
        return nombresColumnas;
    }

    public void setNombresColumnas(List<String> nombresColumnas) {
        this.nombresColumnas = nombresColumnas;
    }

    public ArrayList<String> getColumnasMostradas() {
        ArrayList<String> columnasMostradas = new ArrayList<>();

        if (columnas != null) {
            for (CampoGrupo columna : columnas) {
                if (columna == null) {
                    continue;
                }

                agregarNombreFormateado(columnasMostradas, columna.getNombreCampo());
            }
        }

        if (nombresColumnas != null) {
            for (String columna : nombresColumnas) {
                agregarNombreFormateado(columnasMostradas, columna);
            }
        }

        return columnasMostradas;
    }

    private void agregarNombreFormateado(List<String> columnasMostradas, String columna) {
        if (columna == null) {
            return;
        }

        String limpia = columna.trim().replace('_', ' ');
        if (limpia.isEmpty()) {
            return;
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

        String nombreFormateado = resultado.toString();
        if (!nombreFormateado.isEmpty() && !columnasMostradas.contains(nombreFormateado)) {
            columnasMostradas.add(nombreFormateado);
        }
    }

    public String getNombreMostrado() {
        String nombreLimpio = nombreGrupo != null && !nombreGrupo.trim().isEmpty()
                ? nombreGrupo.trim()
                : (nombreTabla != null ? nombreTabla.trim() : null);

        if (nombreLimpio == null) {
            return null;
        }

        if (nombreLimpio.startsWith("inventario_")) {
            nombreLimpio = nombreLimpio.substring("inventario_".length());
        }

        nombreLimpio = nombreLimpio.replace('_', ' ').trim();
        if (nombreLimpio.isEmpty()) {
            return nombreLimpio;
        }

        String[] palabras = nombreLimpio.split("\\s+");
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
}
