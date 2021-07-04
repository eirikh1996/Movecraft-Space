package io.github.eirikh1996.movecraftspace.expansion.exception

import java.lang.RuntimeException

data class InvalidExpansionException(override val message: String, override val cause : Throwable? = null) : RuntimeException(message, cause) {

}