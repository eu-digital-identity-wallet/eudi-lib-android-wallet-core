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

package eu.europa.ec.eudi.wallet.document


typealias DocumentId = String
typealias DocType = String
typealias NameSpace = String
typealias ElementIdentifier = String
typealias NameSpaces = Map<NameSpace, List<ElementIdentifier>>
typealias NameSpacedValues<T> = Map<NameSpace, Map<ElementIdentifier, T>>
typealias ProofOfDeletion = ByteArray
typealias SharedSecret = ByteArray
typealias Vct = String

