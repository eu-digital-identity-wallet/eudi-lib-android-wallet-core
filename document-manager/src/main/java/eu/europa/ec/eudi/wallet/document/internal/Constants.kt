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

/**
 * Constants for the document manager.
 */

package eu.europa.ec.eudi.wallet.document.internal

/**
 * The maximum nesting depth of issuer provided data: the number of nested arrays, maps and values
 * embedded with tag 24 in CBOR, and of nested arrays and objects in JSON.
 */
internal const val MAX_ISSUER_DATA_DEPTH = 64
