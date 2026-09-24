package com.granatum.core.data.logger

import co.touchlab.kermit.Logger
import com.granatum.core.domain.logger.AppLogger

object KermitLogger: AppLogger {

    override fun debug(message: String) = Logger.d(messageString = message)

    override fun info(message: String) = Logger.i(messageString = message)

    override fun warn(message: String) = Logger.w(messageString = message)

    override fun error(message: String, throwable: Throwable?) = Logger.e(messageString = message, throwable = throwable)
}