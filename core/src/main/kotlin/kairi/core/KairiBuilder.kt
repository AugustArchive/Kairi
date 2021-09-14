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
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import kairi.core.annotations.KairiDsl
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@KairiDsl
class KairiBuilder {
    /**
     * Adds a shutdown hook in the Runtime to close off gateways.
     */
    var shutdownHook: Boolean = false

    /**
     * Sets the JSON builder to use when encoding/decoding objects.
     */
    var json: Json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * The API url to use to query objects
     */
    var useApiUrl: String = "https://api.revolt.chat"

    /**
     * Attachable error callback when Kairi has reached an exception, useful
     * for logging errors.
     */
    var errorCallback: ((Throwable) -> Unit)? = null

    /**
     * Sets your http client implementation here if you wish.
     */
    @OptIn(ExperimentalTime::class)
    var httpClient: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(WebSockets) {
            pingInterval = Duration.milliseconds(30).inWholeMilliseconds
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer(this@KairiBuilder.json)
        }

        engine {
            config {
                followRedirects(true)
            }
        }
    }

    /**
     * The token to use to connect to Revolt's gateway
     */
    var token: String by Delegates.notNull()

    /**
     * Sets the error callback
     * @param callback The callback function to use.
     */
    fun onError(callback: (Throwable) -> Unit) {
        errorCallback = callback
    }

    /**
     * Sets the API url to use
     * @param url The URL to use
     */
    fun setApiUrl(url: String) {
        useApiUrl = url
    }
}
