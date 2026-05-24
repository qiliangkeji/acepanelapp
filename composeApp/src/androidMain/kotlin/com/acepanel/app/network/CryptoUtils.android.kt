package com.acepanel.app.network

import android.util.Base64
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun sha256Hex(data: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(data).joinToString("") { "%02x".format(it) }
}

actual fun hmacSha256Hex(key: ByteArray, data: ByteArray): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(data).joinToString("") { "%02x".format(it) }
}

/**
 * RSA-OAEP + SHA-512（OAEP hash 和 MGF1 均使用 SHA-512）
 * 对应 Go 后端：rsa.EncryptOAEP(sha512.New(), rand.Reader, publicKey, data, nil)
 * 公钥格式：PKIX/SubjectPublicKeyInfo (BEGIN PUBLIC KEY)
 */
actual fun rsaEncryptOaepSha512(publicKeyPem: String, plaintext: ByteArray): String {
    val pem = publicKeyPem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\n", "")
        .replace("\r", "")
        .trim()
    val keyBytes = Base64.decode(pem, Base64.DEFAULT)
    val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-512AndMGF1Padding")
    // OAEPParameterSpec/PSource 在 Android KMP 编译类路径中不可见，通过反射在运行时构造
    // 确保 OAEP hash 和 MGF1 hash 均为 SHA-512，与 Go 后端一致
    val oaepSpec = runCatching {
        val psDefault = Class.forName("java.security.spec.PSource\$PSpecified")
            .getField("DEFAULT").get(null)
        Class.forName("java.security.spec.OAEPParameterSpec")
            .getConstructor(
                String::class.java, String::class.java,
                AlgorithmParameterSpec::class.java,
                Class.forName("java.security.spec.PSource")
            )
            .newInstance("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, psDefault)
                as AlgorithmParameterSpec
    }.getOrNull()
    if (oaepSpec != null) cipher.init(Cipher.ENCRYPT_MODE, key, oaepSpec)
    else cipher.init(Cipher.ENCRYPT_MODE, key)
    return Base64.encodeToString(cipher.doFinal(plaintext), Base64.NO_WRAP)
}

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000L
