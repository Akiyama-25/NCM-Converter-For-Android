package com.example.ncmconverter.metadata

import com.example.ncmconverter.decrypt.model.NcmMetadata

interface MetadataWriter {
    suspend fun write(audioData: ByteArray, metadata: NcmMetadata): ByteArray

    suspend fun write(audioData: ByteArray, metadata: NcmMetadata, lyric: String?): ByteArray {
        return write(audioData, metadata)
    }
}
