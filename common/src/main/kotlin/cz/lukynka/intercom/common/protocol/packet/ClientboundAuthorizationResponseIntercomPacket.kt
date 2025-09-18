package cz.lukynka.intercom.common.protocol.packet

import io.github.dockyardmc.tide.stream.StreamCodec
import cz.lukynka.intercom.common.protocol.IntercomPacket

data class ClientboundAuthorizationResponseIntercomPacket(val status: Status): IntercomPacket {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.enum(), ClientboundAuthorizationResponseIntercomPacket::status,
            ::ClientboundAuthorizationResponseIntercomPacket
        )
    }

    enum class Status {
        SUCCESS,
        INVALID_TOKEN,
        ERROR
    }

}