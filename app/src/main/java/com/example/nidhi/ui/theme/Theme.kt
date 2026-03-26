package com.example.nidhi.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = OnPrimary,
    primaryContainer = Green50,
    secondary = Teal400,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    outline = Outline,
    outlineVariant = OutlineVariant
)

// You can define a DarkColorScheme here if needed. 
// For now, we'll use LightColorScheme for both or just fallback.
private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = OnPrimary,
    background = Color(0xFF052E16), // matching splash background
    surface = Color(0xFF064E3B)
)

@Composable
fun NidhiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun SplashScreen(navController: NavController) {

    // ── Existing auth logic — untouched ──────────────────────────────────────
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        delay(2500)
        if (auth.currentUser != null) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    // ── Animation states ──────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")

    // Ambient blob pulse
    val blobPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue  = 1.14f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob"
    )

    // Ripple ring 1
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue  = 1.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r1s"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r1a"
    )

    // Ripple ring 2 (staggered)
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue  = 1.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r2s"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "r2a"
    )

    // Shimmer sweep on tagline
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Entrance fade + scale
    var visible by remember { mutableStateOf(false) }
    val fadeIn  by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "fadeIn"
    )
    val scaleIn by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.78f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "scaleIn"
    )
    LaunchedEffect(Unit) {
        delay(150)
        visible = true
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF052E16), // emerald-950
                        Color(0xFF064E3B), // emerald-900
                        Color(0xFF065F46)  // emerald-800
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Ambient glow blobs ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = (-80).dp, y = (-150).dp)
                .scale(blobPulse)
                .blur(90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Green500.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 100.dp, y = 160.dp)
                .scale(1f / blobPulse)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Teal400.copy(alpha = 0.38f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 110.dp, y = (-200).dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Green100.copy(alpha = 0.20f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // ── Logo + text content ───────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(fadeIn)
                .scale(scaleIn)
        ) {

            Box(contentAlignment = Alignment.Center) {

                // Ripple ring 2
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(ring2Scale)
                        .alpha(ring2Alpha)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Teal400.copy(alpha = 0.45f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Ripple ring 1
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(ring1Scale)
                        .alpha(ring1Alpha)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Green500.copy(alpha = 0.45f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Logo circle — Green500 → Teal400 → Green600
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Green500, Teal400, Green600),
                                start  = Offset(0f, 0f),
                                end    = Offset(200f, 200f)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Replace with your icon once ready:
                    // Icon(
                    //     painter = painterResource(R.drawable.ic_nidhi_logo),
                    //     contentDescription = "Nidhi",
                    //     tint = Color.White,
                    //     modifier = Modifier.size(56.dp)
                    // )
                    Text(
                        text       = "₹",
                        color      = Color.White,
                        fontSize   = 46.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // App name — theme gradient
            Text(
                text  = "Nidhi",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Green100, Teal400, Green500)
                    ),
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    textAlign     = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline — shimmer sweep
            Text(
                text  = "Your Smart Finance Companion",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.25f)
                        ),
                        start = Offset(shimmer * 700f, 0f),
                        end   = Offset(shimmer * 700f + 350f, 0f)
                    ),
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    textAlign     = TextAlign.Center
                )
            )
        }

        // ── Bottom pill badge ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .background(
                    color = Green600.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text          = "Secure · Smart · Simple",
                color         = Green100.copy(alpha = 0.70f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
        }
    }
}
