package eu.europa.ec.eudi.wallet.zkp.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.util.Base64URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.Path
import software.tice.ChallengeRequestData
import java.security.interfaces.ECPublicKey

class ZKPClient(
    baseUrl: String = "https://staging.verifier.wallet.tice.software/wallet/zkp/",
) {
    private val retrofit: Retrofit = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(Json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()
    private val service: ZkpAPIService = retrofit.create(ZkpAPIService::class.java)

    suspend fun getChallenges(
        zkpRequestId: String,
        requestData: List<Pair<String, ChallengeRequestData>>
    ): List<Pair<String, ECPublicKey>> {
        val response = service.requestZkp(
            zkpRequestId,
            requestData.map {
                ZkpRequest(
                    it.first,
                    it.second.digest,
                    it.second.r,
                    "secp256r1-sha256",
                )
            },
        )

        return response.body()?.map {
            it.id to ECKey.Builder(
                Curve(it.crv),
                Base64URL.from(it.x),
                Base64URL.from(it.y),
            )
                .keyID(it.kid)
                .keyUse(KeyUse.SIGNATURE)
                .build()
                .toECPublicKey()
        } ?: throw IllegalArgumentException("Missing response body")
    }
}

interface ZkpAPIService {
    @POST("{zkpRequestId}/jwks.json")
    suspend fun requestZkp(
        @Path("zkpRequestId") zkpRequestId: String,
        @Body request: List<ZkpRequest>,
    ): Response<List<ZkpResponse>>
}

@Serializable
data class ZkpRequest(
    val id: String,
    val digest: String,
    val r: String,
    @Field("proof_type") val proofType: String,
)

@Serializable
data class ZkpResponse(
    val id: String,
    val kid: String,
    val kty: String, 
    val crv: String,
    val x: String,
    val y: String,
)
