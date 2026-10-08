package com.granatum.core.data.testing

import com.granatum.core.domain.logger.AppLogger

class RecordingLogger : AppLogger {
    val lines = mutableListOf<String>()
    override fun debug(message: String) { lines += message }
    override fun info(message: String) { lines += message }
    override fun warn(message: String) { lines += message }
    override fun error(message: String, throwable: Throwable?) { lines += message }
}
