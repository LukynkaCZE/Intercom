package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf

data class ServerboundEncryptionResponseIntercomPacket(
    val sharedSecret: ByteBuf,
    val verificationToken: ByteBuf
) : IntercomSerializable {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.BYTE_ARRAY, ServerboundEncryptionResponseIntercomPacket::sharedSecret,
            StreamCodec.BYTE_ARRAY, ServerboundEncryptionResponseIntercomPacket::verificationToken,
            ::ServerboundEncryptionResponseIntercomPacket
        )
    }
}