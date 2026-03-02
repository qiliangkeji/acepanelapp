package org.example.project.network

/** SHA-256 哈希，返回十六进制字符串 */
expect fun sha256Hex(data: ByteArray): String

/** HMAC-SHA256，返回十六进制字符串 */
expect fun hmacSha256Hex(key: ByteArray, data: ByteArray): String

/** RSA-OAEP(SHA-512) 加密，返回 Base64 字符串（用于登录密码加密） */
expect fun rsaEncryptOaepSha512(publicKeyPem: String, plaintext: ByteArray): String

/** 获取当前 Unix 秒级时间戳 */
expect fun currentTimeSeconds(): Long
