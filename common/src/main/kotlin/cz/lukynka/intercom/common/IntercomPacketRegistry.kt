package cz.lukynka.intercom.common

import io.netty.channel.ChannelHandlerContext
import cz.lukynka.intercom.common.protocol.IntercomPacket
import cz.lukynka.intercom.common.protocol.IntercomSerializer
import cz.lukynka.intercom.common.protocol.packet.ClientboundAuthorizationResponseIntercomPacket
import cz.lukynka.intercom.common.protocol.packet.ClientboundRegisterResponseIntercomPacket
import cz.lukynka.intercom.common.protocol.packet.ServerboundAuthorizationRequestIntercomPacket
import cz.lukynka.intercom.common.protocol.packet.ServerboundRegisterIntercomClientPacket
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class IntercomPacketRegistry {

    private val _classToPacket: MutableMap<KClass<out IntercomPacket>, IntercomPacketData<*>> = mutableMapOf()
    private val _identifierToPacket: MutableMap<String, IntercomPacketData<*>> = mutableMapOf()

    private val _handlers: MutableMap<KClass<out IntercomPacket>, (IntercomPacket, ChannelHandlerContext) -> Unit> = mutableMapOf()

    init {
        add("register_client", ServerboundRegisterIntercomClientPacket::class, IntercomSerializer.fromStreamCodec(ServerboundRegisterIntercomClientPacket.STREAM_CODEC))
        add("register_client_response", ClientboundRegisterResponseIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundRegisterResponseIntercomPacket.STREAM_CODEC))
        add("authorize_request", ServerboundAuthorizationRequestIntercomPacket::class, IntercomSerializer.fromStreamCodec(ServerboundAuthorizationRequestIntercomPacket.STREAM_CODEC))
        add("authorize_response", ClientboundAuthorizationResponseIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundAuthorizationResponseIntercomPacket.STREAM_CODEC))
    }

    fun <T> add(name: String, kClass: KClass<out IntercomPacket>, serializer: IntercomSerializer<T>) {
        val data = IntercomPacketData<T>(name, kClass, serializer)
        _classToPacket[kClass] = data
        _identifierToPacket[name] = data
    }

    fun <T> getByClass(kClass: KClass<out IntercomPacket>): IntercomPacketData<T> {
        return (_classToPacket[kClass] as IntercomPacketData<T>?) ?: throw IllegalStateException("Class ${kClass.simpleName} is not registered in the packet registry")
    }

    fun <T> getByIdentifier(identifier: String): IntercomPacketData<T> {
        return (_identifierToPacket[identifier] as IntercomPacketData<T>?) ?: throw IllegalStateException("Packet with identifier $identifier is not registered in the packet registry")
    }

    fun getHandler(kClass: KClass<out IntercomPacket>): ((IntercomPacket, ChannelHandlerContext) -> Unit)? {
        return _handlers[kClass]
    }

    data class IntercomPacketData<T>(val identifier: String, val kClass: KClass<out IntercomPacket>, val serializer: IntercomSerializer<T>)
}