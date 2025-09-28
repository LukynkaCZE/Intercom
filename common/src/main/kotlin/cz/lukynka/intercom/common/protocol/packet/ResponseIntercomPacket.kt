package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf

data class ResponseIntercomPacket(val id: Int, val response: ByteBuf) : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.VAR_INT, ResponseIntercomPacket::id,
            StreamCodec.BYTE_ARRAY, ResponseIntercomPacket::response,
            ::ResponseIntercomPacket
        )
    }
}