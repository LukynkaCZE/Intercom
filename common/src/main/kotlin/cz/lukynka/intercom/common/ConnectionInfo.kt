package cz.lukynka.intercom.common

import cz.lukynka.intercom.common.handlers.IntercomPacketHandler
import cz.lukynka.intercom.common.handlers.sendPacket
import cz.lukynka.intercom.common.protocol.IntercomSerializable
import io.netty.channel.ChannelHandlerContext
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

data class ConnectionInfo(val messageHandler: IntercomPacketHandler, val connection: ChannelHandlerContext) {

    fun sendPacket(packet: IntercomSerializable) {
        connection.sendPacket(packet, messageHandler)
    }

    fun <Request : IntercomSerializable, Response : IntercomSerializable> sendRequest(request: Request, responseKclass: KClass<out Response>): CompletableFuture<Response> {
        return messageHandler.requestHandler.sendRequest(this, request, responseKclass)
    }
}