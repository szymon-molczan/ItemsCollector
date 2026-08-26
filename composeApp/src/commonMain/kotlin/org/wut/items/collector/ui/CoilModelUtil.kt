package org.wut.items.collector.ui
























internal fun toCoilModel(value: String, serverBaseUrl: String? = null): Any {
    if (value.isBlank()) return value
    return when {
        value.startsWith("http://") || value.startsWith("https://") ||
            value.startsWith("file://") -> value
        
        value.startsWith("/uploads/") && serverBaseUrl != null ->
            "${serverBaseUrl.trimEnd('/')}$value"
        
        value.startsWith("/") -> "file://$value"
        else -> value
    }
}
