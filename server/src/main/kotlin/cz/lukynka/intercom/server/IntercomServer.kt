package cz.lukynka.intercom.server

import cz.lukynka.bindables.BindableDispatcher
import cz.lukynka.intercom.common.ConnectionInfo
import cz.lukynka.intercom.common.SharedIntercomConstants
import cz.lukynka.intercom.common.handlers.*
import cz.lukynka.intercom.common.protocol.encryption.Crypto
import cz.lukynka.intercom.common.protocol.encryption.EncryptionUtil
import cz.lukynka.intercom.common.protocol.packet.*
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.PrettyLogger
import io.github.dockyardmc.tide.codec.toByteArraySafe
import io.github.dockyardmc.tide.codec.toByteBuf
import io.netty.channel.ChannelHandlerContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class IntercomServer(val messageHandler: IntercomPacketHandler, val authorizationToken: String) {

    companion object {
        val logger: PrettyLogger = PrettyLogger(IntercomServer::class)

        const val ERROR_NOT_MATCHING_VERIFY_TOKEN = "Verify token does not match with the server"
        const val ERROR_ENCRYPTION_DIGEST_FAILED = "Encryption digest validation failed"
        const val ERROR_NOT_MATCHING_AUTH_TOKEN = "Invalid authorization token"
        const val ERROR_NOT_MATCHING_PROTOCOL_VERSION = "Not matching protocol version. (Client: {1}, Server: {2})"
        const val ERROR_SERVER_WITH_SAME_NAME_EXISTS = "Server with the same name is already registered"
    }

    val nettyServer = NettyServer(this, "0.0.0.0", 25566)

    val connectedClients: MutableMap<String, ConnectedClient> = mutableMapOf()
    val pendingClients: MutableMap<ChannelHandlerContext, String> = mutableMapOf()
    val connectionCrypto: MutableMap<ChannelHandlerContext, Crypto> = mutableMapOf()

    val clientAuthorized = BindableDispatcher<ConnectedClient>()

    private val handshakeQueue = LinkedBlockingQueue<Pair<ServerboundHandshakePacket, ConnectionInfo>>()
    private val handshakeProcessor = Executors.newSingleThreadExecutor()

    data class ConnectedClient(val name: String, val connection: ConnectionInfo)

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
                    logger.log("Error processing handshake: ${e.message}", LogType.ERROR)
                }
            }
        }

        messageHandler.channelInactiveDispatcher.subscribe { connection ->
            val connected = connectedClients.values.firstOrNull { it.connection.connection == connection }
            if (connected != null) connectedClients.remove(connected.name)

            pendingClients.remove(connection)
            connectionCrypto.remove(connection)
            handshakeQueue.removeIf { it.second.connection == connection }
            logger.log("Client disconnected: ${connected?.name ?: "Unknown Client"}", LogType.DEBUG)
        }

        messageHandler.packetReceived.subscribe { (packet, connection) ->
            val name = pendingClients[connection] ?: connectedClients.values.firstOrNull { it.connection.connection == connection }?.name ?: "Unknown Client"
            logger.log("[$name -> Server] Received $packet", LogType.NETWORK)
        }

        messageHandler.registry.addPacketHandler(ServerboundHandshakePacket::class) { packet, connection ->
            handshakeQueue.offer(packet to connection)
        }

        messageHandler.registry.addPacketHandler(ServerboundEncryptionResponseIntercomPacket::class) { packet, connection ->
            val cipher = Cipher.getInstance("RSA")
            val crypto = connectionCrypto[connection.connection] ?: return@addPacketHandler

            cipher.init(Cipher.DECRYPT_MODE, crypto.privateKey)

            val verifyToken = cipher.doFinal(packet.verificationToken.toByteArraySafe())
            val sharedSecret = cipher.doFinal(packet.sharedSecret.toByteArraySafe())

            if (!verifyToken.contentEquals(crypto.verifyToken)) {
                handleError(ERROR_NOT_MATCHING_VERIFY_TOKEN, connection.connection)
                return@addPacketHandler
            }

            val sharedSecretKey = SecretKeySpec(sharedSecret, "AES")
            val digestedData = EncryptionUtil.digestData("", EncryptionUtil.keyPair.public, sharedSecretKey)
            if (digestedData == null) {
                handleError(ERROR_ENCRYPTION_DIGEST_FAILED, connection.connection)
                return@addPacketHandler
            }

            crypto.sharedSecret = sharedSecretKey
            connection.sendPacket(ClientboundEncryptionFinish())

            val pipeline = connection.connection.channel().pipeline()
            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_ENCRYPTOR, PacketEncryptionHandler(crypto))
            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_DECRYPTOR, PacketDecryptionHandler(crypto))

            crypto.isConnectionEncrypted = true
            messageHandler.encryptionEnabled = true
        }

        messageHandler.registry.addPacketHandler(ServerboundAuthorizationRequestIntercomPacket::class) { packet, connection ->
            if (packet.authToken != authorizationToken) {
                handleError(ERROR_NOT_MATCHING_AUTH_TOKEN, connection.connection)
                return@addPacketHandler
            }

            connection.sendPacket(ClientboundAuthorizationResponseIntercomPacket())
            val name = pendingClients[connection.connection]!!
            pendingClients.remove(connection.connection)
            val client = ConnectedClient(name, connection)

            connectedClients[name] = client
            logger.log("Client $name connected and authorized! There are now ${connectedClients.size} connected clients!", LogType.SUCCESS)
            clientAuthorized.dispatch(client)
        }

    }

    private fun processHandshake(packet: ServerboundHandshakePacket, connection: ConnectionInfo) {
        if (packet.protocolVersion != SharedIntercomConstants.PROTOCOL_VERSION) {
            handleError(
                ERROR_NOT_MATCHING_PROTOCOL_VERSION
                    .replace("{1}", "${packet.protocolVersion}")
                    .replace("{2}", SharedIntercomConstants.PROTOCOL_VERSION.toString()), connection.connection
            )
            return
        }

        if (connectedClients.containsKey(packet.serverName)) {
            handleError(ERROR_SERVER_WITH_SAME_NAME_EXISTS, connection.connection)
            return
        }

        val crypto = EncryptionUtil.getNewCrypto()
        connectionCrypto[connection.connection] = crypto
        pendingClients[connection.connection] = packet.serverName

        connection.sendPacket(
            ClientboundEncryptionRequestIntercomPacket(
                crypto.publicKey.encoded.toByteBuf(),
                crypto.verifyToken.toByteBuf()
            ),
        )
    }

    fun handleError(message: String, connection: ChannelHandlerContext) {
        val name = pendingClients[connection] ?: connectedClients.values.firstOrNull { it.connection.connection == connection }?.name ?: "Unknown Client"
        connection.sendPacket(ClientboundDisconnectClientIntercomPacket(message), messageHandler)
        logger.log("Client $name has been disconnected, reason: $message. There are now ${connectedClients.size} connected clients!", LogType.ERROR)

        connection.channel().eventLoop().schedule({
            connection.close()
            logger.log("Client $name has been disconnected, reason: $message. There are now ${connectedClients.size} connected clients!", LogType.ERROR)
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