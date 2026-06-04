package com.example.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
    ) {
        // Decorative background glowing accents
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentYellow.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = 400f
                ),
                radius = 400f,
                center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentYellow.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = 500f
                ),
                radius = 500f,
                center = Offset(size.width * 0.2f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Upper visual: wallet icon with decorative circle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = AccentYellow.copy(alpha = 0.15f),
                            radius = size.minDimension / 2.3f
                        )
                        drawCircle(
                            color = AccentYellow,
                            radius = size.minDimension / 2.3f,
                            style = Stroke(
                                width = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .background(AccentYellow, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet Logo",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "EXPENSE TRACKER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.4.sp,
                    color = LightText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("app_title_signin")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A modern financial scanner.\nInspect transaction SMS instantly.",
                    fontSize = 15.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Lower actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error message
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = ErrorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentYellow,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            viewModel.signInWithGoogle(context) { result ->
                                isLoading = false
                                result.onSuccess { onSignInSuccess() }
                                result.onFailure { e ->
                                    errorMessage = when {
                                        e.message?.contains("cancelled", ignoreCase = true) == true -> null
                                        else -> e.message ?: "Sign-in failed. Please try again."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentYellow,
                            contentColor = DarkBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Continue action"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Powered by Firebase Authentication",
                    fontSize = 11.sp,
                    color = MutedText.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
