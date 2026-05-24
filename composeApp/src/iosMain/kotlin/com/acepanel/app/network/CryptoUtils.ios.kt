package com.acepanel.app.network

import kotlinx.cinterop.*
import platform.CommonCrypto.*
import platform.Foundation.*
import platform.Security.*

actual fun sha256Hex(data: ByteArray): String {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    data.toUByteArray().usePinned { inputPin ->
        digest.usePinned { digestPin ->
            CC_SHA256(inputPin.addressOf(0), data.size.convert(), digestPin.addressOf(0))
        }
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

actual fun hmacSha256Hex(key: ByteArray, data: ByteArray): String {
    val result = UByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    key.toUByteArray().usePinned { keyPin ->
        data.toUByteArray().usePinned { dataPin ->
            result.usePinned { resultPin ->
                CCHmac(
                    kCCHmacAlgSHA256,
                    keyPin.addressOf(0),
                    key.size.convert(),
                    dataPin.addressOf(0),
                    data.size.convert(),
                    resultPin.addressOf(0)
                )
            }
        }
    }
    return result.joinToString("") { it.toString(16).padStart(2, '0') }
}

/**
 * RSA-OAEP(SHA-512) 加密，使用 Security.framework
 * kSecKeyAlgorithmRSAEncryptionOAEPSHA512 在 iOS/macOS 上对应 OAEP+SHA512
 */
actual fun rsaEncryptOaepSha512(publicKeyPem: String, plaintext: ByteArray): String {
    val pemStripped = publicKeyPem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\n", "")
        .replace("\r", "")
        .trim()

    val keyData = NSData.create(base64EncodedString = pemStripped, options = 0u)
        ?: error("RSA: 无法解码 Base64 公钥")

    val keyAttributes = mapOf<Any?, Any?>(
        kSecAttrKeyType as Any to kSecAttrKeyTypeRSA,
        kSecAttrKeyClass as Any to kSecAttrKeyClassPublic
    )

    memScoped {
        val keyErrorPtr = alloc<CFErrorRefVar>()
        val secKey = SecKeyCreateWithData(
            keyData as CFDataRef,
            keyAttributes as CFDictionaryRef,
            keyErrorPtr.ptr
        ) ?: run {
            val err = keyErrorPtr.value
            if (err != null) CFRelease(err)
            error("RSA: 无法从 PEM 创建 SecKey")
        }

        val plainData = plaintext.toUByteArray().usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = plaintext.size.convert())
        }

        val encErrorPtr = alloc<CFErrorRefVar>()
        val encrypted = SecKeyCreateEncryptedData(
            secKey,
            kSecKeyAlgorithmRSAEncryptionOAEPSHA512,
            plainData as CFDataRef,
            encErrorPtr.ptr
        )

        CFRelease(secKey)

        if (encrypted == null) {
            val err = encErrorPtr.value
            if (err != null) CFRelease(err)
            error("RSA: 加密失败")
        }

        val nsEncrypted = encrypted as NSData
        val result = nsEncrypted.base64EncodedStringWithOptions(0u)
        CFRelease(encrypted)
        return result
    }
}

actual fun currentTimeSeconds(): Long =
    platform.Foundation.NSDate.date().timeIntervalSince1970.toLong()
