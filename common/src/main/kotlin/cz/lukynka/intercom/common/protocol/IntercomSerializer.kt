package cz.lukynka.intercom.common.protocol

import io.github.dockyardmc.tide.stream.StreamCodec
import io.netty.buffer.ByteBuf

interface IntercomSerializer<T> {
    fun read(buffer: ByteBuf): T
    fun write(buffer: ByteBuf, value: T)

    companion object {
        fun <T> fromStreamCodec(codec: StreamCodec<T>): IntercomSerializer<T> {
            return object : IntercomSerializer<T> {

                override fun read(buffer: ByteBuf): T {
                    return codec.read(buffer)
                }

                override fun write(buffer: ByteBuf, value: T) {
                    codec.write(buffer, value)
                }
            }
        }
    }
}