package com.example.vigiaapp.Archivos;

public class LoginResponse {
    private String message;
    private Usuario usuario;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}

