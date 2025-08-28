package nl.merijntieltjes.intenttester


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

data class ReceivedIntent(
    val time: Long,
    val action: String?,
    val fromPackage: String?,
    val extras: Map<String, Any?>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // UI state
    var actionInput by remember { mutableStateOf("") }
    val actions = remember { mutableStateListOf<String>() }
    val received = remember { mutableStateListOf<ReceivedIntent>() }

    var showClearConfirm by remember { mutableStateOf(false) }

    // Dynamic receiver that app (re)registers whenever actions change
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                val extrasMap = intent.extras?.keySet()?.associateWith { key ->
                    intent.extras?.get(key)?.toPrintable()
                } ?: emptyMap()
                received.add(
                    0, ReceivedIntent(
                        time = System.currentTimeMillis(),
                        action = intent.action,
                        fromPackage = intent.`package` ?: intent.component?.packageName,
                        extras = extrasMap
                    )
                )
            }
        }
    }

    // Register/unregister as actions change or lifecycle re-composes
    DisposableEffect(actions.joinToString("|")) {
        val filter = IntentFilter().also { f ->
            actions.forEach { a ->
                if (a.isNotBlank()) f.addAction(a)
            }
        }
        // (Re)register only if we have at least one action
        if (filter.countActions() > 0) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        }
        onDispose {
            // Safe: unregister if it was registered for any actions
            if (filter.countActions() > 0) {
                kotlin.runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    // Import (JSON array of strings OR newline-delimited text)
    val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins)).readText()
            } ?: ""
            val imported = parseActions(text)
            actions.clear(); actions.addAll(imported)
        }
    }

    // Export (writes JSON array)
    val exportLauncher =
        rememberLauncherForActivityResult(CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                val payload = toJson(actions)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out ->
                    out.write(payload)
                }
            }
        }

    MaterialTheme {
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
                        val a = actionInput.trim()
                        if (a.isNotEmpty() && a !in actions) actions.add(a)
                        actionInput = ""
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add"
                        )
                    }
//                    Button(onClick = {
//                        val a = actionInput.trim()
//                        if (a.isNotEmpty() && a !in actions) actions.add(a)
//                        actionInput = ""
//                    }) { Text("Add") }
                }

                Spacer(Modifier.height(12.dp))

                if (actions.isEmpty()) {
                    AssistiveText("No actions yet. Add some or import from a file.")
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

                Divider(Modifier.padding(vertical = 16.dp))

                Text("Received intents:", style = MaterialTheme.typography.titleMedium)
                if (received.isEmpty()) {
                    AssistiveText("Nothing yet. Send a broadcast matching one of your actions.")
                } else {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        received.forEach { item -> IntentCard(item) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { received.clear() },
                        enabled = received.isNotEmpty()
                    ) { Text("Clear log") }
                    OutlinedButton(onClick = {
                        // Share plain text log via ACTION_SEND (optional helper)
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
            onConfirm = {
                actions.clear(); showClearConfirm = false
            }
        )
    }
}

@Composable
fun AssistiveText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun FlowRowMain(actions: List<String>, onRemove: (String) -> Unit) {
    // Minimal wrap layout without dependency: place chips in rows
    FlowRow(spacing = 8.dp, runSpacing = 8.dp) {
        actions.forEach { a ->
            InputChip(text = a, onRemove = { onRemove(a) })
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    runSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    // Very small wrapper around Column/Row to approximate wrap flow using Layout; keep simple
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(runSpacing)
    ) { content() }
}

@Composable
fun InputChip(text: String, onRemove: () -> Unit) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        trailingIcon = {
            Text("✕", modifier = Modifier.padding(start = 4.dp).clickable { onRemove()})
        },
        modifier = Modifier
    )
    // Remove via long-press would be nicer; keep a long-press-less simple approach:
    // Provide a small button row beneath chips if needed; for brevity, tap the chip to remove
    // (Alternative UX: replace onClick with a Row with a delete icon.)
}

@Composable
fun IntentCard(item: ReceivedIntent) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) }
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(fmt.format(Date(item.time)), style = MaterialTheme.typography.labelSmall)
            Text(item.action ?: "(no action)", style = MaterialTheme.typography.titleMedium)
            item.fromPackage?.let { Text("from: $it", style = MaterialTheme.typography.bodySmall) }
            if (item.extras.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                item.extras.forEach { (k, v) ->
                    Text(
                        "$k = ${v.toString().truncate(300)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseActions(text: String): List<String> =
    runCatching {
        // Try JSON first: ["ACTION_1","ACTION_2"]
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val regex = "\"(.*?)\"".toRegex()
            regex.findAll(trimmed).map { it.groupValues[1] }.filter { it.isNotBlank() }.toList()
        } else {
            // Fallback: newline- or comma-delimited
            trimmed.split('\n', ',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }.getOrDefault(emptyList())

private fun toJson(actions: List<String>): String =
    buildString {
        append('[')
        actions.forEachIndexed { i, a ->
            if (i > 0) append(',')
            append('"').append(a.replace("\"", "\\\"")).append('"')
        }
        append(']')
    }

private fun Any.toPrintable(): Any = when (this) {
    is Bundle -> this.keySet().associateWith { k -> this.get(k)?.toPrintable() }
    is Array<*> -> this.map { it?.toPrintable() }
    is IntArray -> this.toList()
    is LongArray -> this.toList()
    is FloatArray -> this.toList()
    is DoubleArray -> this.toList()
    is BooleanArray -> this.toList()
    is Parcelable -> this.toString()
    else -> this
}

private fun String.truncate(max: Int): String = if (length <= max) this else take(max) + "…"

private fun ReceivedIntent.pretty(): String = buildString {
    append("time=").append(time).append('\n')
    append("action=").append(action).append('\n')
    append("from=").append(fromPackage).append('\n')
    if (extras.isNotEmpty()) {
        append("extras=\n")
        extras.forEach { (k, v) -> append("  ").append(k).append(" = ").append(v).append('\n') }
    }
}
