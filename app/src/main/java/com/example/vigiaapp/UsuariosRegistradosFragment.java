package com.example.vigiaapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.Archivos.ActualizarUsuarioRequest;
import com.example.vigiaapp.Archivos.LoginResponse;
import com.example.vigiaapp.Archivos.RegisterRequest;
import com.example.vigiaapp.Archivos.Usuario;
import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuariosRegistradosFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private ApiService apiService;
    private TextView tvEstadoUsuarios;
    private ProgressBar progressUsuarios;
    private LinearLayout usuariosContainer;

    public UsuariosRegistradosFragment() {
    }

    public static UsuariosRegistradosFragment newInstance(String param1, String param2) {
        UsuariosRegistradosFragment fragment = new UsuariosRegistradosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_usuarios_registrados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        Button btnAgregarUsuario = view.findViewById(R.id.btnAgregarUsuario);
        tvEstadoUsuarios = view.findViewById(R.id.tvEstadoUsuarios);
        progressUsuarios = view.findViewById(R.id.progressUsuarios);
        usuariosContainer = view.findViewById(R.id.usuariosContainer);

        btnAgregarUsuario.setOnClickListener(v -> mostrarDialogoAgregarUsuario());
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        tvEstadoUsuarios.setVisibility(View.VISIBLE);
        tvEstadoUsuarios.setText(R.string.usuarios_cargando);
        progressUsuarios.setVisibility(View.VISIBLE);
        usuariosContainer.removeAllViews();

        apiService.getUsuarios().enqueue(new Callback<List<Usuario>>() {
            @Override
            public void onResponse(@NonNull Call<List<Usuario>> call, @NonNull Response<List<Usuario>> response) {
                if (!isAdded()) {
                    return;
                }

                progressUsuarios.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    mostrarUsuarios(response.body());
                } else {
                    mostrarErrorCarga();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Usuario>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                progressUsuarios.setVisibility(View.GONE);
                mostrarErrorCarga();
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarUsuarios(List<Usuario> usuarios) {
        usuariosContainer.removeAllViews();

        if (usuarios.isEmpty()) {
            tvEstadoUsuarios.setVisibility(View.VISIBLE);
            tvEstadoUsuarios.setText(R.string.usuarios_sin_resultados);
            return;
        }

        tvEstadoUsuarios.setVisibility(View.GONE);

        for (Usuario usuario : usuarios) {
            usuariosContainer.addView(crearVistaUsuario(usuario));
        }
    }

    private void mostrarErrorCarga() {
        tvEstadoUsuarios.setVisibility(View.VISIBLE);
        tvEstadoUsuarios.setText(R.string.usuarios_error_carga);
    }

    private View crearVistaUsuario(Usuario usuario) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dpToPx(4), 0, dpToPx(4));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(4);
        item.setLayoutParams(params);

        TextView tvUsuario = crearTextoCard("Usuario: " + valorSeguro(usuario.getUsuario()));
        TextView tvNombre = crearTextoCard(
                "Nombre: " + valorSeguro(usuario.getNombre()) + " "
                        + valorSeguro(usuario.getApellidoPaterno()) + " "
                        + valorSeguro(usuario.getApellidoMaterno())
        );
        TextView tvCorreo = crearTextoCard("Correo: " + valorSeguro(usuario.getCorreoElectronico()));
        TextView tvRol = crearTextoCard("Rol: " + valorSeguro(usuario.getNombreRol()));
        LinearLayout accionesLayout = crearAccionesUsuario(usuario);

        View separador = new View(requireContext());
        LinearLayout.LayoutParams separadorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        separadorParams.topMargin = dpToPx(4);
        separador.setLayoutParams(separadorParams);
        separador.setBackgroundColor(0xFFCCCCCC);

        item.addView(tvUsuario);
        item.addView(tvNombre);
        item.addView(tvCorreo);
        item.addView(tvRol);
        item.addView(accionesLayout);
        item.addView(separador);

        return item;
    }

    private LinearLayout crearAccionesUsuario(Usuario usuario) {
        LinearLayout accionesLayout = new LinearLayout(requireContext());
        accionesLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(6);
        accionesLayout.setLayoutParams(params);

        accionesLayout.addView(crearBotonEditarUsuario(usuario));
        accionesLayout.addView(crearBotonBorrarUsuario(usuario));
        return accionesLayout;
    }

    private Button crearBotonEditarUsuario(Usuario usuario) {
        Button button = new Button(requireContext());
        button.setText(R.string.editar_usuario_titulo);
        button.setAllCaps(true);
        button.setTextSize(12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(6);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> mostrarDialogoEditarUsuario(usuario));
        return button;
    }

    private Button crearBotonBorrarUsuario(Usuario usuario) {
        Button button = new Button(requireContext());
        button.setText(R.string.borrar);
        button.setAllCaps(true);
        button.setTextSize(12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(6);
        params.leftMargin = dpToPx(8);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> mostrarConfirmacionEliminarUsuario(usuario));
        return button;
    }

    private void mostrarDialogoEditarUsuario(Usuario usuario) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_usuario, null);
        EditText etUsuario = dialogView.findViewById(R.id.etUsuarioEditar);
        EditText etNombre = dialogView.findViewById(R.id.etNombreEditar);
        EditText etApellidoPaterno = dialogView.findViewById(R.id.etApellidoPaternoEditar);
        EditText etApellidoMaterno = dialogView.findViewById(R.id.etApellidoMaternoEditar);
        EditText etCorreo = dialogView.findViewById(R.id.etCorreoEditar);
        Spinner spRol = dialogView.findViewById(R.id.spRolEditar);

        String[] roles = {
                getString(R.string.rol_usuario),
                getString(R.string.rol_administrador)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRol.setAdapter(adapter);

        etUsuario.setText(valorSeguroEditable(usuario.getUsuario()));
        etNombre.setText(valorSeguroEditable(usuario.getNombre()));
        etApellidoPaterno.setText(valorSeguroEditable(usuario.getApellidoPaterno()));
        etApellidoMaterno.setText(valorSeguroEditable(usuario.getApellidoMaterno()));
        etCorreo.setText(valorSeguroEditable(usuario.getCorreoElectronico()));
        spRol.setSelection(esAdministrador(usuario) ? 1 : 0);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.editar_usuario_titulo)
                .setView(dialogView)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String usuarioEditado = texto(etUsuario);
            String nombreEditado = texto(etNombre);
            String apellidoPaternoEditado = texto(etApellidoPaterno);
            String apellidoMaternoEditado = texto(etApellidoMaterno);
            String correoEditado = texto(etCorreo);

            if (usuarioEditado.isEmpty()
                    || nombreEditado.isEmpty()
                    || apellidoPaternoEditado.isEmpty()
                    || correoEditado.isEmpty()) {
                Toast.makeText(requireContext(), R.string.usuario_datos_requeridos, Toast.LENGTH_SHORT).show();
                return;
            }

            Integer idRol = spRol.getSelectedItemPosition() == 1 ? 1 : 2;
            ActualizarUsuarioRequest request = new ActualizarUsuarioRequest(
                    usuarioEditado,
                    nombreEditado,
                    apellidoPaternoEditado,
                    apellidoMaternoEditado,
                    correoEditado,
                    idRol
            );
            actualizarUsuario(usuario, request, dialog);
        }));

        dialog.show();
    }

    private void mostrarDialogoAgregarUsuario() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_usuario, null);
        EditText etUsuario = dialogView.findViewById(R.id.etUsuarioNuevo);
        EditText etNombre = dialogView.findViewById(R.id.etNombreNuevo);
        EditText etApellidoPaterno = dialogView.findViewById(R.id.etApellidoPaternoNuevo);
        EditText etApellidoMaterno = dialogView.findViewById(R.id.etApellidoMaternoNuevo);
        EditText etCorreo = dialogView.findViewById(R.id.etCorreoNuevo);
        EditText etContrasena = dialogView.findViewById(R.id.etContrasenaNueva);
        EditText etConfirmarContrasena = dialogView.findViewById(R.id.etConfirmarContrasenaNueva);
        Spinner spRol = dialogView.findViewById(R.id.spRolNuevo);

        configurarSpinnerRoles(spRol);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.agregar_usuario_titulo)
                .setView(dialogView)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.confirmar, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String usuarioNuevo = texto(etUsuario);
            String nombreNuevo = texto(etNombre);
            String apellidoPaternoNuevo = texto(etApellidoPaterno);
            String apellidoMaternoNuevo = texto(etApellidoMaterno);
            String correoNuevo = texto(etCorreo);
            String contrasenaNueva = texto(etContrasena);
            String confirmarContrasena = texto(etConfirmarContrasena);

            if (usuarioNuevo.isEmpty()
                    || nombreNuevo.isEmpty()
                    || apellidoPaternoNuevo.isEmpty()
                    || correoNuevo.isEmpty()
                    || contrasenaNueva.isEmpty()
                    || confirmarContrasena.isEmpty()) {
                Toast.makeText(requireContext(), R.string.usuario_registro_datos_requeridos, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!contrasenaNueva.equals(confirmarContrasena)) {
                Toast.makeText(requireContext(), R.string.contrasenas_no_coinciden, Toast.LENGTH_SHORT).show();
                return;
            }

            Integer idRol = spRol.getSelectedItemPosition() == 1 ? 1 : 2;
            RegisterRequest request = new RegisterRequest(
                    usuarioNuevo,
                    nombreNuevo,
                    apellidoPaternoNuevo,
                    apellidoMaternoNuevo,
                    correoNuevo,
                    contrasenaNueva,
                    confirmarContrasena
            );
            registrarUsuario(request, idRol, dialog);
        }));

        dialog.show();
    }

    private void configurarSpinnerRoles(Spinner spinner) {
        String[] roles = {
                getString(R.string.rol_usuario),
                getString(R.string.rol_administrador)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void mostrarConfirmacionEliminarUsuario(Usuario usuario) {
        new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.usuario_eliminar_confirmacion, valorSeguro(usuario != null ? usuario.getUsuario() : null)))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> eliminarUsuario(usuario))
                .show();
    }

    private void actualizarUsuario(Usuario usuario, ActualizarUsuarioRequest request, AlertDialog dialog) {
        if (usuario == null || usuario.getId() == null) {
            Toast.makeText(requireContext(), R.string.usuario_actualizado_error, Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.actualizarUsuario(usuario.getId(), request).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(@NonNull Call<Usuario> call, @NonNull Response<Usuario> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.usuario_actualizado_exito, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cargarUsuarios();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.usuario_actualizado_error) + " HTTP " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Usuario> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registrarUsuario(RegisterRequest request, Integer idRol, AlertDialog dialog) {
        apiService.register(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (!isAdded()) {
                    return;
                }

                if (!response.isSuccessful() || response.body() == null || response.body().getUsuario() == null) {
                    Toast.makeText(requireContext(), extraerMensajeErrorRegistro(response), Toast.LENGTH_SHORT).show();
                    return;
                }

                Usuario usuarioCreado = response.body().getUsuario();
                if (idRol != null && idRol == 1 && usuarioCreado.getId() != null) {
                    ActualizarUsuarioRequest actualizarRequest = new ActualizarUsuarioRequest(
                            valorSeguroEditable(usuarioCreado.getUsuario()),
                            valorSeguroEditable(usuarioCreado.getNombre()),
                            valorSeguroEditable(usuarioCreado.getApellidoPaterno()),
                            valorSeguroEditable(usuarioCreado.getApellidoMaterno()),
                            valorSeguroEditable(usuarioCreado.getCorreoElectronico()),
                            idRol
                    );
                    actualizarUsuarioCreado(usuarioCreado, actualizarRequest, dialog);
                    return;
                }

                Toast.makeText(requireContext(), R.string.usuario_registrado_exito, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                cargarUsuarios();
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarUsuarioCreado(Usuario usuario, ActualizarUsuarioRequest request, AlertDialog dialog) {
        apiService.actualizarUsuario(usuario.getId(), request).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(@NonNull Call<Usuario> call, @NonNull Response<Usuario> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.usuario_registrado_exito, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cargarUsuarios();
                } else {
                    Toast.makeText(requireContext(), R.string.usuario_registrado_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Usuario> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            Toast.makeText(requireContext(), R.string.usuario_eliminado_error, Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.eliminarUsuario(usuario.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.usuario_eliminado_exito, Toast.LENGTH_SHORT).show();
                    cargarUsuarios();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.usuario_eliminado_error) + " HTTP " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(),
                        getString(R.string.error_conexion_detalle, t.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private TextView crearTextoCard(String texto) {
        TextView textView = new TextView(requireContext());
        textView.setText(texto);
        textView.setTextSize(15);
        textView.setGravity(Gravity.START);
        return textView;
    }

    private boolean esAdministrador(Usuario usuario) {
        if (usuario == null) {
            return false;
        }

        Integer idRol = usuario.getIdRol();
        String nombreRol = usuario.getNombreRol();
        return (idRol != null && idRol == 1)
                || (nombreRol != null && nombreRol.toUpperCase().contains("ADMIN"));
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    private String valorSeguroEditable(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String texto(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String extraerMensajeErrorRegistro(Response<LoginResponse> response) {
        if (response.errorBody() == null) {
            return getString(R.string.usuario_registrado_error);
        }

        try {
            String errorBody = response.errorBody().string();
            if (errorBody != null && errorBody.contains("\"message\"")) {
                int inicio = errorBody.indexOf("\"message\"");
                int separador = errorBody.indexOf(':', inicio);
                int primeraComilla = errorBody.indexOf('"', separador + 1);
                int segundaComilla = errorBody.indexOf('"', primeraComilla + 1);
                if (primeraComilla >= 0 && segundaComilla > primeraComilla) {
                    return errorBody.substring(primeraComilla + 1, segundaComilla);
                }
            }
        } catch (IOException ignored) {
        }

        return getString(R.string.usuario_registrado_error);
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
