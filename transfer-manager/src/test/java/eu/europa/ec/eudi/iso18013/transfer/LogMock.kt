/*
 * Copyright (c) 2024-2025 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.iso18013.transfer

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

fun mockAndroidLog() = mockStatic(android.util.Log::class.java).apply {
    val printToConsoleAnswer = PrintToConsoleAnswer()

    // Mocking different Log methods
    Mockito.`when`(android.util.Log.d(anyString(), anyString()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.d(anyString(), anyString(), any()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.i(anyString(), anyString()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.i(anyString(), anyString(), any()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.w(anyString(), anyString()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.w(anyString(), anyString(), any()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.e(anyString(), anyString()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.e(anyString(), anyString(), any()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.v(anyString(), anyString()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.v(anyString(), anyString(), any()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.wtf(anyString(), anyString()))
        .thenAnswer(printToConsoleAnswer)
    Mockito.`when`(android.util.Log.wtf(anyString(), anyString(), any()))
        .thenAnswer(printToConsoleAnswer)
}

class PrintToConsoleAnswer : Answer<Int> {
    override fun answer(invocation: InvocationOnMock): Int {
        val methodName = invocation.method.name
        val tag = invocation.getArgument<String>(0)
        val msg = invocation.getArgument<String>(1)
        val throwable = invocation.arguments.getOrNull(2) as? Throwable
        println("Mocked Log - Method: $methodName, TAG: $tag, Message: $msg, Throwable: $throwable")
        throwable?.printStackTrace()
        return 0 // Return value for Log methods
    }
}