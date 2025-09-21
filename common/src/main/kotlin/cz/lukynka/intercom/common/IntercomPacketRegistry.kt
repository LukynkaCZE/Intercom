package cz.lukynka.intercom.common

import cz.lukynka.intercom.common.protocol.IntercomPacket
import cz.lukynka.intercom.common.protocol.IntercomSerializer
import cz.lukynka.intercom.common.protocol.packet.*
import io.netty.channel.ChannelHandlerContext
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class IntercomPacketRegistry {

    private val _classToPacket: MutableMap<KClass<out IntercomPacket>, IntercomPacketData<*>> = mutableMapOf()
    private val _identifierToPacket: MutableMap<String, IntercomPacketData<*>> = mutableMapOf()

    private val _handlers: MutableMap<KClass<out IntercomPacket>, (IntercomPacket, ChannelHandlerContext) -> Unit> = mutableMapOf()

    init {
        add("client_handshake", ServerboundHandshakePacket::class, IntercomSerializer.fromStreamCodec(ServerboundHandshakePacket.STREAM_CODEC))
        add("server_disconnect", ClientboundDisconnectClientIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundDisconnectClientIntercomPacket.STREAM_CODEC))
        add("encryption_request", ClientboundEncryptionRequestIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundEncryptionRequestIntercomPacket.STREAM_CODEC))
        add("encryption_response", ServerboundEncryptionResponseIntercomPacket::class, IntercomSerializer.fromStreamCodec(ServerboundEncryptionResponseIntercomPacket.STREAM_CODEC))
        add("encryption_finish", ClientboundEncryptionFinish::class, IntercomSerializer.fromStreamCodec(ClientboundEncryptionFinish.STREAM_CODEC))

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

    fun <T : IntercomPacket> addHandler(kClass: KClass<T>, handler: (T, ChannelHandlerContext) -> Unit) {
        _handlers[kClass] = handler as (IntercomPacket, ChannelHandlerContext) -> Unit
    }

    data class IntercomPacketData<T>(val identifier: String, val kClass: KClass<out IntercomPacket>, val serializer: IntercomSerializer<T>)
}