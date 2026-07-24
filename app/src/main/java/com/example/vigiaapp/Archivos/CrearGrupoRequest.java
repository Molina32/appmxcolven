package com.example.vigiaapp.Archivos;

import java.util.List;

public class CrearGrupoRequest {
    private final String nombreGrupo;
    private final List<CampoGrupoRequest> columnas;

    public CrearGrupoRequest(String nombreGrupo, List<CampoGrupoRequest> columnas) {
        this.nombreGrupo = nombreGrupo;
        this.columnas = columnas;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public List<CampoGrupoRequest> getColumnas() {
        return columnas;
    }
}
