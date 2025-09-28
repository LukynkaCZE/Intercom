package cz.lukynka.intercom.client

import cz.lukynka.intercom.common.handlers.ChannelHandlers
import cz.lukynka.prettylog.LogType
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class NettyClient(val client: IntercomClient, val ip: String, val port: Int) {

    val bossGroup = NioEventLoopGroup()
    val bootstrap = Bootstrap()

    private var channel: SocketChannel? = null
    private var isShuttingDown = false

    fun start(): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync {
            bootstrap.group(bossGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        channel = ch
                        val pipeline = ch.pipeline()
                            .addLast(ChannelHandlers.FRAME_DECODER, LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4))
                            .addLast(ChannelHandlers.FRAME_ENCODER, LengthFieldPrepender(4))
                            .addLast(ChannelHandlers.MESSAGE_HANDLER, client.messageHandler)
                    }
                })
            connect()
        }
    }

    fun disconnect() {
        isShuttingDown = true

        channel?.let { ch ->
            if (ch.isActive) {
                ch.close().sync()
            }
        }

        val shutdownFuture = bossGroup.shutdownGracefully()

        try {
            shutdownFuture.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            bossGroup.shutdownNow()
        }
        channel = null
    }

    fun isConnected(): Boolean {
        return channel?.isActive == true && !isShuttingDown
    }

    fun reconnect() {
        if (isShuttingDown) {
            client.logger.log("Cannot reconnect - client is shutting down", LogType.WARNING)
            return
        }

        client.logger.log("Scheduling reconnection in 5 seconds...", LogType.WARNING)
        bossGroup.schedule(::connect, 5, TimeUnit.SECONDS)
    }

    private fun connect() {
        if (isShuttingDown) {
            return
        }

        val future = bootstrap.connect(ip, port)

        future.addListener(ChannelFutureListener { channelFuture ->
            if (channelFuture.isSuccess) {
                client.logger.log("Intercom client connected to ${ip}:${port}!", LogType.SUCCESS)
            } else {
                if (!isShuttingDown) {
                    client.logger.log("Connection failed: ${channelFuture.cause()?.message}", LogType.ERROR)
                    reconnect()
                }
            }
        })
    }
}