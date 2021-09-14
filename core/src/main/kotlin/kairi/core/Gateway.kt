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

package kairi.core

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kairi.core.events.Event
import kairi.core.events.EventType
import kairi.core.events.ReadyEvent
import kairi.core.logging.logging
import kairi.core.types.ApiResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@Serializable
internal data class OpenPacket(
    val type: String,
    val error: String? = null
)

/**
 * Represents the gateway connection between your bot and Revolt.
 */
class Gateway internal constructor(
    private val httpClient: HttpClient,
    private val kairi: Kairi,
    val eventFlow: MutableSharedFlow<Any>,
    dispatcher: CoroutineDispatcher
): CoroutineScope {
    private var heartbeatJob: Job? = null
    private var messageJob: Job? = null
    private var defaultSession: DefaultClientWebSocketSession by Delegates.notNull()
    private var lastReceivedAt: Long? = null
    private var lastAckedAt: Long? = null
    private var hasAcked = CompletableDeferred<Unit>()
    private var stopEvent = CompletableDeferred<Unit>()
    private val logger by logging<Gateway>()

    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatcher

    private val errorHandler = CoroutineExceptionHandler { _, t ->
        logger.error("Exception was thrown in coroutine:", t)
    }

    @OptIn(DelicateCoroutinesApi::class)
    internal suspend fun receiveMessageLoop() {
        defaultSession.incoming.receiveAsFlow().collect {
            val data = (it as Frame.Text).readText()
            val json = kairi.builder.json.decodeFromString(Event.serializer(), data)

            GlobalScope.launch(errorHandler) {
                dispatch(json, data)
            }
        }
    }

    fun close() {
        logger.warn("Told to close gateway connection.")
        stopEvent.complete(Unit)
    }

    private suspend fun dispatch(data: Event, raw: String) {
        logger.trace("Received data packet %o", data)
        when (data.type) {
            EventType.Ready -> {
                val event = kairi.builder.json.decodeFromString(ReadyEvent.serializer(), raw)

                kairi.selfUser = event.users.first()
                logger.info("We have launched and received a stable connection! Hello ${kairi.selfUser.username} <3")
                eventFlow.emit(event)

                // populate server cache
                // TODO: server cache
            }

            EventType.Pong -> {
                lastReceivedAt = Instant.now().toEpochMilli()

                val ping = lastReceivedAt!! - lastAckedAt!!
                logger.info("Received a heartbeat. Ping is now at ~${ping}ms")
                hasAcked.complete(Unit)
            }

            EventType.Null -> {
                logger.error("Received `null` packet type, cannot do anything. :(")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun heartbeatLoop() {
        while (true) {
            delay(Duration.Companion.seconds(30))
            defaultSession.send("{\"type\":\"Ping\",\"time\":0}")

            lastAckedAt = Instant.now().toEpochMilli()
            hasAcked.await()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start() {
        // Get information about Revolt
        val resp: ApiResponse = httpClient.get("https://api.revolt.chat") {
            header("x-bot-token", kairi.builder.token)
        }

        httpClient.wss(resp.ws) {
            logger.info("Connected to WebSocket using URI ${resp.ws}!")
            this@Gateway.defaultSession = this

            // Authenticate to Revolt
            send("{\"type\":\"Authenticate\",\"token\":\"${kairi.builder.token}\"}")

            // Poll for events
            val message = incoming
                .receive()
                .readBytes()
                .decodeToString()

            val packet = kairi.builder.json.decodeFromString(OpenPacket.serializer(), message)

            // Check for the message
            when (packet.type) {
                "Error" -> {
                    // TODO: this
                }

                "Authenticated" -> {
                    logger.info("Authentication was successful! Welcome automation.")

                    // Setup message loop
                    messageJob = GlobalScope.launch(errorHandler) {
                        receiveMessageLoop()
                    }

                    heartbeatJob = GlobalScope.launch(errorHandler) {
                        heartbeatLoop()
                    }

                    logger.info("Message loop has been created, waiting for be stopped...")
                    stopEvent.await()

                    logger.warn("Told to disconnect from Revolt.")
                    messageJob?.cancelAndJoin()
                    heartbeatJob?.cancelAndJoin()
                }
            }
        }
    }
}
