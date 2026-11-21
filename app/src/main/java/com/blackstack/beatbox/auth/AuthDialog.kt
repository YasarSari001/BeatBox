package com.blackstack.beatbox.auth
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blackstack.beatbox.R
import com.blackstack.beatbox.auth.AuthController
import com.blackstack.beatbox.auth.FirebaseAuthRepository
import com.blackstack.beatbox.auth.GoogleSignInClient
import com.blackstack.beatbox.auth.GoogleSignInResult
import com.blackstack.beatbox.auth.findActivity
import kotlinx.coroutines.launch
import com.blackstack.beatbox.AuthProvider
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.UserProfile

/**
 * E-posta/şifre, Google ve misafir girişini bir arada sunan Aurora Glass
 * giriş ekranı.
 *
 * Firebase yapılandırılmadığında formlar hata mesajı gösterir; misafir girişi
 * çevrimdışı çalışmaya devam eder.
 */
@Composable
fun AuthDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onLoginSuccess: (UserProfile) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Giriş, 1 = Kayıt
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var googleLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context, strings.lang) { FirebaseAuthRepository(context, strings) }
    val controller = remember(repository, scope, strings.lang) { AuthController(repository, scope, strings) }
    val googleClient = remember(context, strings.lang) {
        GoogleSignInClient(context.findActivity() ?: context, strings)
    }
    val busy = controller.isLoading || googleLoading
    val configWarning = strings.get("firebase_not_configured")
    // Yapılandırma uyarısı zaten görünürken aynı metni hata olarak tekrarlama.
    val visibleError = formError ?: controller.errorMessage?.takeUnless { it == configWarning }

    val selectTab: (Int) -> Unit = { index ->
        selectedTab = index
        formError = null
        controller.clearMessages()
    }

    val startGoogleSignIn: () -> Unit = {
        if (!busy) {
            formError = null
            controller.clearMessages()
            googleLoading = true
            scope.launch {
                val result = runCatching { googleClient.signIn() }.getOrElse { error ->
                    googleLoading = false
                    controller.reportError(
                        error.localizedMessage ?: strings.get("google_sign_in_failed")
                    )
                    return@launch
                }
                googleLoading = false
                when (result) {
                    is GoogleSignInResult.Success ->
                        controller.signInWithGoogle(result.idToken, onLoginSuccess)
                    GoogleSignInResult.Cancelled -> Unit
                    is GoogleSignInResult.Failure -> controller.reportError(result.message)
                }
            }
        }
    }

    val maxCardHeight = (LocalConfiguration.current.screenHeightDp - 40).dp
    val cardShape = RoundedCornerShape(28.dp)

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = cardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        )
                    )
                ),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(modifier = Modifier.heightIn(max = maxCardHeight)) {
                    AuthHeader(
                        strings = strings,
                        dismissEnabled = !busy,
                        onDismiss = onDismiss
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(top = 18.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedTab == 0) {
                                strings.get("login_title")
                            } else {
                                strings.get("register_title")
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(14.dp))

                        AuthTabSwitch(
                            selectedTab = selectedTab,
                            loginLabel = strings.get("login"),
                            registerLabel = strings.get("register"),
                            onSelect = selectTab
                        )

                        Spacer(Modifier.height(16.dp))

                        if (!controller.isConfigured) {
                            AuthMessageSurface(
                                text = "$configWarning ${strings.get("guest_available")}",
                                isError = true
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        visibleError?.let { message ->
                            AuthMessageSurface(text = message, isError = true)
                            Spacer(Modifier.height(10.dp))
                        }

                        controller.infoMessage?.let { message ->
                            AuthMessageSurface(text = message, isError = false)
                            Spacer(Modifier.height(10.dp))
                        }

                        if (busy) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            )
                        }

                        if (selectedTab == 0) {
                            LoginForm(
                                strings = strings,
                                email = email,
                                onEmailChange = { email = it; formError = null },
                                password = password,
                                onPasswordChange = { password = it; formError = null },
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                enabled = !busy,
                                isLoading = controller.isLoading,
                                onForgotPassword = {
                                    val mail = email.trim()
                                    if (!mail.contains("@")) {
                                        formError = strings.get("err_enter_email_reset")
                                    } else {
                                        formError = null
                                        controller.sendPasswordReset(mail)
                                    }
                                },
                                onLogin = {
                                    val mail = email.trim()
                                    when {
                                        mail.isBlank() || password.isBlank() ->
                                            formError = strings.get("err_enter_email_password")
                                        !mail.contains("@") ->
                                            formError = strings.get("err_invalid_email")
                                        else -> {
                                            formError = null
                                            controller.signInWithEmail(mail, password, onLoginSuccess)
                                        }
                                    }
                                }
                            )
                        } else {
                            RegisterForm(
                                strings = strings,
                                name = name,
                                onNameChange = { name = it; formError = null },
                                email = regEmail,
                                onEmailChange = { regEmail = it; formError = null },
                                password = regPassword,
                                onPasswordChange = { regPassword = it; formError = null },
                                passwordVisible = regPasswordVisible,
                                onTogglePasswordVisible = { regPasswordVisible = !regPasswordVisible },
                                enabled = !busy,
                                isLoading = controller.isLoading,
                                onRegister = {
                                    when {
                                        name.isBlank() || regEmail.isBlank() || regPassword.isBlank() ->
                                            formError = strings.get("err_fill_fields")
                                        !regEmail.contains("@") ->
                                            formError = strings.get("err_invalid_email")
                                        regPassword.length < 6 ->
                                            formError = strings.get("err_password_min")
                                        else -> {
                                            formError = null
                                            controller.registerWithEmail(
                                                name,
                                                regEmail,
                                                regPassword,
                                                onLoginSuccess
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        GoogleAuthSection(
                            strings = strings,
                            visible = controller.isConfigured && googleClient.isAvailable,
                            loading = googleLoading,
                            enabled = !busy,
                            onClick = startGoogleSignIn
                        )

                        if (selectedTab == 0) {
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    onLoginSuccess(
                                        UserProfile(
                                            displayName = strings.get("guest_user"),
                                            isLoggedIn = true,
                                            authProvider = AuthProvider.GUEST
                                        )
                                    )
                                },
                                enabled = !busy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PersonOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(strings.get("continue_guest"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Marka bandı: aurora degrade, logo ve kelime markası. */
@Composable
private fun AuthHeader(
    strings: Strings,
    dismissEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush)
            .padding(top = 22.dp, bottom = 18.dp, start = 20.dp, end = 20.dp)
    ) {
        // Sağ üstte kapatma düğmesi; işlem sürerken gizlenir.
        if (dismissEnabled) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
                    .clickable(onClick = onDismiss)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = strings.get("close"),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                        )
                    )
                ),
                shadowElevation = 14.dp
            ) {
                Box(
                    modifier = Modifier.size(66.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beatbox_logo),
                        contentDescription = "BeatBox",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "BeatBox",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onSurface
                        )
                    ),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = strings.get("splash_tagline"),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                letterSpacing = 2.2.sp
            )
        }
    }
}

/** Giriş / Kayıt seçimi: hap biçiminde, kayan seçim göstergesi. */
@Composable
private fun AuthTabSwitch(
    selectedTab: Int,
    loginLabel: String,
    registerLabel: String,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthTab(
            label = loginLabel,
            selected = selectedTab == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        AuthTab(
            label = registerLabel,
            selected = selectedTab == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(180),
        label = "authTabContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        animationSpec = tween(180),
        label = "authTabContent"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = content,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoginForm(
    strings: Strings,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    onForgotPassword: () -> Unit,
    onLogin: () -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(strings.get("email")) },
        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(strings.get("password")) },
        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisible, enabled = enabled) {
                Icon(
                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (passwordVisible) strings.get("hide_password") else strings.get("show_password")
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onForgotPassword,
            enabled = enabled
        ) {
            Text(strings.get("forgot_password"), fontSize = 12.sp)
        }
    }

    PrimaryAuthButton(
        label = strings.get("login"),
        loading = isLoading,
        enabled = enabled,
        onClick = onLogin
    )
}

@Composable
private fun RegisterForm(
    strings: Strings,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    onRegister: () -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(strings.get("full_name")) },
        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(strings.get("email")) },
        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(strings.get("password_min")) },
        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisible, enabled = enabled) {
                Icon(
                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (passwordVisible) strings.get("hide_password") else strings.get("show_password")
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))

    PrimaryAuthButton(
        label = strings.get("register"),
        loading = isLoading,
        enabled = enabled,
        onClick = onRegister
    )

    Spacer(Modifier.height(10.dp))

    Text(
        text = strings.get("register_hint"),
        fontSize = 10.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        textAlign = TextAlign.Center
    )
}

/** Ana eylem düğmesi; yüklenirken içeride ilerleme gösterir. */
@Composable
private fun PrimaryAuthButton(
    label: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.4.sp)
        }
    }
}

@Composable
private fun GoogleAuthSection(
    strings: Strings,
    visible: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (!visible) return

    Spacer(Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        Text(
            text = strings.get("or"),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    }

    Spacer(Modifier.height(12.dp))

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = Color(0xFF4285F4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(strings.get("continue_google"), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AuthMessageSurface(text: String, isError: Boolean) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}
