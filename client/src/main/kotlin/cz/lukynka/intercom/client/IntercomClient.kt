package cz.lukynka.intercom.client

import cz.lukynka.intercom.common.IntercomPacketRegistry
import cz.lukynka.intercom.common.SharedIntercomConstants
import cz.lukynka.intercom.common.handlers.*
import cz.lukynka.intercom.common.protocol.encryption.EncryptionUtil
import cz.lukynka.intercom.common.protocol.packet.*
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import io.github.dockyardmc.tide.codec.toByteArraySafe
import io.github.dockyardmc.tide.codec.toByteBuf
import java.util.concurrent.CompletableFuture
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

data class IntercomClient(val registry: IntercomPacketRegistry, val messageHandler: IntercomPacketHandler, val serverName: String, val authorizationToken: String) {

    val crypto = EncryptionUtil.getNewCrypto()
    val nettyClient = NettyClient(messageHandler, "0.0.0.0", 25566)

    init {
        registry.addHandler(ClientboundDisconnectClientIntercomPacket::class) { packet, _ ->
            log(" ", LogType.ERROR)
            log("Client disconnected by server, reason: ${packet.reason}", LogType.ERROR)
            log(" ", LogType.ERROR)
            messageHandler.bindablePool.dispose()
            nettyClient.disconnect()
        }

        messageHandler.channelActiveDispatcher.subscribe { connection ->
            connection.sendPacket(ServerboundHandshakePacket(serverName, SharedIntercomConstants.PROTOCOL_VERSION), messageHandler)
        }

        registry.addHandler(ClientboundEncryptionRequestIntercomPacket::class) { packet, connection ->

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

            connection.sendPacket(responsePacket, messageHandler)
        }

        registry.addHandler(ClientboundEncryptionFinish::class) { _, connection ->

            val pipeline = connection.channel().pipeline()

            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_ENCRYPTOR, PacketEncryptionHandler(crypto))
            pipeline.addBefore(ChannelHandlers.MESSAGE_HANDLER, ChannelHandlers.PACKET_DECRYPTOR, PacketDecryptionHandler(crypto))

            crypto.isConnectionEncrypted = true
            messageHandler.encryptionEnabled = true

            connection.sendPacket(ServerboundAuthorizationRequestIntercomPacket(authorizationToken), messageHandler)
        }

        registry.addHandler(ClientboundAuthorizationResponseIntercomPacket::class) { _, _ ->
        }
    }

    fun disconnect() {
        nettyClient.disconnect()
    }

    fun connect(): CompletableFuture<Unit> {
        return nettyClient.start()
    }
}