package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomPacket
import io.github.dockyardmc.tide.stream.StreamCodec

data class ServerboundHandshakePacket(val serverName: String, val protocolVersion: Int) : IntercomPacket {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, ServerboundHandshakePacket::serverName,
            StreamCodec.VAR_INT, ServerboundHandshakePacket::protocolVersion,
            ::ServerboundHandshakePacket
        )
    }
}