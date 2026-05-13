package dev.vibecode.mobileagent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.vibecode.mobileagent.LocalSettingsRepository
import dev.vibecode.mobileagent.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repo = LocalSettingsRepository.current
    val viewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(repo))
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.savedAck) {
        if (uiState.savedAck) {
            snackbarHostState.showSnackbar("已保存")
            viewModel.onSavedConsumed()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "密钥与端点仅保存在本机加密存储（EncryptedSharedPreferences）。请勿在共享设备上保存生产密钥。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var agentExpanded by remember { mutableStateOf(false) }
            val agentOptions = SettingsRepository.agentBackendOptions
            val agentLabel =
                agentOptions.firstOrNull { it.first == uiState.agentBackendId }?.second
                    ?: uiState.agentBackendId

            ExposedDropdownMenuBox(
                expanded = agentExpanded,
                onExpandedChange = { agentExpanded = !agentExpanded },
            ) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true,
                    value = agentLabel,
                    onValueChange = {},
                    label = { Text("Agent 后端") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = agentExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = agentExpanded,
                    onDismissRequest = { agentExpanded = false },
                ) {
                    agentOptions.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                agentExpanded = false
                                viewModel.onAgentBackendSelected(id)
                            },
                        )
                    }
                }
            }

            Text(
                text =
                    "选择「OpenClaude（Termux）」后，聊天会通过 Termux 的 RUN_COMMAND 在 Termux 家目录下执行 `node dist/cli.mjs -p …`。" +
                        "需已按 README / third_party/openclaude/ANDROID_INSTALL.md 构建 CLI，并在系统设置中授予本应用 Termux 外部命令权限。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var expanded by remember { mutableStateOf(false) }
            val options = SettingsRepository.providerOptions
            val selectedLabel =
                options.firstOrNull { it.first == uiState.providerId }?.second
                    ?: uiState.providerId

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true,
                    value = selectedLabel,
                    onValueChange = {},
                    label = { Text("提供商（占位）") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                viewModel.onProviderSelected(id)
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text("Base URL（可选）") },
                placeholder = { Text("https://api.example.com/v1") },
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.modelName,
                onValueChange = viewModel::onModelNameChange,
                label = { Text("模型名称（可选）") },
                placeholder = { Text("gpt-4o-mini") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.onSave() },
            ) {
                Text("保存")
            }
        }
    }
}
