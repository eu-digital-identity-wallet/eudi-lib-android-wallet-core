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

package eu.europa.ec.eudi.wallet.presentation

import androidx.annotation.IntDef
import eu.europa.ec.eudi.wallet.presentation.SessionTerminationFlag.Companion.SEND_SESSION_TERMINATION_MESSAGE
import eu.europa.ec.eudi.wallet.presentation.SessionTerminationFlag.Companion.USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION

/**
 * Annotation that defines the possible flags for session termination.
 *
 * @see [PresentationManager.stopProximityPresentation]
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(value = [SEND_SESSION_TERMINATION_MESSAGE, USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION])
annotation class SessionTerminationFlag {
    /**
     * Companion object that defines the possible flags for session termination.
     *
     * @property SEND_SESSION_TERMINATION_MESSAGE the flag that indicates that a session termination message should be sent
     * @property USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION the flag that indicates that the transport specific session termination should be used
     */
    companion object {
        const val SEND_SESSION_TERMINATION_MESSAGE = 1 shl 0
        const val USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION = 1 shl 1
    }
}