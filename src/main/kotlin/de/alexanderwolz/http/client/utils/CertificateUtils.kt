package de.alexanderwolz.http.client.utils

import de.alexanderwolz.commons.log.Logger
import de.alexanderwolz.http.client.model.certificate.CertificateBundle
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v1CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.util.*
import javax.security.auth.x500.X500Principal


object CertificateUtils {

    //INFO: by default public key data uses X509 encoding, private key data uses PKCS8 encoding.

    private const val HEADER_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----"
    private const val FOOTER_PRIVATE_KEY = "-----END PRIVATE KEY-----"
    private const val HEADER_EC_PRIVATE_KEY = "-----BEGIN EC PRIVATE KEY-----"
    private const val FOOTER_EC_PRIVATE_KEY = "-----END EC PRIVATE KEY-----"
    private const val HEADER_CERTIFICATE = "-----BEGIN CERTIFICATE-----"
    private const val FOOTER_CERTIFICATE = "-----END CERTIFICATE-----"

    private val logger = Logger(javaClass)

    init {
        //we need this provider, as ECDH algorithms are not included in default Java Security Package
        Security.addProvider(BouncyCastleProvider())
    }

    fun readPrivateKey(file: File): PrivateKey {
        return getPrivateKey(file.readText(Charsets.UTF_8)).also {
            logger.debug {
                val builder = StringBuilder("Successfully loaded private key:")
                builder.append("\n  File: \t\t${file.name}")
                builder.append("\n  Format:\t\t${it.format}")
                builder.append("\n  Algorithm:\t${it.algorithm}")
                builder.toString()
            }
        }
    }

    fun readPrivateKey(pem: String): PrivateKey {
        return getPrivateKey(pem).also {
            logger.debug {
                val builder = StringBuilder("Successfully loaded private key:")
                builder.append("\n  Format:\t\t${it.format}")
                builder.append("\n  Algorithm:\t${it.algorithm}")
                builder.toString()
            }
        }
    }

    fun readCertificates(vararg binaries: ByteArray): List<X509Certificate> {

        val cf = CertificateFactory.getInstance("X.509")
        val certificates = ArrayList<X509Certificate>()
        binaries.forEach { binary ->
            ByteArrayInputStream(binary).use {
                while (it.available() > 0) {
                    val certificate = cf.generateCertificate(it) as X509Certificate
                    certificates.add(certificate)
                }
            }
        }

        val builder = StringBuilder("Successfully loaded certificate(s):")

        certificates.forEachIndexed { index, certificate ->
            if (index > 0) {
                builder.append("\n  --------------------------------------------------------")
            }
            builder.append("\n  Subject: \t\t${certificate.subjectX500Principal}")
            if (certificate.subjectX500Principal != certificate.issuerX500Principal) {
                builder.append("\n  Issuer: \t\t${certificate.issuerX500Principal}")
            }
            builder.append("\n  Type:\t\t\t${certificate.type}")
            builder.append("\n  Algorithm:\t${certificate.sigAlgName}")
            builder.append("\n  Valid From: \t${certificate.notBefore}")
            builder.append("\n  Valid To: \t${certificate.notAfter}")
        }
        logger.debug { builder.toString() }
        return certificates
    }

    fun readCertificates(folder: File, vararg fileNames: String): List<X509Certificate> {
        val cf = CertificateFactory.getInstance("X.509")
        val allCertificates = ArrayList<X509Certificate>()

        val builder = StringBuilder("Successfully loaded certificate(s):")

        val files = if (fileNames.isEmpty()) {
            listOf(folder) //we assume client wants to use folder as given file
        } else {
            fileNames.map { File(folder, it) }
        }

        files.forEachIndexed { index, file ->
            if (index > 0) {
                builder.append("\n    --------------------------------------------")
            }
            FileInputStream(file).buffered().use {
                val certificates = ArrayList<X509Certificate>()
                builder.append("\n  File: \t\t${file.name}")
                while (it.available() > 0) {
                    val certificate = cf.generateCertificate(it) as X509Certificate
                    certificates.add(certificate)
                }
                allCertificates.addAll(certificates)
                certificates.forEachIndexed { index, certificate ->
                    if (index > 0) {
                        builder.append("\n  --------------------------------------------------------")
                    }
                    builder.append("\n  Subject: \t\t${certificate.subjectX500Principal}")
                    if (certificate.subjectX500Principal != certificate.issuerX500Principal) {
                        builder.append("\n  Issuer: \t\t${certificate.issuerX500Principal}")
                    }
                    builder.append("\n  Type:\t\t\t${certificate.type}")
                    builder.append("\n  Algorithm:\t${certificate.sigAlgName}")
                    builder.append("\n  Valid From: \t${certificate.notBefore}")
                    builder.append("\n  Valid To: \t${certificate.notAfter}")
                }
            }
        }
        return allCertificates.also {
            logger.trace { builder.toString() }
        }
    }

