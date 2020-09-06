package io.github.eirikh1996.movecraftspace.expansion.exception

import java.lang.RuntimeException

data class ExpansionLoadException constructor(override val message : String, override val cause : Throwable) : RuntimeException(message, cause) {

}

data class ExpansionEnableException constructor(override val message : String, override val cause : Throwable) : RuntimeException(message, cause) {

}

data class ExpansionDisableException constructor(override val message : String, override val cause : Throwable) : RuntimeException(message, cause) {

}