package cz.lukynka.intercom.common.handlers

import cz.lukynka.bindables.BindablePool
import cz.lukynka.intercom.common.IntercomPacketRegistry
import cz.lukynka.intercom.common.protocol.IntercomPacket
import cz.lukynka.intercom.common.protocol.packet.WrappedIntercomPacket
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

@Sharable
class IntercomPacketHandler(val registry: IntercomPacketRegistry) : ChannelInboundHandlerAdapter() {

    var encryptionEnabled = false

    val bindablePool = BindablePool()

    val channelActiveDispatcher = bindablePool.provideBindableDispatcher<ChannelHandlerContext>()
    val channelInactiveDispatcher = bindablePool.provideBindableDispatcher<ChannelHandlerContext>()
    val packetReceived = bindablePool.provideBindableDispatcher<Pair<IntercomPacket, ChannelHandlerContext>>()
    val exceptionThrown = bindablePool.provideBindableDispatcher<Pair<Throwable, ChannelHandlerContext>>()

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

            val packetData = registry.getByIdentifier<IntercomPacket>(wrappedPacket.identifier)
            try {
                val packet = packetData.serializer.read(wrappedPacket.data)
                packetReceived.dispatch(packet to connection)
                registry.getHandler(packet::class)?.invoke(packet, connection)
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

fun ChannelHandlerContext.sendPacket(packet: IntercomPacket, handler: IntercomPacketHandler) {
    val data = handler.registry.getByClass<IntercomPacket>(packet::class)
    val buffer = Unpooled.buffer()
    data.serializer.write(buffer, packet)

    val wrapped = WrappedIntercomPacket(data.identifier, buffer)
    val wrappedBuffer = Unpooled.buffer()
    WrappedIntercomPacket.STREAM_CODEC.write(wrappedBuffer, wrapped)
    this.writeAndFlush(wrappedBuffer)
}