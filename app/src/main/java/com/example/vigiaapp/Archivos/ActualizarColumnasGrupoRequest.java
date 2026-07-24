package com.example.vigiaapp.Archivos;

import java.util.List;

public class ActualizarColumnasGrupoRequest {
    private final List<CampoGrupoRequest> columnas;

    public ActualizarColumnasGrupoRequest(List<CampoGrupoRequest> columnas) {
        this.columnas = columnas;
    }

    public List<CampoGrupoRequest> getColumnas() {
        return columnas;
    }
}
