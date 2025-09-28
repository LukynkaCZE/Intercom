package cz.lukynka.intercom.common.protocol.packet

import io.github.dockyardmc.tide.stream.StreamCodec
import cz.lukynka.intercom.common.protocol.IntercomSerializable

data class ServerboundAuthorizationRequestIntercomPacket(val authToken: String) : IntercomSerializable {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, ServerboundAuthorizationRequestIntercomPacket::authToken,
            ::ServerboundAuthorizationRequestIntercomPacket
        )
    }

    override fun toString(): String {
        return "ServerboundAuthorizationRequestIntercomPacket(authToken=********)"
    }

}