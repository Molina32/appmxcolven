package com.example.vigiaapp.Archivos;

public class ActualizarUsuarioRequest {
    private final String usuario;
    private final String nombre;
    private final String apellidoPaterno;
    private final String apellidoMaterno;
    private final String correoElectronico;
    private final Integer idRol;

    public ActualizarUsuarioRequest(String usuario,
                                    String nombre,
                                    String apellidoPaterno,
                                    String apellidoMaterno,
                                    String correoElectronico,
                                    Integer idRol) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correoElectronico = correoElectronico;
        this.idRol = idRol;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public Integer getIdRol() {
        return idRol;
    }
}
