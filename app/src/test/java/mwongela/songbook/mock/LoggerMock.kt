package mwongela.songbook.mock

import mwongela.songbook.info.logger.LogLevel
import mwongela.songbook.info.logger.Logger

class LoggerMock : Logger() {

    override fun fatal(t: Throwable) {
        log(t.message, LogLevel.FATAL, "[FATAL ERROR] ")
    }

    override fun printInfo(msg: String) {
        println(msg)
    }

    override fun printError(msg: String) {
        System.err.println(msg)
    }

    override fun printDebug(msg: String) {
        println(msg)
    }

    override fun printWarn(msg: String) {
        println(msg)
    }

}
