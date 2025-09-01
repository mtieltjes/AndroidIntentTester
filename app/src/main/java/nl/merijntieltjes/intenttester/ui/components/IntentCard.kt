package nl.merijntieltjes.intenttester.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.merijntieltjes.intenttester.model.ReceivedIntent
import nl.merijntieltjes.intenttester.util.truncate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntentCard(item: ReceivedIntent, modifier: Modifier = Modifier) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) }
    ElevatedCard(
        modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(fmt.format(Date(item.time)), style = MaterialTheme.typography.labelSmall)
            Text(item.action ?: "(no action)", style = MaterialTheme.typography.titleMedium)
            item.fromPackage?.let { Text("from: $it", style = MaterialTheme.typography.bodySmall) }
            if (item.extras.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                item.extras.forEach { (key, value) ->
                    Text(
                        "$key = ${value.toString().truncate(300)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}