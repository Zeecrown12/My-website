package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.auth.AuthValidation
import com.example.data.auth.PasswordStrength
import com.example.ui.theme.ZeecrownAmberWarning
import com.example.ui.theme.ZeecrownDeepPurple
import com.example.ui.theme.ZeecrownElegantSilver
import com.example.ui.theme.ZeecrownGoldInk
import com.example.ui.theme.ZeecrownLightLilac
import com.example.ui.theme.ZeecrownPureWhite
import com.example.ui.theme.ZeecrownRoyalPurple
import com.example.ui.theme.ZeecrownSageSuccess

@Composable
fun SignInForm(
    isLoading: Boolean,
    onSubmit: (email: String, password: String) -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                fieldError = null
            },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signin_email_input"),
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                fieldError = null
            },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signin_password_input"),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("forgot_password_link"),
            ) {
                Text("Forgot Password?")
            }
        }

        if (fieldError != null) {
            Text(
                text = fieldError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = {
                val emailErr = AuthValidation.validateEmail(email)
                if (emailErr != null) {
                    fieldError = emailErr
                    return@Button
                }
                if (password.isBlank()) {
                    fieldError = "Please enter your password."
                    return@Button
                }
                onSubmit(email.trim(), password)
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .minimumInteractiveComponentSize()
                .testTag("email_signin_button"),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZeecrownRoyalPurple,
                contentColor = ZeecrownPureWhite,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ZeecrownPureWhite, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Signing In...")
            } else {
                Text("Sign In", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun RegisterForm(
    isLoading: Boolean,
    onSubmit: (firstName: String, lastName: String, email: String, phone: String, password: String) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }

    val strength = remember(password) { AuthValidation.evaluatePasswordStrength(password) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    fieldError = null
                },
                label = { Text("First Name *") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("register_first_name_input"),
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    fieldError = null
                },
                label = { Text("Last Name *") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("register_last_name_input"),
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                fieldError = null
            },
            label = { Text("Email Address *") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email_input"),
        )

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                fieldError = null
            },
            label = { Text("Phone Number (e.g., 080... or +234...) *") },
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = { Text("Supports Nigerian (080/081/070/090/+234) and international E.164 formats") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_phone_input"),
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                fieldError = null
            },
            label = { Text("Password *") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_password_input"),
        )

        if (password.isNotEmpty()) {
            PasswordStrengthMeter(strength = strength)
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                fieldError = null
            },
            label = { Text("Confirm Password *") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(
                    onClick = { confirmVisible = !confirmVisible },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (confirmVisible) "Hide confirm password" else "Show confirm password",
                    )
                }
            },
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_confirm_password_input"),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = {
                    acceptedTerms = it
                    fieldError = null
                },
                modifier = Modifier.testTag("register_terms_checkbox"),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "I accept the Zeecrown Terms of Service & Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showPolicyDialog = true },
                )
            }
        }

        if (fieldError != null) {
            Text(
                text = fieldError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = {
                val err = AuthValidation.validateName(firstName, "First name")
                    ?: AuthValidation.validateName(lastName, "Last name")
                    ?: AuthValidation.validateEmail(email)
                    ?: AuthValidation.validatePhone(phone)
                    ?: (if (!strength.meetsMinimum) "Password must be at least 8 characters and include lowercase plus uppercase or a number/symbol." else null)
                    ?: AuthValidation.validatePasswordConfirmation(password, confirmPassword)
                    ?: (if (!acceptedTerms) "Please accept the Terms & Privacy Policy to create your account." else null)

                if (err != null) {
                    fieldError = err
                    return@Button
                }
                onSubmit(
                    firstName.trim(),
                    lastName.trim(),
                    email.trim(),
                    AuthValidation.normalizePhone(phone),
                    password,
                )
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .minimumInteractiveComponentSize()
                .testTag("register_submit_button"),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZeecrownRoyalPurple,
                contentColor = ZeecrownPureWhite,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ZeecrownPureWhite, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Creating Account...")
            } else {
                Text("Create Account", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Terms & Privacy Notice") },
            text = {
                Text(
                    text = "ZEECROWN STORE uses your name, email, phone number, and saved delivery addresses solely to fulfill your orders, verify payments, and provide delivery updates. Passwords are handled securely by Firebase Authentication and are never stored in plain text.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = false }) { Text("Close") }
            },
        )
    }
}

@Composable
fun PasswordStrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val activeColor = when (strength.score) {
        0, 1 -> MaterialTheme.colorScheme.error
        2 -> ZeecrownAmberWarning
        3 -> ZeecrownGoldInk
        else -> ZeecrownSageSuccess
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = ZeecrownLightLilac,
        border = BorderStroke(1.dp, ZeecrownElegantSilver),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Password Strength", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = strength.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = activeColor,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (strength.score >= i) activeColor else ZeecrownElegantSilver)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StrengthCheckBadge("8+ chars", strength.hasMinLength)
                StrengthCheckBadge("Upper", strength.hasUppercase)
                StrengthCheckBadge("Lower", strength.hasLowercase)
                StrengthCheckBadge("Number/Symbol", strength.hasDigitOrSymbol)
            }
        }
    }
}

@Composable
private fun StrengthCheckBadge(label: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = if (met) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (met) ZeecrownSageSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (met) ZeecrownDeepPurple else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ForgotPasswordForm(
    isLoading: Boolean,
    resetSentMessage: String?,
    onSubmitReset: (email: String) -> Unit,
    onBackToSignIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var fieldError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Reset Your Password",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Enter the email address associated with your Zeecrown account and we will send password reset instructions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                fieldError = null
            },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("forgot_email_input"),
        )

        if (fieldError != null) {
            Text(
                text = fieldError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (resetSentMessage != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = ZeecrownSageSuccess.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ZeecrownSageSuccess.copy(alpha = 0.45f)),
            ) {
                Text(
                    text = resetSentMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZeecrownDeepPurple,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Button(
            onClick = {
                val err = AuthValidation.validateEmail(email)
                if (err != null) {
                    fieldError = err
                    return@Button
                }
                onSubmitReset(email.trim())
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .minimumInteractiveComponentSize()
                .testTag("send_reset_link_button"),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZeecrownRoyalPurple,
                contentColor = ZeecrownPureWhite,
            ),
        ) {
            Text(if (isLoading) "Sending..." else "Send Password Reset Link")
        }

        TextButton(
            onClick = onBackToSignIn,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Back to Sign In")
        }
    }
}
