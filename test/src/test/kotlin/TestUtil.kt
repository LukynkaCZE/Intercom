import cz.lukynka.intercom.common.IntercomRegistry
import cz.lukynka.intercom.common.protocol.IntercomSerializer
import packets.ClientboundTestHelloPacket
import packets.ServerboundTestHiPacket

object TestUtil {
    val registry = IntercomRegistry()
    const val AUTH_TOKEN = "123abc"

    init {
        registry.add("hello", ClientboundTestHelloPacket::class, IntercomSerializer.fromStreamCodec(ClientboundTestHelloPacket.STREAM_CODEC))
        registry.add("hi", ServerboundTestHiPacket::class, IntercomSerializer.fromStreamCodec(ServerboundTestHiPacket.STREAM_CODEC))
    }
}