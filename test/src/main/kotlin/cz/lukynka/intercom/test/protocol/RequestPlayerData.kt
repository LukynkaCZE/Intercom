package cz.lukynka.intercom.test.protocol

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec

data class RequestPlayerData(val username: String) : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, RequestPlayerData::username,
            ::RequestPlayerData
        )
    }
}

