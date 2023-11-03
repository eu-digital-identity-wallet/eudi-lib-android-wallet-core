/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.eudi.wallet.documentsTest.util

import eu.europa.ec.eudi.wallet.R
import eu.europa.ec.eudi.wallet.document.Constants as DocConstants

/**
 * Document Type, e.g. MDL, PID, MICOV, MVR
 *
 * @property docTypeName the document type name, e.g. eu.europa.ec.eudiw.pid.1
 * @property userFriendlyName a user friendly name for the document
 * @constructor Create empty Doc type
 */
enum class DocType(val docTypeName: String, val userFriendlyName: Int) {

    /**
     * PID Doc Type
     *
     * @constructor Create empty Pid
     */
    PID(DocConstants.EU_PID_DOCTYPE, R.string.eu_pid_doctype_name),

    /**
     * MDL Doc Type
     *
     * @constructor Create empty Mdl
     */
    MDL(DocConstants.MDL_DOCTYPE, R.string.mdl_doctype_name),

    /**
     * MICOV Doc Type
     *
     * @constructor Create empty Micov
     */
    MICOV(DocConstants.MICOV_DOCTYPE, R.string.micov_doctype_name),

    /**
     * MVR Doc Type
     *
     * @constructor Create empty Mvr
     */
    MVR(DocConstants.MVR_DOCTYPE, R.string.mvr_doctype_name);

    companion object {
        infix fun from(name: String): DocType? =
            DocType.values().firstOrNull { it.docTypeName == name }
    }
}