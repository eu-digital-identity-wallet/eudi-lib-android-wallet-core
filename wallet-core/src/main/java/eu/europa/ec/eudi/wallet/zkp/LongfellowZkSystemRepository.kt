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
        "circuits/longfellow-libzk-v1/6_1_4096_2945_137e5a75ce72735a37c8a72da1a8a0a5df8d13365c2ae3d2c2bd6a0e7197c7c6",
        "circuits/longfellow-libzk-v1/6_2_4025_2945_b4bb6f01b7043f4f51d8302a30b36e3d4d2d0efc3c24557ab9212ad524a9764e",
        "circuits/longfellow-libzk-v1/6_3_4121_2945_b2211223b954b34a1081e3fbf71b8ea2de28efc888b4be510f532d6ba76c2010",
        "circuits/longfellow-libzk-v1/6_4_4283_2945_c70b5f44a1365c53847eb8948ad5b4fdc224251a2bc02d958c84c862823c49d6"
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