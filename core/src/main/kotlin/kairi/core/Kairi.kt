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

import kairi.core.entities.UserEntity
import kairi.core.events.Event
import kairi.core.logging.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * DSL function to create a [Kairi] instance.
 * @param block The builder to use when creating an instance.
 * @return A [Kairi] instance with your options.
 */
@OptIn(ExperimentalContracts::class)
fun Kairi(block: KairiBuilder.() -> Unit): Kairi {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return Kairi(KairiBuilder().apply(block))
}

/**
 * Represents the main instance to run your Revolt bot on.
 * @param builder The builder used to connect to Revolt's services.
 */
class Kairi internal constructor(val builder: KairiBuilder) {
    private val executor = Executors.newCachedThreadPool(KairiThreadFactory())
    private val logger by logging<Kairi>()
    lateinit var selfUser: UserEntity

    val gateway = Gateway(
        builder.httpClient,
        this,
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE),
        executor.asCoroutineDispatcher()
    )

    /**
     * Launches your bot to the skies! I mean, it'll connect your bot
     * to Revolt.
     */
    suspend fun launch() {
        if (builder.shutdownHook) {
            val thread = Thread({
                logger.warn("Received CTRL+C call, now disconnecting...")
                gateway.close()
            }, "Kairi-ShutdownThread")

            Runtime.getRuntime().addShutdownHook(thread)
        }

        gateway.start()
    }
}

suspend inline fun <reified T: Event> Kairi.on(
    scope: CoroutineScope = this.gateway,
    noinline consumer: suspend T.() -> Unit
) = this
    .gateway
    .eventFlow
    .buffer(Channel.UNLIMITED)
    .filterIsInstance<T>()
    .onEach {
        scope.launch {
            kotlin.runCatching {
                consumer(it)
            }.onFailure {
                this@on.builder.errorCallback?.invoke(it)
            }
        }
    }.launchIn(scope)
