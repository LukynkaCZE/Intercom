package cz.lukynka.intercom.common.protocol.packet

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec

class ClientboundEncryptionFinish : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(::ClientboundEncryptionFinish)
    }
}