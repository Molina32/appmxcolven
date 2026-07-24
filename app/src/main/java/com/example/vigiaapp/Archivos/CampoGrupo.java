package com.example.vigiaapp.Archivos;

import com.google.gson.annotations.SerializedName;

public class CampoGrupo {
    @SerializedName(value = "nombreCampo", alternate = {"nombreColumna", "nombre", "campo"})
    private String nombreCampo;

    @SerializedName(value = "tipoDato", alternate = {"type", "tipo"})
    private String tipoDato;

    public String getNombreCampo() {
        return nombreCampo;
    }

    public void setNombreCampo(String nombreCampo) {
        this.nombreCampo = nombreCampo;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }
}