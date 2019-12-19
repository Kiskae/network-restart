package net.serverpeon.networkrestart.router

object NotAuthenticated : RuntimeException() {
    init {
        stackTrace = emptyArray()
    }

    interface Helper {
        suspend fun authenticate()
    }
}

suspend fun <R> ignoreAuthentication(block: suspend () -> R): R? = try {
    block()
} catch (ignored: NotAuthenticated) {
    null
}

suspend inline fun NotAuthenticated.Helper.whileAuthenticated(block: suspend () -> Any): Nothing {
    while (true) {
        try {
            block()
        } catch (ignored: NotAuthenticated) {
            authenticate()
        }
    }
}