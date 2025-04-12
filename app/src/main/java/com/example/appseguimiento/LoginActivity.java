package com.example.appseguimiento;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.appseguimiento.workers.RemoteLoginWorker;
import com.example.appseguimiento.workers.RemoteRegisterWorker;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(view -> {
            String nombre = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (nombre.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete ambos campos", Toast.LENGTH_SHORT).show();
                return;
            }

            Data datos = new Data.Builder()
                    .putString("nombre", nombre)
                    .putString("password", password)
                    .build();

            OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(RemoteLoginWorker.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(loginRequest.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            boolean success = workInfo.getOutputData().getBoolean("success", false);

                            if (success) {
                                SharedPreferences prefs = getSharedPreferences("miAppPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("nombre", workInfo.getOutputData().getString("nombre"));
                                editor.putInt("id", workInfo.getOutputData().getInt("id", -1));
                                editor.apply();

                                Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                String msg = workInfo.getOutputData().getString("error");
                                Toast.makeText(this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            WorkManager.getInstance(this).enqueue(loginRequest);
        });

        btnRegister.setOnClickListener(view -> {
            String nombre = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (nombre.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete ambos campos", Toast.LENGTH_SHORT).show();
                return;
            }

            Data datos = new Data.Builder()
                    .putString("nombre", nombre)
                    .putString("password", password)
                    .build();

            OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(RemoteRegisterWorker.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Toast.makeText(this,
                                    "OK".equals(resultado) ? "Registro exitoso" : "Error al registrar",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

            WorkManager.getInstance(this).enqueue(registerRequest);
        });
    }
}
