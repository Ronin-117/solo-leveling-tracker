package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WorkoutViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@Composable
fun LoginScreen(
    viewModel: WorkoutViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isSignUp by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    val authError by viewModel.authError.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Label - Cyan Outline style enclosed in thin geometric border
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, NeonCyan), shape = RoundedCornerShape(2.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 4.dp)
        ) {
            Text(
                text = "SOLO LEVELING",
                color = NeonCyan,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "MINIMALIST WORKOUT LOG",
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Switch Tab row (Outlined tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = { isSignUp = false },
                modifier = Modifier
                    .weight(1f)
                    .background(if (!isSignUp) NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
                    .border(BorderStroke(1.dp, if (!isSignUp) NeonCyan else Color.Transparent), shape = RoundedCornerShape(2.dp))
                    .testTag("tab_login")
            ) {
                Text(
                    text = "LOGIN",
                    color = if (!isSignUp) NeonCyan else TextGray,
                    fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = { isSignUp = true },
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSignUp) NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
                    .border(BorderStroke(1.dp, if (isSignUp) NeonCyan else Color.Transparent), shape = RoundedCornerShape(2.dp))
                    .testTag("tab_signup")
            ) {
                Text(
                    text = "REGISTER",
                    color = if (isSignUp) NeonCyan else TextGray,
                    fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Error message panel
        AnimatedVisibility(
            visible = authError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            authError?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color.Transparent)
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "ERROR: $it",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Credentials Fields - Geometric sharp corners (2.dp)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username Icon", tint = NeonCyan) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = TextGray,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = TextGray,
                cursorColor = NeonCyan
            ),
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("username_input")
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = NeonCyan) },
            trailingIcon = {
                val icon = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(icon, contentDescription = "Toggle Password Visibility", tint = NeonCyan)
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = TextGray,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = TextGray,
                cursorColor = NeonCyan
            ),
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .testTag("password_input")
        )

        // Action Button - Strictly Outlined, No Fills, 2.dp shape
        OutlinedButton(
            onClick = {
                if (isSignUp) {
                    viewModel.register(username, password) { success ->
                        if (success) {
                            onLoginSuccess()
                        }
                    }
                } else {
                    viewModel.login(username, password) { success ->
                        if (success) {
                            onLoginSuccess()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_submit_button"),
            border = BorderStroke(1.dp, NeonCyan),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = NeonCyan
            ),
            shape = RoundedCornerShape(2.dp)
        ) {
            Text(
                text = if (isSignUp) "CREATE ACCOUNT" else "SIGN IN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Demo Credentials: user / password",
            color = TextGray.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
