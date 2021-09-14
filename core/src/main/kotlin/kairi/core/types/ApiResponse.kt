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

package kairi.core.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val revolt: String,
    val features: RevoltFeatures,
    val ws: String,
    val app: String,
    val vapid: String
)

@Serializable
data class RevoltFeatures(
    val captcha: RevoltCaptcha,
    val email: Boolean,
    val autumn: AutumnApiResponse,
    val january: JanuaryApiResponse,
    val voso: VosoApiResponse,

    @SerialName("invite_only")
    val inviteOnly: Boolean
)

@Serializable
data class RevoltCaptcha(
    val enabled: Boolean,
    val key: String?
)

@Serializable
data class AutumnApiResponse(
    val enabled: Boolean,
    val url: String
)

@Serializable
data class JanuaryApiResponse(
    val enabled: Boolean,
    val url: String
)

@Serializable
data class VosoApiResponse(
    val enabled: Boolean,
    val url: String,
    val ws: String
)
