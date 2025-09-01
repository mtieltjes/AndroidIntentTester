package nl.merijntieltjes.intenttester.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

class IntentReceiver(private val context: Context) {
    private var isRegistered = false
    private var receiver: BroadcastReceiver? = null

    fun register(actions: List<String>, onReceive: (Intent) -> Unit) {
        unregister()
        if (actions.isEmpty()) return
        val filter = IntentFilter().also { filter ->
            actions.filter { it.isNotBlank() }.forEach { filter.addAction(it) }
        }
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                intent.let(onReceive)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, filter)
        }
        receiver = broadcastReceiver
        isRegistered = true
    }


    fun unregister() {
        if (!isRegistered) return
        runCatching { context.unregisterReceiver(receiver) }
        receiver = null
        isRegistered = false
    }
}