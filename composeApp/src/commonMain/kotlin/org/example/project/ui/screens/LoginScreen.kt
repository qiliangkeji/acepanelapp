package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val vm = viewModel<LoginViewModel>()
    val config by vm.config.collectAsStateWithLifecycle()
    val username by vm.username.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val passCode by vm.passCode.collectAsStateWithLifecycle()
    val captchaCode by vm.captchaCode.collectAsStateWithLifecycle()
    val captcha by vm.captcha.collectAsStateWithLifecycle()
    val is2fa by vm.is2fa.collectAsStateWithLifecycle()
    val loginStatus by vm.loginStatus.collectAsStateWithLifecycle()

    val isLoading = loginStatus is LoginViewModel.LoginStatus.Loading
    val loginError = (loginStatus as? LoginViewModel.LoginStatus.Error)?.message

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }
            Text("登录面板", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Box(modifier = Modifier.size(36.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 面板信息
            if (config != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = config!!.name.ifEmpty { "面板" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                        Text(
                            text = "${config!!.scheme}://${config!!.host}:${config!!.port}",
                            fontSize = 13.sp,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }

            // 用户名
            LoginFieldSection("用户名") {
                OutlinedTextField(
                    value = username,
                    onValueChange = { vm.onUsernameChanged(it) },
                    placeholder = { Text("admin") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 密码
            LoginFieldSection("密码") {
                OutlinedTextField(
                    value = password,
                    onValueChange = { vm.password.value = it },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 2FA 验证码（如需要）
            if (is2fa) {
                LoginFieldSection("两步验证码") {
                    OutlinedTextField(
                        value = passCode,
                        onValueChange = { vm.passCode.value = it },
                        placeholder = { Text("6位验证码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 图形验证码（如需要）
            if (captcha?.required == true) {
                LoginFieldSection("图形验证码") {
                    OutlinedTextField(
                        value = captchaCode,
                        onValueChange = { vm.captchaCode.value = it },
                        placeholder = { Text("验证码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 错误提示
            if (loginError != null) {
                Text(
                    text = loginError,
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444)
                )
            }

            // 登录按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLoading) Color(0xFFBFDBFE) else Color(0xFF2563EB))
                    .clickable(enabled = !isLoading) {
                        vm.login(onLoginSuccess)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("登录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LoginFieldSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF71717A),
            letterSpacing = 1.sp
        )
        content()
    }
}
