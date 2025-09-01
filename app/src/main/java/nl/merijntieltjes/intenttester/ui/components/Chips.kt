package nl.merijntieltjes.intenttester.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FlowRowMain(actions: List<String>, onRemove: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action -> InputChip(text = action) { onRemove(action) } }
    }
}

@Composable
fun InputChip(text: String, onRemove: () -> Unit) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        trailingIcon = { Text("✕", modifier = Modifier.clickable { onRemove() }) }
    )
}