package cz.lukynka.intercom.common

import cz.lukynka.intercom.common.protocol.IntercomSerializable
import cz.lukynka.intercom.common.protocol.IntercomSerializer
import cz.lukynka.intercom.common.protocol.packet.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class IntercomRegistry {

    private val _classToPacket: MutableMap<KClass<out IntercomSerializable>, IntercomSerializerData<*>> = mutableMapOf()
    private val _identifierToPacket: MutableMap<String, IntercomSerializerData<*>> = mutableMapOf()

    private val _packetHandlers: MutableMap<KClass<out IntercomSerializable>, (IntercomSerializable, ConnectionInfo) -> Unit> = mutableMapOf()
    private val _requestHandlers: MutableMap<KClass<out IntercomSerializable>, (IntercomSerializable, ConnectionInfo) -> IntercomSerializable> = mutableMapOf()

    init {
        add("client_handshake", ServerboundHandshakePacket::class, IntercomSerializer.fromStreamCodec(ServerboundHandshakePacket.STREAM_CODEC))
        add("server_disconnect", ClientboundDisconnectClientIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundDisconnectClientIntercomPacket.STREAM_CODEC))
        add("encryption_request", ClientboundEncryptionRequestIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundEncryptionRequestIntercomPacket.STREAM_CODEC))
        add("encryption_response", ServerboundEncryptionResponseIntercomPacket::class, IntercomSerializer.fromStreamCodec(ServerboundEncryptionResponseIntercomPacket.STREAM_CODEC))
        add("encryption_finish", ClientboundEncryptionFinish::class, IntercomSerializer.fromStreamCodec(ClientboundEncryptionFinish.STREAM_CODEC))

        add("authorize_request", ServerboundAuthorizationRequestIntercomPacket::class, IntercomSerializer.fromStreamCodec(ServerboundAuthorizationRequestIntercomPacket.STREAM_CODEC))
        add("authorize_response", ClientboundAuthorizationResponseIntercomPacket::class, IntercomSerializer.fromStreamCodec(ClientboundAuthorizationResponseIntercomPacket.STREAM_CODEC))

        add("request", RequestIntercomPacket::class, IntercomSerializer.fromStreamCodec(RequestIntercomPacket.STREAM_CODEC))
        add("response", ResponseIntercomPacket::class, IntercomSerializer.fromStreamCodec(ResponseIntercomPacket.STREAM_CODEC))
    }

    fun <T> add(name: String, kClass: KClass<out IntercomSerializable>, serializer: IntercomSerializer<T>) {
        if (_identifierToPacket.containsKey(name)) throw IllegalStateException("Packet with identifier $name is already registered in the packet registry")
        if (_classToPacket.containsKey(kClass)) throw IllegalStateException("Class ${kClass.simpleName} is already registered in the packet registry")
        val data = IntercomSerializerData<T>(name, kClass, serializer)
        _classToPacket[kClass] = data
        _identifierToPacket[name] = data
    }

    fun <T> getByClass(kClass: KClass<out IntercomSerializable>): IntercomSerializerData<T> {
        return (_classToPacket[kClass] as IntercomSerializerData<T>?) ?: throw IllegalStateException("Class ${kClass.simpleName} is not registered in the packet registry")
    }

    fun <T> getByIdentifier(identifier: String): IntercomSerializerData<T> {
        return (_identifierToPacket[identifier] as IntercomSerializerData<T>?) ?: throw IllegalStateException("Packet with identifier $identifier is not registered in the packet registry")
    }

    fun getPacketHandlers(kClass: KClass<out IntercomSerializable>): ((IntercomSerializable, ConnectionInfo) -> Unit)? {
        return _packetHandlers[kClass]
    }

    fun getRequestHandler(kClass: KClass<out IntercomSerializable>): ((IntercomSerializable, ConnectionInfo) -> IntercomSerializable)? {
        return _requestHandlers[kClass]
    }

    fun <T : IntercomSerializable> addPacketHandler(kClass: KClass<T>, handler: (T, ConnectionInfo) -> Unit) {
        if (_packetHandlers.containsKey(kClass)) return
        _packetHandlers[kClass] = handler as (IntercomSerializable, ConnectionInfo) -> Unit
    }

    fun <Request : IntercomSerializable, Response : IntercomSerializable> addRequestHandler(requestClass: KClass<out Request>, handler: (Request, ConnectionInfo) -> Response) {
        if (_requestHandlers.containsKey(requestClass)) throw IllegalStateException("Request handler for ${requestClass.simpleName} is already registered")
        _requestHandlers[requestClass] = handler as (IntercomSerializable, ConnectionInfo) -> IntercomSerializable
    }

    data class IntercomSerializerData<T>(val identifier: String, val kClass: KClass<out IntercomSerializable>, val serializer: IntercomSerializer<T>)
}