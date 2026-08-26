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

package eu.europa.ec.eudi.wallet.transactionLogging.model

/**
 * The kind of a transaction (TS10 §3.1).
 *
 * Each [TransactionEntry] subtype maps to one value. `CredentialIssuance` and
 * `CredentialReissuance` are separate values but share the same data (TS10 §3.5). Each name
 * matches the TS10 string used on the wire.
 */
enum class TransactionType {
    Presentation,
    PseudonymPresentation,
    W2WPresentation,
    W2WPresentationRequest,
    CredentialIssuance,
    CredentialReissuance,
    CredentialDeletion,
    PseudonymGeneration,
    PseudonymDeletion,
    CertificateIssuance,
    CertificateDeletion,
    SigningSealing,
    DataDeletionRequest,
    DPAReport,
    OtherTransaction,
}
