package de.alexanderwolz.http.client

import de.alexanderwolz.http.client.log.Logger
import de.alexanderwolz.http.client.model.CertificateBundle
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class HttpClient {

    private val logger = Logger(javaClass)

    private val okHttpClient = createOkHttpClient()

    private fun createOkHttpClient(bundle: CertificateBundle? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()

        val proxyURI = URI.create("http://localhost:9090")
        val type = Proxy.Type.valueOf(proxyURI.scheme.uppercase())
        val proxy = Proxy(type, InetSocketAddress(proxyURI.host, proxyURI.port))
        builder.proxy(proxy)

        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        bundle?.let {
            val sslSocket = createCustomSslSocketFactory(it)
            builder.sslSocketFactory(sslSocket.sslSocketFactory, sslSocket.trustManager)
        }
        return builder.build()
    }

    private fun createCustomSslSocketFactory(bundle: CertificateBundle): SslSocket {

        val verify = false

        //generate custom key store
        val keyManagers = createCustomKeyStore(bundle.certificates, bundle.privateKey)

        //generate custom trust managers
        val trustManagers = createCustomTrustStore(bundle.caCertificates, verify)

        logger.trace { "Creating SSL socket factory .." }
        val socketFactory = SSLContext.getInstance("TLS").apply {
            init(keyManagers, trustManagers, SecureRandom())
        }.socketFactory

        logger.trace { "Done creating SSL socket factory" }
        return SslSocket(socketFactory, trustManagers[0] as X509TrustManager)

    }

    private fun createCustomKeyStore(certificates: List<Certificate>, privateKey: PrivateKey): Array<KeyManager> {
        return KeyManagerFactory.getInstance("SunX509").apply {
            logger.trace { "Creating keystore" }
            val passArray = "keystore".toCharArray()
            val keyStore = createEmptyKeyStore(passArray).apply {
                certificates.forEachIndexed { index, certificate ->
                    setCertificateEntry("certificate-$index", certificate)
                }
                setKeyEntry("private-key", privateKey, passArray, certificates.toTypedArray())
            }
            init(keyStore, passArray)
        }.keyManagers
    }

    private fun createCustomTrustStore(caCertificates: List<Certificate>, verify: Boolean = true): Array<TrustManager> {
        return if (verify) {
            logger.trace { "Creating ca truststore (verify=true)" }
            //generate custom trust store
            TrustManagerFactory.getInstance("SunX509").apply {
                val trustStore = createEmptyKeyStore("truststore".toCharArray()).apply {
                    caCertificates.forEachIndexed { index, certificate ->
                        setCertificateEntry("ca-${index + 1}", certificate)
                    }
                }
                init(trustStore)
            }.trustManagers
        } else {
            logger.trace { "Creating mock-truststore (verify=false)" }
            arrayOf(createAllowAllTrustManager())
        }
    }

    private fun createEmptyKeyStore(password: CharArray): KeyStore {
        return KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, password)
        }
    }

    private fun createAllowAllTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

}