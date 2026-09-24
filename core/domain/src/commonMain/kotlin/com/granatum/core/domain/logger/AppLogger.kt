package com.granatum.core.domain.logger

interface AppLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}