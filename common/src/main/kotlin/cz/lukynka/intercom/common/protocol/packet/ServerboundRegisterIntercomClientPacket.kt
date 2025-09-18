package cz.lukynka.intercom.common.protocol.packet

import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf
import cz.lukynka.intercom.common.protocol.IntercomPacket

data class ServerboundRegisterIntercomClientPacket(
    val clientVersion: Int,
    val publicKey: ByteBuf,
    val verificationToken: ByteBuf
) : IntercomPacket {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.VAR_INT, ServerboundRegisterIntercomClientPacket::clientVersion,
            StreamCodec.BYTE_ARRAY, ServerboundRegisterIntercomClientPacket::publicKey,
            StreamCodec.BYTE_ARRAY, ServerboundRegisterIntercomClientPacket::verificationToken,
            ::ServerboundRegisterIntercomClientPacket
        )
    }
}