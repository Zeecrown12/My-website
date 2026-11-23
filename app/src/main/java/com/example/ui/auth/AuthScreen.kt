package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.AuthManager
import com.example.ui.components.BoutiqueErrorBanner
import com.example.ui.components.ZeecrownBrandLogo
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSoftLavender
import com.example.ui.theme.ZeecrownWarmIvory
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    REGISTER,
    FORGOT_PASSWORD,
}

data class RegistrationBootstrapData(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
)

@Composable
fun AuthScreen(
    initialMode: AuthMode = AuthMode.SIGN_IN,
    canBrowseAsGuest: Boolean = true,
    guestHintBanner: String? = null,
    onContinueAsGuest: () -> Unit = {},
    onAuthSuccess: (RegistrationBootstrapData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    if (canBrowseAsGuest) {
        BackHandler(onBack = onContinueAsGuest)
    }

    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resetSentMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ZeecrownWarmIvory, ZeecrownSoftLavender.copy(alpha = 0.45f), ZeecrownPureWhite)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (canBrowseAsGuest) {
                    IconButton(
                        onClick = onContinueAsGuest,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("auth_back_to_store"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Store")
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                ZeecrownBrandLogo(compact = true)
                Spacer(Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.large),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_editorial),
                    contentDescription = "Zeecrown Store Luxury Fashion Editorial",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ZeecrownDeepPurple.copy(alpha = 0.72f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZeecrownWarmIvory.copy(alpha = 0.94f),
                    ) {
                        Text(
                            text = "LAGOS · ABUJA · WORLDWIDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeecrownDeepPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.sign_in_headline),
                        style = MaterialTheme.typography.titleLarge,
                        color = ZeecrownPureWhite,
                    )
                }
            }

            if (!guestHintBanner.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = ZeecrownLightLilac,
                    border = BorderStroke(1.dp, ZeecrownElegantSilver),
                ) {
                    Text(
                        text = guestHintBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZeecrownDeepPurple,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, ZeecrownElegantSilver),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (mode != AuthMode.FORGOT_PASSWORD) {
                        TabRow(
                            selectedTabIndex = if (mode == AuthMode.SIGN_IN) 0 else 1,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            Tab(
                                selected = mode == AuthMode.SIGN_IN,
                                onClick = {
                                    mode = AuthMode.SIGN_IN
                                    errorMessage = null
                                },
                                text = { Text("Sign In") },
                                modifier = Modifier.testTag("auth_tab_signin"),
                            )
                            Tab(
                                selected = mode == AuthMode.REGISTER,
                                onClick = {
                                    mode = AuthMode.REGISTER
                                    errorMessage = null
                                },
                                text = { Text("Create Account") },
                                modifier = Modifier.testTag("auth_tab_register"),
                            )
                        }
                    }

                    if (errorMessage != null) {
                        BoutiqueErrorBanner(message = errorMessage!!)
                    }

                    when (mode) {
                        AuthMode.SIGN_IN -> {
                            SignInForm(
                                isLoading = isLoading,
                                onForgotPasswordClick = {
                                    errorMessage = null
                                    resetSentMessage = null
                                    mode = AuthMode.FORGOT_PASSWORD
                                },
                                onSubmit = { email, password ->
                                    errorMessage = null
                                    isLoading = true
                                    scope.launch {
                                        val res = AuthManager.signInWithEmailPassword(email, password)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                onAuthSuccess(RegistrationBootstrapData())
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.localizedMessage ?: "Sign in failed."
                                            }
                                        )
                                    }
                                },
                            )
                        }

                        AuthMode.REGISTER -> {
                            RegisterForm(
                                isLoading = isLoading,
                                onSubmit = { firstName, lastName, email, phone, password ->
                                    errorMessage = null
                                    isLoading = true
                                    scope.launch {
                                        val res = AuthManager.registerWithEmailPassword(
                                            firstName = firstName,
                                            lastName = lastName,
                                            email = email,
                                            password = password,
                                        )
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                onAuthSuccess(
                                                    RegistrationBootstrapData(
                                                        firstName = firstName,
                                                        lastName = lastName,
                                                        phone = phone,
                                                    )
                                                )
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.localizedMessage ?: "Registration failed."
                                            }
                                        )
                                    }
                                },
                            )
                        }

                        AuthMode.FORGOT_PASSWORD -> {
                            ForgotPasswordForm(
                                isLoading = isLoading,
                                resetSentMessage = resetSentMessage,
                                onBackToSignIn = {
                                    errorMessage = null
                                    resetSentMessage = null
                                    mode = AuthMode.SIGN_IN
                                },
                                onSubmitReset = { email ->
                                    errorMessage = null
                                    resetSentMessage = null
                                    isLoading = true
                                    scope.launch {
                                        val res = AuthManager.sendPasswordReset(email)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = {
                                                resetSentMessage =
                                                    "If an account exists for $email, a password reset link has been dispatched. Check your inbox and spam folder."
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.localizedMessage ?: "Could not send reset email."
                                            }
                                        )
                                    }
                                },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ZeecrownElegantSilver)
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ZeecrownElegantSilver)
                    }

                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            AuthManager.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess(RegistrationBootstrapData())
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                },
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("google_sign_in_button"),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, ZeecrownRoyalPurple),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.sign_in_with_google),
                            style = MaterialTheme.typography.titleMedium,
                            color = ZeecrownRoyalPurple,
                        )
                    }

                    if (canBrowseAsGuest) {
                        TextButton(
                            onClick = onContinueAsGuest,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("continue_as_guest_button"),
                        ) {
                            Text(
                                text = "Browse Collection as Guest",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
