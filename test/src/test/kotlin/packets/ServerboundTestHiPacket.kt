package packets

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.github.dockyardmc.tide.stream.StreamCodec

data class ServerboundTestHiPacket(val sequence: Int) : IntercomSerializable {
    companion object {
        val STREAM_CODEC = StreamCodec.of(
            StreamCodec.VAR_INT, ServerboundTestHiPacket::sequence,
            ::ServerboundTestHiPacket
        )
    }
}