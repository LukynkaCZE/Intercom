package cz.lukynka.intercom.common.handlers

import cz.lukynka.intercom.common.protocol.encryption.Crypto
import cz.lukynka.intercom.common.protocol.encryption.EncryptionBase
import cz.lukynka.intercom.common.protocol.encryption.EncryptionUtil
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncryptionHandler(private val crypto: Crypto) : MessageToByteEncoder<ByteBuf>() {

    private val encryptionBase = EncryptionBase(EncryptionUtil.getEncryptionCipherInstance(crypto))

    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        if (!crypto.isConnectionEncrypted) {
            out.writeBytes(msg.retain())
            return
        }

        out.writeBytes(encryptionBase.encrypt(msg.retain()))
        msg.release()
    }
}