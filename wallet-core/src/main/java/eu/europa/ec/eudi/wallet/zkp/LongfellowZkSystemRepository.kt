/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.zkp

import android.content.Context
import kotlinx.io.bytestring.ByteString
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem

/**
 * Data class representing a circuit with its filename and byte content.
 *
 * @property filename the name of the circuit file
 * @property bytes the byte content of the circuit
 */
data class Circuit(
    val filename: String,
    val bytes: ByteString
)

/**
 * Repository builder for Longfellow zero-knowledge proof systems.
 *
 * @property circuits the list of circuits to be added to the Longfellow Zk system
 */
class LongfellowZkSystemRepository(
    private val circuits: List<Circuit>
) {
    fun build(): ZkSystemRepository {

        require(circuits.isNotEmpty()) { "No circuits provided for Longfellow ZK system." }

        return ZkSystemRepository().apply {
            val longfellowSystem = LongfellowZkSystem()
            circuits.forEach { circuit ->
                longfellowSystem.addCircuit(
                    circuitFilename = circuit.filename,
                    circuitBytes = circuit.bytes
                )
            }
            add(longfellowSystem)
        }
    }
}

/**
 * Default Longfellow circuits.
 */
object LongfellowCircuits {
    private val defaultCircuits = listOf(
        "circuits/longfellow-libzk-v1/7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
        "circuits/longfellow-libzk-v1/7_2_4265_4096_6a5810683e62b6d7766ebd0d7ca72518a2b8325418142adcadb10d51dbbcd5ad",
        "circuits/longfellow-libzk-v1/7_3_4307_4096_8ee4849ae1293ae6fe5f9082ce3e5e15c4f198f2998c682fa1b727237d6d252f",
        "circuits/longfellow-libzk-v1/7_4_4415_4096_5aebdaaafe17296a3ef3ca6c80c6e7505e09291897c39700410a365fb278e460"
    )

    @JvmStatic
    fun get(context: Context): List<Circuit> {
        val appContext = context.applicationContext
        return defaultCircuits.map { path ->
            val bytes = appContext.assets.open(path).use { ByteString(it.readBytes()) }
            Circuit(
                filename = path.substringAfterLast('/'),
                bytes = bytes
            )
        }
    }
}