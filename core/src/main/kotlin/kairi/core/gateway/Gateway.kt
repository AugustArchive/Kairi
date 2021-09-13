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

package kairi.core.gateway

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kairi.core.Kairi
import kairi.core.logging.logging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

/**
 * Represents the gateway connection between your bot and Revolt.
 */
class Gateway internal constructor(
    private val httpClient: HttpClient,
    private val kairi: Kairi,
    val eventFlow: MutableSharedFlow<Any>,
    dispatcher: CoroutineDispatcher
): CoroutineScope {
    private var defaultSession: DefaultClientWebSocketSession by Delegates.notNull()
    private var hasAcked = CompletableDeferred<Boolean>()
    private var stopEvent = CompletableDeferred<Unit>()
    private val logger by logging<Gateway>()
    private val pings: MutableList<Int> = mutableListOf()

    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatcher

    @OptIn(DelicateCoroutinesApi::class)
    internal suspend fun receiveMessageLoop() {
        defaultSession.incoming.receiveAsFlow().collect {
            val data = (it as Frame.Text).readText()
            val json = Json.decodeFromString(JsonObject.serializer(), data)

            GlobalScope.launch {
                dispatch(json)
            }
        }
    }

    private fun dispatch(data: JsonObject) {
        logger.debug("Received object:")
        logger.debug(data.toString())
    }

    suspend fun start() {
        logger.info("Welcome chief! Let's get logged in...")

        // Get information about Revolt
        val resp: JsonObject = httpClient.get("https://${kairi.builder.useApiUrl}") {
            header("x-bot-token", kairi.builder.token)
        }

        val wsUrl = resp["ws"]?.jsonPrimitive?.content ?: error("unable to get ws url.")
        httpClient.wss(wsUrl) {
            logger.info("Connected to WebSocket successfully.")
            this@Gateway.defaultSession = this

            // Authenticate to Revolt
            send(JsonObject(mapOf(
                "type" to JsonPrimitive("Authenticate"),
                "token" to JsonPrimitive(kairi.builder.token)
            )).toString())
        }
    }
}
