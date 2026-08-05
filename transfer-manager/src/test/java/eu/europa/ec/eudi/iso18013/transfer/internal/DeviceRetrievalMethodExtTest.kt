/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.internal

import eu.europa.ec.eudi.iso18013.transfer.engagement.BleRetrievalMethod
import org.junit.Assert
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.Test
import org.multipaz.cbor.Cbor as CborUtil

@RunWith(Parameterized::class)
class DeviceRetrievalMethodExtTest(
    private val peripheralServerMode: Boolean,
    private val centralClientMode: Boolean,
    private val clearBleCache: Boolean,
    private val expectedMethodCount: Int,
) {

    companion object {
        @Parameters(name = "{index}: peripheralServerMode={0}, centralClientMode={1}, clearBleCache={2}")
        @JvmStatic
        fun data(): List<Array<Any>> {
            return listOf(
                arrayOf(false, true, false, 1),
                arrayOf(false, false, false, 0),
                arrayOf(true, false, false, 1),
                arrayOf(true, true, false, 1),
                arrayOf(false, true, true, 1),
                arrayOf(false, false, true, 0),
                arrayOf(true, false, true, 1),
                arrayOf(true, true, true, 1),
            )
        }
    }

    @Test
    fun testDeviceRetrievalMethodConnectionMethodsAndTransportOptions() {
        val bleRetrievalMethod =
            BleRetrievalMethod(peripheralServerMode, centralClientMode, clearBleCache)
        val connectionMethods = bleRetrievalMethod.connectionMethod
        Assert.assertEquals(expectedMethodCount, connectionMethods.size)

        val transportOptions = listOf(bleRetrievalMethod).transportOptions
        Assert.assertEquals(clearBleCache, transportOptions.bleClearCache)

        connectionMethods.firstOrNull()
            ?.toDeviceEngagement()
            ?.let { CborUtil.toDiagnostics(it) }
            ?.also { println("BleOptions: $it") }
    }

}