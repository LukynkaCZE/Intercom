package cz.lukynka.intercom.common.handlers

import cz.lukynka.bindables.BindablePool
import cz.lukynka.intercom.common.ConnectionInfo
import cz.lukynka.intercom.common.IntercomRegistry
import cz.lukynka.intercom.common.protocol.IntercomSerializable
import cz.lukynka.intercom.common.protocol.packet.RequestIntercomPacket
import cz.lukynka.intercom.common.protocol.packet.ResponseIntercomPacket
import cz.lukynka.intercom.common.protocol.packet.WrappedIntercomPacket
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

@Sharable
class IntercomPacketHandler(val registry: IntercomRegistry) : ChannelInboundHandlerAdapter() {

    constructor(supplier: () -> IntercomRegistry) : this(supplier.invoke())

    var encryptionEnabled = false

    val bindablePool = BindablePool()

    val channelActiveDispatcher = bindablePool.provideBindableDispatcher<ChannelHandlerContext>()
    val channelInactiveDispatcher = bindablePool.provideBindableDispatcher<ChannelHandlerContext>()
    val packetReceived = bindablePool.provideBindableDispatcher<Pair<IntercomSerializable, ChannelHandlerContext>>()
    val exceptionThrown = bindablePool.provideBindableDispatcher<Pair<Throwable, ChannelHandlerContext>>()

    val requestHandler = RequestHandler(this)

    init {
        registry.addPacketHandler(RequestIntercomPacket::class) { packet, connection ->
            requestHandler.handleRequest(packet, connection)
        }

        registry.addPacketHandler(ResponseIntercomPacket::class) { packet, connection ->
            requestHandler.handleResponse(packet, connection)
        }
    }

    override fun channelActive(connection: ChannelHandlerContext) {
        channelActiveDispatcher.dispatch(connection)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        channelInactiveDispatcher.dispatch(ctx)
    }

    override fun channelRead(connection: ChannelHandlerContext, msg: Any) {
        val buffer = msg as ByteBuf

        try {
            val wrappedPacket = WrappedIntercomPacket.STREAM_CODEC.read(buffer)

            val packetData = registry.getByIdentifier<IntercomSerializable>(wrappedPacket.identifier)
            try {
                val packet = packetData.serializer.read(wrappedPacket.data)
                packetReceived.dispatch(packet to connection)
                registry.getPacketHandlers(packet::class)?.invoke(packet, ConnectionInfo(this, connection))
            } finally {
                wrappedPacket.data.release()
            }
        } catch (ex: Throwable) {
            exceptionThrown.dispatch(ex to connection)
        } finally {
            buffer.release()
        }
    }
}

fun ChannelHandlerContext.sendPacket(packet: IntercomSerializable, handler: IntercomPacketHandler) {
    val data = handler.registry.getByClass<IntercomSerializable>(packet::class)
    val buffer = Unpooled.buffer()
    data.serializer.write(buffer, packet)

    val wrapped = WrappedIntercomPacket(data.identifier, buffer)
    val wrappedBuffer = Unpooled.buffer()
    WrappedIntercomPacket.STREAM_CODEC.write(wrappedBuffer, wrapped)
    this.writeAndFlush(wrappedBuffer)
}