    fun addCertificateHeaders(pem: String): String {
        return HEADER_CERTIFICATE + "\n" + pem + "\n" + FOOTER_CERTIFICATE
    }

    fun addPrivateKeyHeaders(pem: String): String {
        return HEADER_PRIVATE_KEY + "\n" + pem + "\n" + FOOTER_PRIVATE_KEY
    }

    fun addPrivateKeyHeaders(privateKey: PrivateKey): String {
        return addPrivateKeyHeaders(Base64.getEncoder().encode(privateKey.encoded).decodeToString())
    }

    fun removeHeadersAndFooters(pem: String): String {
        val plain = pem.replace(System.lineSeparator(), "").replace("", "").replace(HEADER_PRIVATE_KEY, "")
            .replace(FOOTER_PRIVATE_KEY, "").replace(HEADER_EC_PRIVATE_KEY, "").replace(FOOTER_EC_PRIVATE_KEY, "")
            .replace(HEADER_CERTIFICATE, "").replace(FOOTER_CERTIFICATE, "").replace(System.lineSeparator(), "")
            .replace("\n", "")
        return plain
    }

    fun getPrivateKey(pem: String): PrivateKey {
        if (isEllipticCurve(pem)) {
            val parsedPem = PEMParser(StringReader(pem)).readObject()
            if (parsedPem is PEMKeyPair) {
                return JcaPEMKeyConverter().getPrivateKey(parsedPem.privateKeyInfo)
            }
            throw IllegalArgumentException("Unsupported PEM type: $parsedPem")
        }

        //DEFAULT is RSA based key-pair
        val encoded = Base64.getDecoder().decode(removeHeadersAndFooters(pem))
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        return keyFactory.generatePrivate(keySpec)
    }

    private fun isEllipticCurve(pem: String): Boolean {
        return pem.startsWith("-----BEGIN EC PRIVATE KEY-----")
    }

    fun decodePem(encoded: ByteArray): String {
        return Base64.getDecoder().decode(encoded).decodeToString()
    }

    fun decodePem(encoded: String): String {
        return Base64.getDecoder().decode(encoded).decodeToString()
    }

    fun resolveKeyPairFile(file: File, certFolder: File? = null): File {
        if (file.exists()) {
            return file
        }
        certFolder?.let {
            File(certFolder, file.path).also {
                if (it.exists()) {
                    logger.trace { "Found specified file in certificate folder: ${it.path}" }
                    return it
                }
            }
            val root = File("").absoluteFile
            File(root, "${certFolder.path}/${file.path}").also {
                if (it.exists()) {
                    logger.trace { "Found specified file in certificate folder: ${it.path}" }
                    return it
                }
            }
        }
        throw NoSuchElementException("Could not resolve file: ${file.path}")
    }

    fun generateNewCertificatePair(subjectText: String, serial: BigInteger): CertificateBundle {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(RSAKeyGenParameterSpec(2048, BigInteger.valueOf(65537)))
        }.generateKeyPair()
        val privateKey = keyPair.private as RSAPrivateKey
        val publicKey = keyPair.public as RSAPublicKey

        val subject = X500Name(X500Principal(subjectText).name)

        val notBefore = Date()
        val calendar = Calendar.getInstance().apply {
            setTime(notBefore)
            add(Calendar.YEAR, 3)
        }
        val notAfter = calendar.time

        val publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(
            RSAKeyParameters(false, publicKey.modulus, publicKey.publicExponent)
        )

        val contentSigner = JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(privateKey)
        val holder = X509v1CertificateBuilder(
            subject, serial, notBefore, notAfter, subject, publicKeyInfo
        ).build(contentSigner)
        val certificate = JcaX509CertificateConverter().getCertificate(holder)

        return CertificateBundle(privateKey, listOf(certificate), emptyList())
    }

}