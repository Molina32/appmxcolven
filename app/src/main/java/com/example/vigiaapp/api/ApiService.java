package com.example.vigiaapp.api;

import com.example.vigiaapp.Archivos.ActualizarColumnasGrupoRequest;
import com.example.vigiaapp.Archivos.ActualizarNombreGrupoRequest;
import com.example.vigiaapp.Archivos.ActualizarUsuarioRequest;
import com.example.vigiaapp.Archivos.CrearGrupoRequest;
import com.example.vigiaapp.Archivos.Grupo;
import com.example.vigiaapp.Archivos.LoginRequest;
import com.example.vigiaapp.Archivos.LoginResponse;
import com.example.vigiaapp.Archivos.RegisterRequest;
import com.example.vigiaapp.Archivos.Usuario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface ApiService {
    // Endpoint de login (cambia el path si es diferente en tu API)
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Endpoint de registro (cambia el path si es diferente en tu API)
    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest registerRequest);

    // Endpoint para listar usuarios registrados.
    @GET("auth/usuarios")
    Call<List<Usuario>> getUsuarios();

    // Endpoint para actualizar un usuario registrado.
    @PUT("auth/usuarios/{id}")
    Call<Usuario> actualizarUsuario(@Path("id") Long id, @Body ActualizarUsuarioRequest request);

    // Endpoint para borrar un usuario registrado.
    @DELETE("auth/usuarios/{id}")
    Call<Void> eliminarUsuario(@Path("id") Long id);

    // Endpoint para listar grupos de inventario.
    @GET("inventario/grupos")
    Call<List<Grupo>> getGrupos();

    // Endpoint para crear grupo y permitir que el backend genere su tabla.
    @POST("inventario/grupos")
    Call<Grupo> crearGrupo(@Query("usuarioId") Long usuarioId, @Body CrearGrupoRequest crearGrupoRequest);

    // Endpoint para borrar un grupo y su tabla asociada.
    @DELETE("inventario/grupos/{id}")
    Call<Void> eliminarGrupo(@Path("id") Long id, @Query("usuarioId") Long usuarioId);

    // Endpoint para editar el nombre del grupo y su tabla.
    @PUT("inventario/grupos/{identificador}/nombre")
    Call<Grupo> actualizarNombreGrupo(@Path("identificador") String identificador,
                                      @Query("usuarioId") Long usuarioId,
                                      @Body ActualizarNombreGrupoRequest request);

    // Endpoint para listar registros guardados dentro de un grupo.
    @GET("inventario/grupos/{identificador}/registros")
    Call<List<LinkedHashMap<String, Object>>> getRegistrosGrupo(@Path("identificador") String identificador);

    // Endpoint para guardar un registro dentro de un grupo.
    @POST("inventario/grupos/{identificador}/registros")
    Call<Void> guardarRegistroGrupo(@Path("identificador") String identificador,
                                    @Query("usuarioId") Long usuarioId,
                                    @Body Map<String, Object> valores);

    // Endpoint para actualizar un registro dentro de un grupo.
    @PUT("inventario/grupos/{identificador}/registros/{registroId}")
    Call<Void> actualizarRegistroGrupo(@Path("identificador") String identificador,
                                       @Path("registroId") Long registroId,
                                       @Query("usuarioId") Long usuarioId,
                                       @Body Map<String, Object> valores);

    @PUT("inventario/grupos/{identificador}/registros/{registroId}/stock/aumentar")
    Call<LinkedHashMap<String, Object>> aumentarStockRegistro(@Path("identificador") String identificador,
                                                              @Path("registroId") Long registroId,
                                                              @Query("usuarioId") Long usuarioId);

    @PUT("inventario/grupos/{identificador}/registros/{registroId}/stock/reducir")
    Call<LinkedHashMap<String, Object>> reducirStockRegistro(@Path("identificador") String identificador,
                                                             @Path("registroId") Long registroId,
                                                             @Query("usuarioId") Long usuarioId);

    // Endpoint para borrar un registro dentro de un grupo.
    @DELETE("inventario/grupos/{identificador}/registros/{registroId}")
    Call<Void> eliminarRegistroGrupo(@Path("identificador") String identificador,
                                     @Path("registroId") Long registroId,
                                     @Query("usuarioId") Long usuarioId);

    // Endpoint para consultar columnas configuradas de un grupo.
    @GET("inventario/grupos/{identificador}/columnas")
    Call<List<String>> getColumnasGrupo(@Path("identificador") String identificador);

    // Endpoint para renombrar columnas existentes y agregar nuevas.
    @PUT("inventario/grupos/{identificador}/columnas")
    Call<Void> actualizarColumnasGrupo(@Path("identificador") String identificador,
                                       @Query("usuarioId") Long usuarioId,
                                       @Body ActualizarColumnasGrupoRequest request);

    @GET("inventario/historial/admin")
    Call<List<LinkedHashMap<String, Object>>> getHistorialAdmin();

    @DELETE("inventario/historial/admin")
    Call<Void> borrarHistorialAdmin();

}
