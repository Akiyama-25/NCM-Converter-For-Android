package com.example.ncmconverter.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeteaseCryptoTest {

    @Test
    fun `weapi generates non-empty params and valid 256-hex encSecKey`() {
        val json = "{\"id\":12345,\"s\":\"test\"}"
        val form = NeteaseCrypto.weapi(json)

        assertTrue(form.params.isNotBlank())
        assertEquals(256, form.encSecKey.length)
        assertTrue(form.encSecKey.matches(Regex("^[0-9a-fA-F]{256}$")))
    }

    @Test
    fun `eapi generates uppercase hex string`() {
        val path = "/api/song/lyric"
        val payload = "{\"id\":12345}"
        val encrypted = NeteaseCrypto.eapi(path, payload)

        assertTrue(encrypted.isNotBlank())
        assertTrue(encrypted.length % 32 == 0)
        assertTrue(encrypted.matches(Regex("^[0-9A-F]+$")))
    }

    @Test
    fun `eapi decrypt valid ciphertext roundtrips`() {
        val path = "/api/test/path"
        val json = "{\"code\":200,\"message\":\"hello\"}"
        val encrypted = NeteaseCrypto.eapi(path, json)

        val decrypted = NeteaseCrypto.decryptEapiResponse(encrypted)
        assertNotNull(decrypted)
        assertTrue(decrypted!!.contains(path))
        assertTrue(decrypted.contains(json))
    }

    @Test
    fun `decryptEapiResponse returns null on invalid hex`() {
        assertNull(NeteaseCrypto.decryptEapiResponse("1234"))
        assertNull(NeteaseCrypto.decryptEapiResponse("ZZZZ"))
    }
}