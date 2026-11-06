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

package eu.europa.ec.eudi.wallet

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v2CRLBuilder
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.X509CRL
import java.security.spec.ECGenParameterSpec
import java.util.Date

/** A CRL of "CN=Root CA" that lists [revokedSerials] and is valid for a day. */
fun testCrl(vararg revokedSerials: Long): X509CRL {
    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    val now = Date()
    val builder = X509v2CRLBuilder(X500Name("CN=Root CA"), now).apply {
        setNextUpdate(Date(now.time + 86_400_000L))
        revokedSerials.forEach { addCRLEntry(BigInteger.valueOf(it), now, 0) }
    }
    return JcaX509CRLConverter().getCRL(builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)))
}
