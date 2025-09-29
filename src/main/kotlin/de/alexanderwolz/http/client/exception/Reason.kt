package de.alexanderwolz.http.client.exception

data class Reason(val code: String, val description: String) {
    companion object {
        const val CODE_UNKNOWN = "unknown"
        const val CODE_CLIENT_ERROR = "client error"
        const val CODE_NO_BODY = "no body"
    }
}