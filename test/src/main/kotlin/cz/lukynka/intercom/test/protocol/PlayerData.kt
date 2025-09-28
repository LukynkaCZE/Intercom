package cz.lukynka.intercom.test.protocol

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec

data class PlayerData(val username: String, val level: Int, val xp: Int) : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, PlayerData::username,
            StreamCodec.VAR_INT, PlayerData::level,
            StreamCodec.VAR_INT, PlayerData::xp,
            ::PlayerData
        )
    }
}