package cz.lukynka.intercom.client

import cz.lukynka.bindables.BindableDispatcher
import cz.lukynka.intercom.common.ConnectionInfo
import cz.lukynka.intercom.common.SharedIntercomConstants
import cz.lukynka.intercom.common.handlers.*
import cz.lukynka.intercom.common.protocol.encryption.EncryptionUtil
import cz.lukynka.intercom.common.protocol.packet.*
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.PrettyLogger
import cz.lukynka.prettylog.style.LogStyle
import cz.lukynka.prettylog.style.StaticLogPrefix
import io.github.dockyardmc.tide.codec.toByteArraySafe
import io.github.dockyardmc.tide.codec.toByteBuf
import java.util.concurrent.CompletableFuture
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

data class IntercomClient(val messageHandler: IntercomPacketHandler, val serverName: String, val authorizationToken: String) {

    val logger = PrettyLogger(StaticLogPrefix(" $serverName ", LogStyle.FILLED_LIGHT_GRAY))
    val crypto = EncryptionUtil.getNewCrypto()
    val nettyClient = NettyClient(this, "0.0.0.0", 25566)

    val successfullyConnectedToServer = BindableDispatcher<ConnectionInfo>()

    init {
        messageHandler.registry.addPacketHandler(ClientboundDisconnectClientIntercomPacket::class) { packet, _ ->
            logger.log(" ", LogType.ERROR)
            logger.log("Client disconnected by server, reason: ${packet.reason}", LogType.ERROR)
            logger.log(" ", LogType.ERROR)
            messageHandler.bindablePool.dispose()
            nettyClient.disconnect()
        }

        messageHandler.channelInactiveDispatcher.subscribe { connection ->
            nettyClient.reconnect()
        }

        messageHandler.channelActiveDispatcher.subscribe { connection ->
            connection.sendPacket(ServerboundHandshakePacket(serverName, SharedIntercomConstants.PROTOCOL_VERSION), messageHandler)
        }

        messageHandler.registry.addPacketHandler(ClientboundEncryptionRequestIntercomPacket::class) { packet, connection ->

            val serverPublicKeyBytes = packet.publicKey.toByteArraySafe()
            val verifyToken = packet.verifyToken.toByteArraySafe()

            val serverPublicKey = EncryptionUtil.publicRSAKeyFrom(serverPublicKeyBytes)

            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(128)
            val sharedSecret = keyGenerator.generateKey()

            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey)

            val encryptedSharedSecret = cipher.doFinal(sharedSecret.encoded)
            val encryptedVerifyToken = cipher.doFinal(verifyToken)

            crypto.sharedSecret = sharedSecret

            val responsePacket = ServerboundEncryptionResponseIntercomPacket(
                encryptedSharedSecret.toByteBuf(),
                encryptedVerifyToken.toByteBuf()
            )

            connection.sendPacket(responsePacket)
        }

        messageHandler.registry.addPacketHandler(ClientboundEncryptionFinish::class) { _, connection ->

            val pipeline = connection.connection.channel().pipeline()

            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_ENCRYPTOR, PacketEncryptionHandler(crypto))
            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_DECRYPTOR, PacketDecryptionHandler(crypto))

            crypto.isConnectionEncrypted = true
            messageHandler.encryptionEnabled = true

            connection.sendPacket(ServerboundAuthorizationRequestIntercomPacket(authorizationToken))
        }

        messageHandler.registry.addPacketHandler(ClientboundAuthorizationResponseIntercomPacket::class) { _, connection ->
            successfullyConnectedToServer.dispatch(connection)
        }
    }

    fun disconnect() {
        nettyClient.disconnect()
    }

    fun connect(): CompletableFuture<Unit> {
        return nettyClient.start()
    }
}