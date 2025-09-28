package cz.lukynka.intercom.common.handlers

import cz.lukynka.intercom.common.ConnectionInfo
import cz.lukynka.intercom.common.protocol.IntercomSerializable
import cz.lukynka.intercom.common.protocol.IntercomSerializer
import cz.lukynka.intercom.common.protocol.packet.RequestIntercomPacket
import cz.lukynka.intercom.common.protocol.packet.ResponseIntercomPacket
import cz.lukynka.intercom.common.request.IntercomRequest
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.netty.buffer.Unpooled
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class RequestHandler(val handler: IntercomPacketHandler) {

    val requestId = AtomicInteger(0)

    private val requestByMessage: MutableMap<Int, IntercomRequest<out IntercomSerializable, out IntercomSerializable>> = mutableMapOf()

    fun <Request : IntercomSerializable, Response : IntercomSerializable> sendRequest(connectionInfo: ConnectionInfo, request: Request, responseKclass: KClass<out Response>): CompletableFuture<Response> {
        val requestSerializerData = handler.registry.getByClass<Request>(request::class)
        val responseSerializerData = handler.registry.getByClass<Response>(responseKclass)
        val intercomRequest = IntercomRequest(request, responseSerializerData)
        val id = requestId.getAndIncrement()
        requestByMessage[id] = intercomRequest
        val buffer = Unpooled.buffer()
        requestSerializerData.serializer.write(buffer, request)
        connectionInfo.sendPacket(RequestIntercomPacket(id, requestSerializerData.identifier, buffer))
        return intercomRequest.future
    }

    fun handleRequest(request: RequestIntercomPacket, connectionInfo: ConnectionInfo) {
        val serializerData = handler.registry.getByIdentifier<IntercomSerializable>(request.identifier)
        val decoded = serializerData.serializer.read(request.request)

        val requestHandler = handler.registry.getRequestHandler(decoded::class) ?: throw IllegalStateException("There is not handler for request ${decoded::class.simpleName}")
        val response = requestHandler.invoke(decoded, connectionInfo)
        val responseSerializer = handler.registry.getByClass<IntercomSerializable>(response::class)
        val buffer = Unpooled.buffer()
        responseSerializer.serializer.write(buffer, response)
        connectionInfo.sendPacket(ResponseIntercomPacket(request.id, buffer))
    }

    fun handleResponse(response: ResponseIntercomPacket, connectionInfo: ConnectionInfo) {
        val request = requestByMessage.remove(response.id)
        if (request == null) {
            log("Received unexpected response with id ${response.id}", LogType.ERROR)
            return
        }

        val serializer = request.responseSerializerData.serializer as IntercomSerializer<IntercomSerializable>
        val decoded = serializer.read(response.response)
        (request.future as CompletableFuture<IntercomSerializable>).complete(decoded)
    }
}