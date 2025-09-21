package cz.lukynka.intercom.server

import cz.lukynka.intercom.common.IntercomPacketRegistry
import cz.lukynka.intercom.common.SharedIntercomConstants
import cz.lukynka.intercom.common.handlers.*
import cz.lukynka.intercom.common.protocol.encryption.Crypto
import cz.lukynka.intercom.common.protocol.encryption.EncryptionUtil
import cz.lukynka.intercom.common.protocol.packet.*
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.github.dockyardmc.tide.codec.toByteArraySafe
import io.github.dockyardmc.tide.codec.toByteBuf
import io.netty.channel.ChannelHandlerContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

data class IntercomServer(val registry: IntercomPacketRegistry, val messageHandler: IntercomPacketHandler, val authorizationToken: String) {

    companion object {
        const val ERROR_NOT_MATCHING_VERIFY_TOKEN = "Verify token does not match with the server"
        const val ERROR_ENCRYPTION_DIGEST_FAILED = "Encryption digest validation failed"
        const val ERROR_NOT_MATCHING_AUTH_TOKEN = "Invalid authorization token"
        const val ERROR_NOT_MATCHING_PROTOCOL_VERSION = "Not matching protocol version. (Client: {1}, Server: {2})"
        const val ERROR_SERVER_WITH_SAME_NAME_EXISTS = "Server with the same name is already registered"
    }

    val nettyServer = NettyServer(messageHandler, "0.0.0.0", 25566)

    val connectedClients: MutableMap<String, ConnectedClient> = mutableMapOf()
    val pendingClients: MutableMap<ChannelHandlerContext, String> = mutableMapOf()
    val connectionCrypto: MutableMap<ChannelHandlerContext, Crypto> = mutableMapOf()

    private val handshakeQueue = LinkedBlockingQueue<Pair<ServerboundHandshakePacket, ChannelHandlerContext>>()
    private val handshakeProcessor = Executors.newSingleThreadExecutor()

    data class ConnectedClient(val name: String, val connection: ChannelHandlerContext)

    init {
        messageHandler.exceptionThrown.subscribe { (ex, connection) ->
            handleError(ex.message ?: "${ex::class.simpleName}", connection)
        }

        handshakeProcessor.submit {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val (packet, connection) = handshakeQueue.take()
                    processHandshake(packet, connection)
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    log("Error processing handshake: ${e.message}", LogType.ERROR)
                }
            }
        }

        messageHandler.channelInactiveDispatcher.subscribe { connection ->
            val connected = connectedClients.values.firstOrNull { it.connection == connection }
            if (connected != null) connectedClients.remove(connected.name)

            pendingClients.remove(connection)
            connectionCrypto.remove(connection)
            handshakeQueue.removeIf { it.second == connection }
            log("Client disconnected: ${connected?.name ?: "Unknown Client"}", LogType.DEBUG)
        }

        messageHandler.packetReceived.subscribe { (packet, connection) ->
            val name = pendingClients[connection] ?: connectedClients.values.firstOrNull { it.connection == connection }?.name ?: "Unknown Client"
            log("[$name -> Server] Received $packet", LogType.NETWORK)
        }

        registry.addHandler(ServerboundHandshakePacket::class) { packet, connection ->
            handshakeQueue.offer(packet to connection)
        }

        registry.addHandler(ServerboundEncryptionResponseIntercomPacket::class) { packet, connection ->
            val cipher = Cipher.getInstance("RSA")
            val crypto = connectionCrypto[connection] ?: return@addHandler

            cipher.init(Cipher.DECRYPT_MODE, crypto.privateKey)

            val verifyToken = cipher.doFinal(packet.verificationToken.toByteArraySafe())
            val sharedSecret = cipher.doFinal(packet.sharedSecret.toByteArraySafe())

            if (!verifyToken.contentEquals(crypto.verifyToken)) {
                handleError(ERROR_NOT_MATCHING_VERIFY_TOKEN, connection)
                return@addHandler
            }

            val sharedSecretKey = SecretKeySpec(sharedSecret, "AES")
            val digestedData = EncryptionUtil.digestData("", EncryptionUtil.keyPair.public, sharedSecretKey)
            if (digestedData == null) {
                handleError(ERROR_ENCRYPTION_DIGEST_FAILED, connection)
                return@addHandler
            }

            crypto.sharedSecret = sharedSecretKey
            connection.sendPacket(ClientboundEncryptionFinish(), messageHandler)

            val pipeline = connection.channel().pipeline()
            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_ENCRYPTOR, PacketEncryptionHandler(crypto))
            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_DECRYPTOR, PacketDecryptionHandler(crypto))

            crypto.isConnectionEncrypted = true
            messageHandler.encryptionEnabled = true
        }

        registry.addHandler(ServerboundAuthorizationRequestIntercomPacket::class) { packet, connection ->
            if (packet.authToken != authorizationToken) {
                handleError(ERROR_NOT_MATCHING_AUTH_TOKEN, connection)
                return@addHandler
            }

            connection.sendPacket(ClientboundAuthorizationResponseIntercomPacket(), messageHandler)
            val name = pendingClients[connection]!!
            pendingClients.remove(connection)
            val client = ConnectedClient(name, connection)

            connectedClients[name] = client
            log("Client $name connected and authorized! There are now ${connectedClients.size} connected clients!", LogType.SUCCESS)
        }

    }

    private fun processHandshake(packet: ServerboundHandshakePacket, connection: ChannelHandlerContext) {
        if (packet.protocolVersion != SharedIntercomConstants.PROTOCOL_VERSION) {
            handleError(
                ERROR_NOT_MATCHING_PROTOCOL_VERSION
                    .replace("{1}", "${packet.protocolVersion}")
                    .replace("{2}", SharedIntercomConstants.PROTOCOL_VERSION.toString()), connection
            )
            return
        }

        if (connectedClients.containsKey(packet.serverName)) {
            handleError(ERROR_SERVER_WITH_SAME_NAME_EXISTS, connection)
            return
        }

        val crypto = EncryptionUtil.getNewCrypto()
        connectionCrypto[connection] = crypto
        pendingClients[connection] = packet.serverName

        connection.sendPacket(
            ClientboundEncryptionRequestIntercomPacket(
                crypto.publicKey.encoded.toByteBuf(),
                crypto.verifyToken.toByteBuf()
            ),
            messageHandler
        )
    }

    fun handleError(message: String, connection: ChannelHandlerContext) {
        val name = pendingClients[connection] ?: connectedClients.values.firstOrNull { it.connection == connection }?.name ?: "Unknown Client"
        connection.sendPacket(ClientboundDisconnectClientIntercomPacket(message), messageHandler)
        log("Client $name has been disconnected, reason: $message. There are now ${connectedClients.size} connected clients!", LogType.ERROR)

        connection.channel().eventLoop().schedule({
            connection.close()
            log("Client $name has been disconnected, reason: $message. There are now ${connectedClients.size} connected clients!", LogType.ERROR)
        }, 100, TimeUnit.MILLISECONDS)

    }

    fun start(): CompletableFuture<Unit> {
        return nettyServer.start()
    }

    fun shutdown() {
        handshakeProcessor.shutdownNow()
        nettyServer.shutdown()
        messageHandler.bindablePool.dispose()
    }
}