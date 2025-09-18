package cz.lukynka.intercom.common.protocol.packet

import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf

data class WrappedIntercomPacket(val identifier: String, val data: ByteBuf) {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.STRING, WrappedIntercomPacket::identifier,
            StreamCodec.BYTE_ARRAY, WrappedIntercomPacket::data,
            ::WrappedIntercomPacket
        )
    }
}