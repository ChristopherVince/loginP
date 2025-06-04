package com.vince.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;
import java.util.HashMap;
import java.util.Map;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private EditText nameEditText;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView loginTextView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        nameEditText = findViewById(R.id.nameEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        db = FirebaseFirestore.getInstance();


        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            String name = nameEditText.getText().toString().trim();

            // Validaciones
            if (name.isEmpty()) {
                nameEditText.setError("El nombre es obligatorio");
                nameEditText.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                emailEditText.setError("El correo es obligatorio");
                emailEditText.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Correo no válido");
                emailEditText.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError("La contraseña es obligatoria");
                passwordEditText.requestFocus();
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError("Mínimo 6 caracteres");
                passwordEditText.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordEditText.setError("Las contraseñas no coinciden");
                confirmPasswordEditText.requestFocus();
                return;
            }

            // Crear usuario
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Enviar verificación ANTES de cerrar sesión
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                // Guardar datos en Firestore
                                                String uid = user.getUid();
                                                Map<String, Object> userMap = new HashMap<>();
                                                userMap.put("uid", uid);
                                                userMap.put("email", email);
                                                userMap.put("name", name);

                                                db.collection("users").document(uid)
                                                        .set(userMap)
                                                        .addOnSuccessListener(unused -> {
                                                            Toast.makeText(RegisterActivity.this,
                                                                    "Cuenta creada. Verifica tu correo electrónico.",
                                                                    Toast.LENGTH_LONG).show();
                                                            mAuth.signOut();
                                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                            finish();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(RegisterActivity.this,
                                                                    "Error al guardar datos: " + e.getMessage(),
                                                                    Toast.LENGTH_LONG).show();
                                                        });
                                            } else {
                                                Toast.makeText(RegisterActivity.this,
                                                        "No se pudo enviar el correo de verificación: " + verifyTask.getException().getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            String msg = task.getException().getMessage();
                            if (msg != null && msg.contains("email address is already in use")) {
                                emailEditText.setError("Este correo ya está registrado");
                                emailEditText.requestFocus();
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Error al registrar: " + msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        loginTextView = findViewById(R.id.loginTextView);

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });



    }
}
