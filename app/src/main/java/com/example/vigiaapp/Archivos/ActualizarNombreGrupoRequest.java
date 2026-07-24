package com.example.vigiaapp.Archivos;

public class ActualizarNombreGrupoRequest {
    private final String nombreGrupo;

    public ActualizarNombreGrupoRequest(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }
}
