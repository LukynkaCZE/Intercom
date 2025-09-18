package cz.lukynka.intercom.common.protocol.packet

import io.github.dockyardmc.tide.stream.StreamCodec
import cz.lukynka.intercom.common.protocol.IntercomPacket

data class ServerboundAuthorizationRequestIntercomPacket(val authToken: String) : IntercomPacket {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, ServerboundAuthorizationRequestIntercomPacket::authToken,
            ::ServerboundAuthorizationRequestIntercomPacket
        )
    }

}