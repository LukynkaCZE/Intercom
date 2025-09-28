package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec

data class ClientboundDisconnectClientIntercomPacket(val reason: String) : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, ClientboundDisconnectClientIntercomPacket::reason,
            ::ClientboundDisconnectClientIntercomPacket
        )
    }
}