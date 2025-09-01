package nl.merijntieltjes.intenttester.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.merijntieltjes.intenttester.R
import nl.merijntieltjes.intenttester.model.ReceivedIntent
import nl.merijntieltjes.intenttester.receiver.IntentReceiver
import nl.merijntieltjes.intenttester.ui.components.ConfirmDialog
import nl.merijntieltjes.intenttester.ui.components.FlowRowMain
import nl.merijntieltjes.intenttester.ui.components.IntentCard
import nl.merijntieltjes.intenttester.ui.theme.IntentTesterTheme
import nl.merijntieltjes.intenttester.util.parseActions
import nl.merijntieltjes.intenttester.util.pretty
import nl.merijntieltjes.intenttester.util.toJson
import nl.merijntieltjes.intenttester.util.toPrintable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val context = LocalContext.current

    // State
    var actionInput by remember { mutableStateOf("") }
    val actions = remember { mutableStateListOf<String>() }
    val received = remember { mutableStateListOf<ReceivedIntent>() }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Receiver
    val controller = remember { IntentReceiver(context) }
    DisposableEffect(actions.joinToString("|")) {
        controller.register(actions) { intent ->
            val extrasMap =
                intent.extras?.keySet()?.associateWith { k -> intent.extras?.get(k)?.toPrintable() }
                    ?: emptyMap()
            received.add(
                0, ReceivedIntent(
                    time = System.currentTimeMillis(),
                    action = intent.action,
                    fromPackage = intent.`package` ?: intent.component?.packageName,
                    extras = extrasMap
                )
            )
        }
        onDispose { controller.unregister() }
    }

    // Import (JSON array or newline text)
    val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()
                ?.use { it.readText() } ?: ""
            val imported = parseActions(text)
            actions.clear(); actions.addAll(imported)
        }
    }

    // Export (JSON)
    val exportLauncher =
        rememberLauncherForActivityResult(CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()
                    ?.use { it.write(toJson(actions)) }
            }
        }

    IntentTesterTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Intent Tester") },
                    actions = {
                        IconButton(onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/*"
                                )
                            )
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.upload),
                                contentDescription = "Import"
                            )
                        }
                        IconButton(onClick = { exportLauncher.launch("intent-actions.json") }) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Export"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = actionInput,
                        onValueChange = { actionInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add action (e.g. com.example.PING)") }
                    )
                    IconButton(onClick = {
                        val action = actionInput.trim()
                        if (action.isNotEmpty() && action !in actions) actions.add(action)
                        actionInput = ""
                    }) { Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add") }
                }

                Spacer(Modifier.height(12.dp))

                if (actions.isEmpty()) {
                    Text(
                        "No actions yet. Add some or import from a file.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            "Active actions:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showClearConfirm = true },
                            enabled = actions.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear actions"
                            )
                        }
                    }
                    FlowRowMain(actions) { toRemove -> actions.remove(toRemove) }
                }

                HorizontalDivider(Modifier.padding(vertical = 16.dp))

                Text("Received intents:", style = MaterialTheme.typography.titleMedium)
                if (received.isEmpty()) {
                    Text(
                        "Nothing yet. Send a broadcast matching one of your actions.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        received.forEach { item ->
                            IntentCard(
                                item
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { received.clear() },
                        enabled = received.isNotEmpty()
                    ) { Text("Clear log") }
                    OutlinedButton(onClick = {
                        val text = received.joinToString("\n\n") { it.pretty() }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Share log"))
                    }, enabled = received.isNotEmpty()) { Text("Share log") }
                }
            }
        }
    }
    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear all actions?",
            body = "This will unregister the receiver until you add some actions again.",
            onDismiss = { showClearConfirm = false },
            onConfirm = { actions.clear(); showClearConfirm = false }
        )
    }
}