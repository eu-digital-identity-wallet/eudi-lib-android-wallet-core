/*
 * Copyright (c) 2026 European Commission
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

/**
 * The error of the [TransferEvent.Error] triggered when [TransferManager.sendResponse] does not send
 * a response, because there is no active presentation session or the response was generated for
 * another session. The active session, if any, is not affected.
 *
 * @param message the reason the response was not sent
 */
class ResponseNotSentException(message: String) : IllegalStateException(message)
