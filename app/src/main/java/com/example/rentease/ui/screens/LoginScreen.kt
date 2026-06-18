package com.example.rentease.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.R
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.NetworkUtils
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(R.drawable.logo_aplikasi),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Masuk ke Akun",
                style = MaterialTheme.typography.headlineMedium,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Masuk untuk melanjutkan menyewa",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TextHint)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextHint)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle",
                                    tint = TextHint
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )

                    TextButton(
                        onClick = { /* TODO: forgot password dialog */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Lupa Kata Sandi?", color = Primary, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    GlowButton(
                        text = if (isLoading) "Memproses..." else "Masuk",
                        onClick = {
                            if (!NetworkUtils.checkAndNotify(context)) return@GlowButton
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Silakan isi semua field", Toast.LENGTH_SHORT).show()
                                return@GlowButton
                            }
                            isLoading = true
                            performLogin(context, authManager, email.trim(), password, onLoginSuccess, { isLoading = false })
                        },
                        enabled = !isLoading
                    )

                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Belum punya akun?",
                color = TextLight,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Daftar Sekarang",
                color = Primary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable { onNavigateToRegister() }
                    .padding(4.dp)
            )
        }
    }
}

private fun performLogin(
    context: android.content.Context,
    authManager: FirebaseAuthManager,
    email: String,
    password: String,
    onLoginSuccess: (String) -> Unit,
    onFinishLoading: () -> Unit
) {
    if (email == "admin@gmail.com") {
        authManager.loginOrRegisterPredefinedAccount(
            email = email,
            password = password,
            role = FirebaseAuthManager.ROLE_ADMIN,
            onSuccess = { role ->
                onFinishLoading()
                onLoginSuccess(role)
            },
            onFailure = { error ->
                onFinishLoading()
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    } else if (email == "petugas@gmail.com") {
        authManager.loginOrRegisterPredefinedAccount(
            email = email,
            password = password,
            role = FirebaseAuthManager.ROLE_PETUGAS,
            onSuccess = { role ->
                onFinishLoading()
                onLoginSuccess(role)
            },
            onFailure = { error ->
                onFinishLoading()
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    } else {
        authManager.loginUser(
            email = email,
            password = password,
            onSuccess = { role ->
                onFinishLoading()
                onLoginSuccess(role)
            },
            onFailure = { error ->
                onFinishLoading()
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    }
}
