package cz.lukynka.intercom.common.handlers

import cz.lukynka.intercom.common.IntercomPacketRegistry
import cz.lukynka.bindables.BindableDispatcher
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import cz.lukynka.intercom.common.protocol.IntercomPacket
import cz.lukynka.intercom.common.protocol.packet.WrappedIntercomPacket

@Sharable
class IntercomMessageHandler(val registry: IntercomPacketRegistry) : ChannelInboundHandlerAdapter() {

    var encryptionEnabled = false
    var state: ConnectionState = ConnectionState.HANDSHAKE

    val channelActiveDispatcher = BindableDispatcher<ChannelHandlerContext>()
    val channelInactiveDispatcher = BindableDispatcher<ChannelHandlerContext>()

    private var channelHandlerContext: ChannelHandlerContext? = null

    override fun channelActive(connection: ChannelHandlerContext) {
        if (channelHandlerContext == null) channelHandlerContext = connection
        channelActiveDispatcher.dispatch(connection)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (channelHandlerContext != null) channelHandlerContext = null
        channelInactiveDispatcher.dispatch(ctx)
    }

    override fun channelRead(connection: ChannelHandlerContext, msg: Any) {
        val buffer = msg as ByteBuf
        if (channelHandlerContext == null) channelHandlerContext = connection
        try {
            val wrappedPacket = WrappedIntercomPacket.STREAM_CODEC.read(buffer)
            val packetData = registry.getByIdentifier<IntercomPacket>(wrappedPacket.identifier)
            try {
                val packet = packetData.serializer.read(wrappedPacket.data)
                registry.getHandler(packet::class)?.invoke(packet, connection)
            } finally {
                wrappedPacket.data.release()
            }
        } finally {
            buffer.release()
        }
    }

    fun sendPacket(packet: IntercomPacket) {
        if (channelHandlerContext == null) throw IllegalStateException("Connection has not been opened yet")
        val data = registry.getByClass<IntercomPacket>(packet::class)
        val buffer = Unpooled.buffer()
        data.serializer.write(buffer, packet)
        channelHandlerContext!!.writeAndFlush(buffer)
    }

    enum class ConnectionState {
        HANDSHAKE,
        AUTHORIZATION,
        ACTIVE
    }
}