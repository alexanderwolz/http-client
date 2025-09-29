package de.alexanderwolz.http.client.log

import org.slf4j.LoggerFactory


class Logger(clazz: Class<*>) {

    private val logger = LoggerFactory.getLogger(clazz)

    fun trace(message: () -> String) {
        if (logger.isTraceEnabled) {
            logger.trace(message())
        }
    }

    fun debug(message: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(message())
        }
    }

    fun info(message: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(message())
        }
    }


    fun warn(message: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(message())
        }
    }

    fun error(message: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(message())
        }
    }

    fun error(throwable: Throwable) {
        error(throwable) {
            throwable.message ?: throwable.javaClass.name
        }
    }

    fun error(throwable: Throwable?, message: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(message(), throwable)
        }
    }
}