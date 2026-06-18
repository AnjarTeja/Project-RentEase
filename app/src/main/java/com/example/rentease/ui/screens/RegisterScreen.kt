package com.example.rentease.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager() }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }

            Image(
                painter = painterResource(R.drawable.logo_aplikasi),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Buat Akun",
                style = MaterialTheme.typography.headlineMedium,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Daftarkan akun baru untuk mulai menyewa",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 20.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor Telepon") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextHint) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextHint)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Kata Sandi") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextHint) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextHint)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowButton(
                        text = if (isLoading) "Mendaftarkan..." else "Daftar",
                        onClick = {
                            when {
                                name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() -> {
                                    Toast.makeText(context, "Harap isi semua field", Toast.LENGTH_SHORT).show()
                                }
                                password != confirmPassword -> {
                                    Toast.makeText(context, "Kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                                }
                                password.length < 6 -> {
                                    Toast.makeText(context, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                                }
                                email.trim().equals("admin@gmail.com", ignoreCase = true) ||
                                email.trim().equals("petugas@gmail.com", ignoreCase = true) -> {
                                    Toast.makeText(context, "Email tidak tersedia untuk pendaftaran", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    isLoading = true
                                    authManager.registerUser(
                                        email = email.trim(),
                                        password = password,
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        onSuccess = {
                                            isLoading = false
                                            Toast.makeText(context, "Pendaftaran berhasil! Silakan login.", Toast.LENGTH_LONG).show()
                                            onRegisterSuccess()
                                        },
                                        onFailure = { error ->
                                            isLoading = false
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sudah punya akun? ",
                color = TextLight,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Masuk",
                color = Primary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.clickable { onNavigateToLogin() }.padding(4.dp)
            )
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
    focusedLabelColor = Primary,
    unfocusedLabelColor = TextHint,
    cursorColor = Primary,
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark
)
