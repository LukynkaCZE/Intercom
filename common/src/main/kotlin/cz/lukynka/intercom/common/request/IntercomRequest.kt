package cz.lukynka.intercom.common.request

import cz.lukynka.intercom.common.IntercomRegistry
import cz.lukynka.intercom.common.protocol.IntercomSerializable
import java.util.concurrent.CompletableFuture

data class IntercomRequest<Request : IntercomSerializable, Response : IntercomSerializable>(val request: Request, val responseSerializerData: IntercomRegistry.IntercomSerializerData<Response>) {
    val future = CompletableFuture<Response>()
}
