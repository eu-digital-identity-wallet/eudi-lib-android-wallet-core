/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet

import COSE.Message
import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import com.upokecenter.cbor.CBORObject
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class TestSign1ForAuthlete {

    @Ignore("This test is ignored because it is meant to be run only by hand. It is not a unit test.")
    @Test
    fun `test sign1 cwt that we send to authlete is verified`() {

        val sign1Hex =
        // from example
//                        "d83dd284589da3012603746f70656e6964347663692d70726f6f662b63777468434f53455f4b6579a6010202582b3165354159394579423031586e557a61364c704a7a6b30326e36595f416d6d6e5362304642654e56567255032620012158203d2c50ac3db3974fe65dc02acf59a0a92781a018679b1db2c41129ac163c176d225820c55f1f6e2d454a8b14ba72deb8b36e2e4262aa663a4ca88c9eeafe1a7db0475da05860a4016c747261636b315f6c6967687403781a68747470733a2f2f747269616c2e617574686c6574652e6e6574061a665fdcab0a582b762d31622d6e38326b454a476248524f53656b47736d522d784575616d4378595f5430745874514e2d64595840461c38c8127b70ca2e491478da59c8764255513e9b15968d17367276ba2cecdaf4da83a343bef866d86f6fa516264859805d4d6cf7d836b6152eda61a42bf759"

            // from test
            "d83dd284586da3012603746f70656e6964347663692d70726f6f662b63777468434f53455f4b6579a401022001215820c584947cb27885c927ee1d3062edbbb71b25f19b92d968f5767cb91cd7ff4abc225820cd2a0a3c52a3e94650a161272042d348f174e6b770a0f596ba8f1410f3629d68a05860a4016c747261636b315f6c6967687403781a68747470733a2f2f747269616c2e617574686c6574652e6e6574061a6661a0910a582b373432523836737067344e576c754c52483043716a6d303877477772747a3653325359553253587733636f584830460221009c14113a04660a35829efe0efd3c2278c95ef9983fab46f9c35eae54c26ae152022100aa789b9051d50757a4438574ac2a23ed9255f631f0efcc3447b5ba1e9e4237a6"
        val sign1Bytes = Hex.decode(sign1Hex)
        val cwtUntagged = CBORObject.DecodeFromBytes(sign1Bytes).let {
            if (it.isTagged) it.Untag() else it
        }.EncodeToBytes()
        val sign1Message = Message.DecodeFromBytes(cwtUntagged, MessageTag.Sign1) as Sign1Message
        val oneKey = OneKey(sign1Message.protectedAttributes.get("COSE_Key"))
        Assert.assertTrue(sign1Message.validate(oneKey))
    }
}