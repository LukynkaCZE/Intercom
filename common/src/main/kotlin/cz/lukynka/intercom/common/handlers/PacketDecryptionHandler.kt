package cz.lukynka.intercom.common.handlers

import cz.lukynka.intercom.common.protocol.encryption.EncryptionBase
import cz.lukynka.intercom.common.protocol.encryption.Crypto
import cz.lukynka.intercom.common.protocol.encryption.EncryptionUtil
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

class PacketDecryptionHandler(private val crypto: Crypto) : MessageToMessageDecoder<ByteBuf>() {

    val encryptionBase = EncryptionBase(EncryptionUtil.getDecryptionCipherInstance(crypto))

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        if (!crypto.isConnectionEncrypted) {
            out.add(msg.retain())
            return
        }

        val byteBuf = encryptionBase.decrypt(ctx, msg.retain())
        out.add(byteBuf)
        msg.release()
    }


}