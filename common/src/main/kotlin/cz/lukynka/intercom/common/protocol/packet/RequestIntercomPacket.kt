package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf

data class RequestIntercomPacket(val id: Int, val identifier: String, val request: ByteBuf) : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.VAR_INT, RequestIntercomPacket::id,
            StreamCodec.STRING, RequestIntercomPacket::identifier,
            StreamCodec.BYTE_ARRAY, RequestIntercomPacket::request,
            ::RequestIntercomPacket
        )
    }
}