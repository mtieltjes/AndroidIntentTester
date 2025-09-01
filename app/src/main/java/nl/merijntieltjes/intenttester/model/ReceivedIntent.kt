package nl.merijntieltjes.intenttester.model

data class ReceivedIntent(
    val time: Long,
    val action: String?,
    val fromPackage: String?,
    val extras: Map<String, Any?>
)
