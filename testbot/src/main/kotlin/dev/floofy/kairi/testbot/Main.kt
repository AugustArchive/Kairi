/**
 * Copyright (c) 2021 Noel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.floofy.kairi.testbot

import kairi.core.Kairi
import kairi.core.events.ReadyEvent
import kairi.core.on
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Main {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            logger.error("You need to append a token after `java -jar testbot.jar [token]`")
            exitProcess(1)
        }

        logger.info("Launching Kairi...")
        val bot = Kairi {
            token = args[0]
            shutdownHook = true

            onError {
                logger.error("Unhandled error has occured:", it)
            }
        }

        runBlocking {
            bot.on<ReadyEvent> {
                logger.info("Logged in as ${bot.selfUser.username} (${bot.selfUser.id})")
            }

            bot.launch()
        }
    }
}
