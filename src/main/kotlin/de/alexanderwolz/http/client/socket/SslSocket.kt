package de.alexanderwolz.http.client.socket

import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

data class SslSocket(val sslSocketFactory: SSLSocketFactory, val trustManager: X509TrustManager)