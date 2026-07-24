package com.example.vigiaapp.Archivos;

import com.google.gson.annotations.SerializedName;

public class Usuario {
    @SerializedName("idUsuario")
    private Long id;
    private String usuario;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    @SerializedName("correo")
    private String correoElectronico;
    private String contrasena;
    private Rol rol;

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }

    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public String getNombreRol() {
        return rol != null ? rol.getNombre() : null;
    }

    public Integer getIdRol() {
        return rol != null ? rol.getIdRol() : null;
    }
}

