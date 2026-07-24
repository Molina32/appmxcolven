package com.example.vigiaapp.Archivos;

public class CampoGrupoRequest {
    private final String nombreCampo;
    private final String tipoDato;

    public CampoGrupoRequest(String nombreCampo, String tipoDato) {
        this.nombreCampo = nombreCampo;
        this.tipoDato = tipoDato;
    }

    public String getNombreCampo() {
        return nombreCampo;
    }

    public String getTipoDato() {
        return tipoDato;
    }
}
