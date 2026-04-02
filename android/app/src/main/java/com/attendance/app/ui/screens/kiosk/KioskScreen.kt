package com.attendance.app.ui.screens.kiosk

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val KioskDarkBg = Color(0xFF121212)
private val KioskCardBg = Color(0xFF1E1E1E)
private val KioskTextPrimary = Color(0xFFFFFFFF)
private val KioskTextSecondary = Color(0xFFB0B0B0)
private val KioskGreen = Color(0xFF4CAF50)
private val KioskRed = Color(0xFFF44336)
private val KioskAccent = Color(0xFF2196F3)

@Composable
fun KioskScreen(
    onExitKiosk: () -> Unit,
    viewModel: KioskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the input field when success clears
    LaunchedEffect(state.punchSuccess) {
        if (state.punchSuccess == null) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // PIN dialog
    if (state.showPinDialog) {
        PinDialog(
            pinInput = state.pinInput,
            errorMessage = state.errorMessage,
            onPinChanged = { viewModel.onPinInputChanged(it) },
            onConfirm = { viewModel.exitKiosk(onExitKiosk) },
            onDismiss = { viewModel.dismissPinDialog() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskDarkBg)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with lock icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { viewModel.showPinDialog() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Exit Kiosk",
                        tint = KioskTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title and date/time
            Text(
                text = "ATTENDANCE" + if (state.unitName.isNotEmpty()) " - ${state.unitName}" else "",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KioskTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${state.currentDate}  |  ${state.currentTime}",
                fontSize = 18.sp,
                color = KioskTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main content area
            AnimatedContent(
                targetState = state.punchSuccess != null,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "kiosk_content"
            ) { showingSuccess ->
                if (showingSuccess && state.punchSuccess != null) {
                    // Success screen
                    PunchSuccessContent(
                        result = state.punchSuccess!!,
                        countdownRemaining = state.countdownRemaining,
                        onTapToClear = { viewModel.clearInput() }
                    )
                } else {
                    // Input and employee match screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Code input card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = KioskCardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Enter Employee Code",
                                    fontSize = 20.sp,
                                    color = KioskTextSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = state.inputCode,
                                    onValueChange = { viewModel.onCodeChanged(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = KioskTextPrimary,
                                        letterSpacing = 4.sp
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        autoCorrectEnabled = false,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            // If employee matched, punch with suggested type
                                            if (state.matchedEmployee != null) {
                                                viewModel.punch(state.suggestedPunchType)
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = KioskAccent,
                                        unfocusedBorderColor = KioskTextSecondary.copy(alpha = 0.3f),
                                        cursorColor = KioskAccent,
                                        focusedTextColor = KioskTextPrimary,
                                        unfocusedTextColor = KioskTextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        if (state.inputCode.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.clearInput() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    tint = KioskTextSecondary
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Employee match card
                        AnimatedVisibility(
                            visible = state.matchedEmployee != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            state.matchedEmployee?.let { employee ->
                                EmployeeMatchCard(
                                    employee = employee,
                                    lastPunchType = state.lastPunchType,
                                    suggestedPunchType = state.suggestedPunchType,
                                    onPunchIn = { viewModel.punch("IN") },
                                    onPunchOut = { viewModel.punch("OUT") }
                                )
                            }
                        }

                        // No match hint
                        AnimatedVisibility(
                            visible = state.inputCode.length >= 2 && state.matchedEmployee == null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "No employee found with code \"${state.inputCode}\"",
                                fontSize = 16.sp,
                                color = KioskTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        // Error message
                        if (state.errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.errorMessage!!,
                                fontSize = 16.sp,
                                color = KioskRed,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeMatchCard(
    employee: com.attendance.app.data.local.dao.EmployeeWithDetails,
    lastPunchType: String?,
    suggestedPunchType: String,
    onPunchIn: () -> Unit,
    onPunchOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KioskCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Checkmark indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KioskGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = KioskGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Employee name
            Text(
                text = employee.name.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KioskTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Code and ID
            Text(
                text = "Code: ${employee.code}  |  ID: ${employee.empId}",
                fontSize = 16.sp,
                color = KioskTextSecondary,
                textAlign = TextAlign.Center
            )

            // Department
            if (!employee.departmentName.isNullOrEmpty()) {
                Text(
                    text = employee.departmentName,
                    fontSize = 16.sp,
                    color = KioskTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Last punch info
            if (lastPunchType != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last punch: $lastPunchType",
                    fontSize = 14.sp,
                    color = KioskTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Punch buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Punch IN button
                Button(
                    onClick = onPunchIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (suggestedPunchType == "IN")
                            KioskGreen else KioskGreen.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "PUNCH IN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Punch OUT button
                Button(
                    onClick = onPunchOut,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (suggestedPunchType == "OUT")
                            KioskRed else KioskRed.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "PUNCH OUT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PunchSuccessContent(
    result: PunchResult,
    countdownRemaining: Int,
    onTapToClear: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large animated checkmark
        Box(
            modifier = Modifier
                .size((80 * scale).dp)
                .clip(CircleShape)
                .background(
                    if (result.punchType == "IN") KioskGreen.copy(alpha = 0.2f)
                    else KioskRed.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (result.punchType == "IN") KioskGreen else KioskRed,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Employee name
        Text(
            text = result.employeeName.uppercase(),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = KioskTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Punch type and time
        Text(
            text = "Punched ${result.punchType} at ${result.time}",
            fontSize = 22.sp,
            color = if (result.punchType == "IN") KioskGreen else KioskRed,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Countdown
        Text(
            text = "Auto-clearing in $countdownRemaining...",
            fontSize = 16.sp,
            color = KioskTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tap to clear early
        OutlinedButton(
            onClick = onTapToClear,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = KioskTextSecondary
            )
        ) {
            Text("Tap to clear now", fontSize = 14.sp)
        }
    }
}

@Composable
private fun PinDialog(
    pinInput: String,
    errorMessage: String?,
    onPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Exit Kiosk Mode")
        },
        text = {
            Column {
                Text("Enter PIN to exit kiosk mode")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinChanged,
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
