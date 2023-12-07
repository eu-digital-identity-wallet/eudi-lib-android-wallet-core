/*
 *  Copyright (c) 2023 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.document.issue.openid4vci

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class OpenId4VciAuthorizeActivity : AppCompatActivity() {

    companion object {
        internal var callback: OpenId4VciManager.AuthorizationCallback? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        try {
            callback?.onAuthorizationCode(intent.data?.getQueryParameter("code"))
        } catch (e: Exception) {
            callback?.onAuthorizationCode(null)
        } finally {
            // clear callback
            callback = null
            finish()
        }
    }
}