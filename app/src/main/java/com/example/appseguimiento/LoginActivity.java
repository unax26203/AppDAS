package com.example.appseguimiento;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.appseguimiento.data.AppDatabase;
import com.example.appseguimiento.data.User;
import com.example.appseguimiento.data.UserDao;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private UserDao userDao;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        db = AppDatabase.getDatabase(this);
        userDao = db.userDao();
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = etUsername.getText().toString().trim();
                final String password = etPassword.getText().toString().trim();
                // Verificar que los campos no estén vacíos
                if(username.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Complete ambos campos", Toast.LENGTH_SHORT).show();
                    return;
                }
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        User user = userDao.login(username, password);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Verificar si el usuario existe
                                if(user != null) {
                                    Toast.makeText(LoginActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();
                                    // Ir a MainActivity
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                 // En caso de que las credenciales sean incorrectas
                                } else {
                                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = etUsername.getText().toString().trim();
                final String password = etPassword.getText().toString().trim();
                // Verificar que los campos no estén vacíos
                if(username.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Complete ambos campos", Toast.LENGTH_SHORT).show();
                    return;
                }
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Primero, verificamos si el usuario ya existe
                        User existing = userDao.getUserByUsername(username);
                        if(existing != null) {
                            runOnUiThread(new Runnable() {
                                // Mostrar un mensaje si el usuario ya existe
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "El usuario ya existe", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            User newUser = new User(username, password);
                            userDao.insertUser(newUser);
                            runOnUiThread(new Runnable() {
                                // Mostrar un mensaje si el registro fue exitoso
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}
