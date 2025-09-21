package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomPacket
import io.github.dockyardmc.tide.stream.StreamCodec

class ClientboundEncryptionFinish : IntercomPacket {
    companion object {
        val STREAM_CODEC = StreamCodec.of(::ClientboundEncryptionFinish)
    }
}