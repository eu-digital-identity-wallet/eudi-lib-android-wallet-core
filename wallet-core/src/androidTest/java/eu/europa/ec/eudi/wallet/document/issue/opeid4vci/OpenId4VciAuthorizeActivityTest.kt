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

package eu.europa.ec.eudi.wallet.document.issue.opeid4vci

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciAuthorizeActivity
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciManager
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class OpenId4VciAuthorizeActivityTest {

    private lateinit var callback: OpenId4VciManager.AuthorizationCallback

    @Before
    fun setUp() {
        Intents.init()
        callback = Mockito.mock(OpenId4VciManager.AuthorizationCallback::class.java)
        OpenId4VciAuthorizeActivity.callback = callback
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun test_onCreate_calls_callback_with_authorization_code() {
        val authorizationCode = "authorization_code"
        val deeplinkUrl = "http://eudi-openid4ci://authorize?code=$authorizationCode"
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            OpenId4VciAuthorizeActivity::class.java
        ).apply {
            data = Uri.parse(deeplinkUrl)
        }
        val scenario = ActivityScenario.launch<OpenId4VciAuthorizeActivity>(intent).apply {
            val expectedIntent = hasData(deeplinkUrl)
            Intents.intended(expectedIntent)

            Mockito.verify(callback).onAuthorizationCode(authorizationCode)
        }
        Assert.assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun test_onCreate_calls_callback_with_null_if_code_is_missing() {
        val deeplinkUrl = "http://eudi-openid4ci://authorize"
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            OpenId4VciAuthorizeActivity::class.java
        ).apply {
            data = Uri.parse(deeplinkUrl)
        }
        val scenario = ActivityScenario.launch<OpenId4VciAuthorizeActivity>(intent).apply {
            val expectedIntent = hasData(deeplinkUrl)
            Intents.intended(expectedIntent)

            Mockito.verify(callback).onAuthorizationCode(null)
        }
        Assert.assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }
}