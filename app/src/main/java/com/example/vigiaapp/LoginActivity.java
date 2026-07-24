package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.example.vigiaapp.Archivos.LoginRequest;
import com.example.vigiaapp.Archivos.LoginResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_IS_ADMIN = "is_admin";
    private static final String PREFS_SESSION = "vigia_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_IS_ADMIN = "is_admin";

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private Button btnRegister;
    private Group loginGroup;
    private FrameLayout fragmentContainer;
    private View authHeaderContainer;
    private TextView tvHeaderTitle;
    private ImageButton btnBackNavigation;
    private DrawerLayout drawerLayout;
    private ImageButton btnNavigationView;
    private NavigationView navigationView;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        loginGroup = findViewById(R.id.loginGroup);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        authHeaderContainer = findViewById(R.id.authHeaderContainer);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnBackNavigation = findViewById(R.id.btnBackNavigation);
        drawerLayout = findViewById(R.id.drawerLayout);
        btnNavigationView = findViewById(R.id.btnNavigationView);
        navigationView = findViewById(R.id.navigationView);

        apiService = ApiClient.getClient().create(ApiService.class);
        getSupportFragmentManager().addOnBackStackChangedListener(
                this::syncFragmentVisibility
        );
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(androidx.fragment.app.FragmentManager fm, Fragment f) {
                if (f.getId() == R.id.fragmentContainer) {
                    updateAuthenticatedHeader(f);
                }
            }
        }, false);

        btnBackNavigation.setOnClickListener(v -> getSupportFragmentManager().popBackStack());
        btnNavigationView.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

                if (validateInputs(username, password)) {
                    performLogin(username, password);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                showFragmentContainer();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new RegistroFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        if (savedInstanceState != null) {
            syncFragmentVisibility();
        } else if (isSessionActive()) {
            restoreSession();
        } else {
            showLoginForm();
        }
    }

    private boolean validateInputs(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Por favor ingrese su usuario", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Por favor ingrese su contraseÃ±a", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performLogin(String username, String password) {
        hideKeyboard();
        LoginRequest loginRequest = new LoginRequest(username, password);
        Call<LoginResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();

                    String rol = response.body().getUsuario().getNombreRol();
                    Integer idRol = response.body().getUsuario().getIdRol();
                    String usuarioRespuesta = response.body().getUsuario().getUsuario();
                    boolean isAdmin = isAdminRole(rol, idRol, usuarioRespuesta, username);
                    saveSession(isAdmin);

                    if (isAdmin) {
                        openAdminPanel();
                    } else {
                        openUserPanel();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error de conexiÃ³n: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractErrorMessage(Response<LoginResponse> response) {
        if (response.errorBody() == null) {
            return "Credenciales incorrectas";
        }

        try {
            String errorJson = response.errorBody().string();
            LoginResponse errorResponse = new Gson().fromJson(errorJson, LoginResponse.class);
            if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                return errorResponse.getMessage();
            }
        } catch (IOException ignored) {
        }

        return "Credenciales incorrectas";
    }

    private boolean isAdminRole(String rol, Integer idRol, String usuarioRespuesta, String usernameIngresado) {
        if (idRol != null && idRol == 1) {
            return true;
        }

        if (rol != null) {
            String normalizedRole = rol.trim().toUpperCase();
            if (normalizedRole.equals("ADMIN")
                    || normalizedRole.equals("ADMINISTRADOR")
                    || normalizedRole.contains("ADMIN")) {
                return true;
            }
        }

        return isAdminUsername(usuarioRespuesta) || isAdminUsername(usernameIngresado);
    }

    private boolean isAdminUsername(String username) {
        if (username == null) {
            return false;
        }

        String normalizedUsername = username.trim().toUpperCase();
        return normalizedUsername.equals("ADMIN")
                || normalizedUsername.equals("ADMINISTRADOR")
                || normalizedUsername.contains("ADMIN");
    }

    private void openAdminPanel() {
        showFragmentContainer();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new PanelAdministradorFragment())
                .commit();
    }

    private void openUserPanel() {
        showFragmentContainer();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new PanelUsuarioFragment())
                .commit();
    }

    private void showFragmentContainer() {
        hideKeyboard();
        loginGroup.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        if (!isSessionActive()) {
            authHeaderContainer.setVisibility(View.GONE);
            btnNavigationView.setVisibility(View.GONE);
            navigationView.setVisibility(View.GONE);
        }
    }

    private void showLoginForm() {
        drawerLayout.closeDrawer(GravityCompat.END);
        fragmentContainer.setVisibility(View.GONE);
        loginGroup.setVisibility(View.VISIBLE);
        authHeaderContainer.setVisibility(View.GONE);
        btnNavigationView.setVisibility(View.GONE);
        navigationView.setVisibility(View.GONE);
    }

    private void syncFragmentVisibility() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null) {
            if (!isAuthorizedForFragment(currentFragment)) {
                return;
            }
            showFragmentContainer();
            updateAuthenticatedHeader(currentFragment);
        } else {
            showLoginForm();
        }
    }

    private boolean isAuthorizedForFragment(Fragment fragment) {
        if (fragment instanceof InventarioAdminFragment && !isAdminSession()) {
            Toast.makeText(this, "Solo el administrador puede acceder a Inventario.", Toast.LENGTH_SHORT).show();
            openUserPanel();
            return false;
        }

        return true;
    }

    public void logout() {
        clearSession();
        drawerLayout.closeDrawer(GravityCompat.END);
        getSupportFragmentManager().popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(currentFragment)
                    .commitNow();
        }

        etUsername.setText("");
        etPassword.setText("");
        showLoginForm();
    }

    public void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.cerrar_sesion_confirmacion)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> logout())
                .show();
    }

    private boolean handleNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuCerrarSesion) {
            drawerLayout.closeDrawer(GravityCompat.END);
            showLogoutConfirmation();
            return true;
        }
        return false;
    }

    private void updateAuthenticatedHeader(Fragment fragment) {
        if (fragment == null || !isSessionActive()) {
            authHeaderContainer.setVisibility(View.GONE);
            btnNavigationView.setVisibility(View.GONE);
            navigationView.setVisibility(View.GONE);
            return;
        }

        authHeaderContainer.setVisibility(View.VISIBLE);
        btnBackNavigation.setVisibility(canNavigateBack() ? View.VISIBLE : View.GONE);
        btnNavigationView.setVisibility(View.VISIBLE);
        navigationView.setVisibility(View.VISIBLE);
        tvHeaderTitle.setText(getHeaderTitle(fragment));
    }

    private boolean canNavigateBack() {
        return getSupportFragmentManager().getBackStackEntryCount() > 0;
    }

    private String getHeaderTitle(Fragment fragment) {
        if (fragment instanceof PanelAdministradorFragment) {
            return getString(R.string.panel_administrador_titulo);
        }
        if (fragment instanceof PanelUsuarioFragment) {
            return getString(R.string.panel_usuario_titulo);
        }
        if (fragment instanceof MenuPrincipalFragment) {
            return getString(R.string.menu_principal_titulo);
        }
        if (fragment instanceof UsuariosRegistradosFragment) {
            return getString(R.string.usuarios_registrados_titulo);
        }
        if (fragment instanceof ReportesAdminFragment) {
            return getString(R.string.reportes_titulo);
        }
        if (fragment instanceof MovimientosAdminFragment) {
            return getString(R.string.movimientos_titulo);
        }
        if (fragment instanceof BdAdminDataFragment) {
            return getString(R.string.base_de_datos_titulo);
        }
        if (fragment instanceof HistorialAdminFragment) {
            return getString(R.string.historial_titulo);
        }
        if (fragment instanceof GrupoInventarioFragment) {
            return ((GrupoInventarioFragment) fragment).getTituloPantalla();
        }
        if (fragment instanceof GrupoInventarioAdminFragment) {
            return ((GrupoInventarioAdminFragment) fragment).getTituloPantalla();
        }
        if (fragment instanceof InventarioAdminFragment) {
            return getString(R.string.inventario_admin_titulo);
        }
        if (fragment instanceof InventarioFragment) {
            return getString(R.string.inventario_opcion);
        }
        return "";
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = fragmentContainer;
        }

        if (view != null) {
            IBinder windowToken = view.getWindowToken();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0);
            }
            view.clearFocus();
        }
    }

    private void restoreSession() {
        if (isAdminSession()) {
            openAdminPanel();
        } else {
            openUserPanel();
        }
    }

    private void saveSession(boolean isAdmin) {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply();
    }

    private boolean isSessionActive() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private boolean isAdminSession() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_ADMIN, false);
    }

    private void clearSession() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit().clear().apply();
    }
}
