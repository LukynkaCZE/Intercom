package cz.lukynka.intercom.common.protocol.packet

import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf
import cz.lukynka.intercom.common.protocol.IntercomPacket

data class ClientboundRegisterResponseIntercomPacket(
    val serverVersion: Int,
    val sharedSecret: ByteBuf,
    val verificationToken: ByteBuf
) : IntercomPacket {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.VAR_INT, ClientboundRegisterResponseIntercomPacket::serverVersion,
            StreamCodec.BYTE_ARRAY, ClientboundRegisterResponseIntercomPacket::sharedSecret,
            StreamCodec.BYTE_ARRAY, ClientboundRegisterResponseIntercomPacket::verificationToken,
            ::ClientboundRegisterResponseIntercomPacket
        )
    }

}