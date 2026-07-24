package com.example.vigiaapp.Archivos;

import com.google.gson.annotations.SerializedName;

public class Rol {
    @SerializedName("idRol")
    private Integer idRol;
    private String nombre;

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}

