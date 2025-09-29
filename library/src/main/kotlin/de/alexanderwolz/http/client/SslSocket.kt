package de.alexanderwolz.http.de.alexanderwolz.http.client

import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

data class SslSocket(val sslSocketFactory: SSLSocketFactory, val trustManager: X509TrustManager)