package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomPacket
import io.github.dockyardmc.tide.stream.StreamCodec

data class ClientboundDisconnectClientIntercomPacket(val reason: String) : IntercomPacket {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, ClientboundDisconnectClientIntercomPacket::reason,
            ::ClientboundDisconnectClientIntercomPacket
        )
    }
}