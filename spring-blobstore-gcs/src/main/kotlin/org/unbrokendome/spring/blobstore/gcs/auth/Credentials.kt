package org.unbrokendome.spring.blobstore.gcs.auth

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.springframework.core.io.Resource
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.URI
import java.security.PrivateKey
import javax.annotation.WillNotClose


private const val DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token"


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ServiceAccountCredentials::class, name = "service_account"),
    JsonSubTypes.Type(AuthorizedUserCredentials::class, name = "authorized_user")
)
interface Credentials {

    val tokenUri: URI

    companion object {

        @JvmStatic
        fun serviceAccount(): ServiceAccountCredentialsBuilder =
            ServiceAccountCredentials.Builder()

        @JvmStatic
        fun authorizedUser(): AuthorizedUserCredentialsBuilder =
            AuthorizedUserCredentials.Builder()

        @JvmStatic
        fun fromJson(resource: Resource, objectMapper: ObjectMapper? = null): Credentials =
            resource.inputStream.use { input ->
                (objectMapper ?: ObjectMapper()).readerFor(Credentials::class.java)
                    .readValue(input)
            }

        @JvmStatic
        fun fromJson(json: String, objectMapper: ObjectMapper? = null): Credentials =
            (objectMapper ?: ObjectMapper()).readerFor(Credentials::class.java)
                .readValue(json)
    }
}


interface CredentialsBuilder<B : CredentialsBuilder<B>> {

    fun withTokenUri(tokenUri: URI): B

    fun withClientId(clientId: String): B

    fun build(): Credentials
}


@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
@Suppress("UNCHECKED_CAST")
internal abstract class AbstractCredentialsBuilder<B : CredentialsBuilder<B>>
    : CredentialsBuilder<B> {

    protected var tokenUri: URI = URI.create(DEFAULT_TOKEN_URI)
    protected lateinit var clientId: String

    @JsonProperty(required = false)
    override fun withTokenUri(tokenUri: URI): B = apply {
        this.tokenUri = tokenUri
    } as B

    @JsonProperty(required = true)
    override fun withClientId(clientId: String) = apply {
        this.clientId = clientId
    } as B
}


interface ServiceAccountCredentialsBuilder : CredentialsBuilder<ServiceAccountCredentialsBuilder> {

    fun withClientEmail(clientEmail: String): ServiceAccountCredentialsBuilder


    fun withPrivateKey(privateKey: PrivateKey): ServiceAccountCredentialsBuilder


    @JvmDefault
    fun withPrivateKey(privateKeyPem: String): ServiceAccountCredentialsBuilder =
        withPrivateKey(StringReader(privateKeyPem))


    @JvmDefault
    @JsonIgnore
    fun withPrivateKey(@WillNotClose privateKeyPemReader: Reader): ServiceAccountCredentialsBuilder {
        val pemObject = PEMParser(privateKeyPemReader).readObject()

        val converter = JcaPEMKeyConverter()

        val privateKey = when (pemObject) {
            is PrivateKeyInfo ->
                converter.getPrivateKey(pemObject)
            is PEMKeyPair ->
                converter.getKeyPair(pemObject).private
            else ->
                throw IllegalArgumentException("Private key must be a PEM-encoded private key or key pair")
        }

        return withPrivateKey(privateKey)
    }

    @JvmDefault
    @JsonIgnore
    fun withPrivateKey(@WillNotClose privateKeyPemInput: InputStream): ServiceAccountCredentialsBuilder =
        withPrivateKey(InputStreamReader(privateKeyPemInput))


    @JvmDefault
    @JsonIgnore
    fun withPrivateKey(privateKeyPemResource: Resource): ServiceAccountCredentialsBuilder =
        privateKeyPemResource.inputStream.use { input -> withPrivateKey(input) }


    fun withPrivateKeyId(privateKeyId: String): ServiceAccountCredentialsBuilder

    fun withScopes(scopes: Iterable<String>): ServiceAccountCredentialsBuilder

    @JvmDefault
    @JsonIgnore
    fun withScopes(vararg scopes: String) =
        withScopes(scopes.toSet())
}


interface AuthorizedUserCredentialsBuilder : CredentialsBuilder<AuthorizedUserCredentialsBuilder> {

    fun withClientSecret(clientSecret: String): AuthorizedUserCredentialsBuilder

    fun withRefreshToken(refreshToken: String): AuthorizedUserCredentialsBuilder
}


@JsonDeserialize(builder = ServiceAccountCredentials.Builder::class)
internal data class ServiceAccountCredentials(
    override val tokenUri: URI,
    val clientId: String,
    val clientEmail: String,
    val privateKeyId: String,
    val privateKey: PrivateKey,
    val scopes: Set<String> = emptySet()
) : Credentials {


    fun withScopes(scopes: Set<String>) =
        copy(scopes = this.scopes + scopes)


    @JsonPOJOBuilder
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Builder : AbstractCredentialsBuilder<ServiceAccountCredentialsBuilder>(), ServiceAccountCredentialsBuilder {

        private lateinit var clientEmail: String
        private lateinit var privateKey: PrivateKey
        private lateinit var privateKeyId: String
        private var scopes: Set<String> = emptySet()

        @JsonProperty(required = true)
        override fun withClientEmail(clientEmail: String) = apply {
            this.clientEmail = clientEmail
        }

        @JsonIgnore
        override fun withPrivateKey(privateKey: PrivateKey) = apply {
            this.privateKey = privateKey
        }

        @JsonProperty(required = true)
        override fun withPrivateKey(privateKeyPem: String) =
            super.withPrivateKey(privateKeyPem)

        @JsonProperty(required = true)
        override fun withPrivateKeyId(privateKeyId: String) = apply {
            this.privateKeyId = privateKeyId
        }

        @JsonProperty(required = false)
        override fun withScopes(scopes: Iterable<String>) = apply {
            this.scopes += scopes
        }

        override fun build(): ServiceAccountCredentials =
            ServiceAccountCredentials(
                tokenUri = tokenUri,
                clientId = clientId,
                clientEmail = clientEmail,
                privateKeyId = privateKeyId,
                privateKey = privateKey
            )
    }
}


internal data class AuthorizedUserCredentials(
    override val tokenUri: URI,
    val clientId: String,
    val clientSecret: String,
    val refreshToken: String
) : Credentials {

    @JsonPOJOBuilder
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Builder : AbstractCredentialsBuilder<AuthorizedUserCredentialsBuilder>(), AuthorizedUserCredentialsBuilder {

        private lateinit var clientSecret: String
        private lateinit var refreshToken: String

        @JsonProperty(required = true)
        override fun withClientSecret(clientSecret: String) = apply {
            this.clientSecret = clientSecret
        }

        @JsonProperty(required = true)
        override fun withRefreshToken(refreshToken: String) = apply {
            this.refreshToken = refreshToken
        }

        override fun build(): AuthorizedUserCredentials =
            AuthorizedUserCredentials(
                tokenUri = tokenUri,
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )
    }
}
