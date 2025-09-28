package cz.lukynka.intercom.server

import cz.lukynka.intercom.common.handlers.ChannelHandlers
import cz.lukynka.intercom.common.handlers.IntercomPacketHandler
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import java.util.concurrent.CompletableFuture

class NettyServer(val server: IntercomServer, val ip: String, val port: Int) {

    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()
    
    private var serverChannel: io.netty.channel.Channel? = null

    fun start(): CompletableFuture<Unit> {
        val bootstrap = ServerBootstrap()
        return CompletableFuture.supplyAsync {
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        val pipeline = ch.pipeline()
                            .addLast(ChannelHandlers.FRAME_DECODER, LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4))
                            .addLast(ChannelHandlers.FRAME_ENCODER, LengthFieldPrepender(4))
                            .addLast(ChannelHandlers.MESSAGE_HANDLER, server.messageHandler)
                    }
                })

            val future = bootstrap.bind(ip, port).sync()
            serverChannel = future.channel()
            IntercomServer.logger.log("Intercom server running to $ip:$port!", LogType.SUCCESS)
        }
    }
    
    fun waitForShutdown() {
        serverChannel?.closeFuture()?.sync()
    }
    
    fun shutdown() {
        serverChannel?.close()
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }
}