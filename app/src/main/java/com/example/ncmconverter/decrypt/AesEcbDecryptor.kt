package com.example.ncmconverter.decrypt

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object AesEcbDecryptor {

    private const val ALGORITHM = "AES/ECB/PKCS7Padding"

    fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }
}
