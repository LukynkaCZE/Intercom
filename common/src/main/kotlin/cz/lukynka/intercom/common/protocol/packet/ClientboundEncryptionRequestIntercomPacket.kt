package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomPacket
import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf

data class ClientboundEncryptionRequestIntercomPacket(
    val publicKey: ByteBuf,
    val verifyToken: ByteBuf
) : IntercomPacket {

    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.BYTE_ARRAY, ClientboundEncryptionRequestIntercomPacket::publicKey,
            StreamCodec.BYTE_ARRAY, ClientboundEncryptionRequestIntercomPacket::verifyToken,
            ::ClientboundEncryptionRequestIntercomPacket
        )
    }

}