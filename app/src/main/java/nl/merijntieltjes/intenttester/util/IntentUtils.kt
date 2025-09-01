package nl.merijntieltjes.intenttester.util

import android.os.Bundle
import android.os.Parcelable
import nl.merijntieltjes.intenttester.model.ReceivedIntent

// Get actions from a JSON array. Fallback to comma/newline/semicolon separation.
fun parseActions(text: String): List<String> = runCatching {
    val trimmed = text.trim()
    if (trimmed.startsWith("[")) {
        "\"(.*?)\"".toRegex()
            .findAll(trimmed)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toList()
    } else {
        trimmed.split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}.getOrDefault(emptyList())

fun toJson(actions: List<String>): String = buildString {
    append('[')
    actions.forEachIndexed { i, a ->
        if (i > 0) append(',')
        append('"').append(a.replace("\"", "\\\"")).append('"')
    }
    append(']')
}

// Make any array into a list for better print output
fun Any.toPrintable(): Any = when (this) {
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

fun String.truncate(max: Int): String = if (length <= max) this else take(max) + "…"

fun ReceivedIntent.pretty(): String = buildString {
    append("time=").append(time).append('\n')
    append("action=").append(action).append('\n')
    append("from=").append(fromPackage).append('\n')
    if (extras.isNotEmpty()) {
        append("extras=\n")
        extras.forEach { (k, v) -> append(" ").append(k).append(" = ").append(v).append('\n') }
    }
}