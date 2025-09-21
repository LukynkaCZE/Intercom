package cz.lukynka.intercom.test

import cz.lukynka.intercom.client.IntercomClient
import cz.lukynka.intercom.common.IntercomPacketRegistry
import cz.lukynka.intercom.common.handlers.IntercomPacketHandler
import cz.lukynka.intercom.server.IntercomServer
import java.util.UUID

fun main() {
    val authorizationToken = "123abc"
    val serverRegistry = IntercomPacketRegistry()
    val serverHandler = IntercomPacketHandler(serverRegistry)
    val server = IntercomServer(serverRegistry, serverHandler, authorizationToken)
    server.start().thenAccept {
        for (i in 0 until 30) {
            val _registry = IntercomPacketRegistry()
            val _handler = IntercomPacketHandler(_registry)
            val _client = IntercomClient(_registry, _handler, UUID.randomUUID().toString(), authorizationToken)
            _client.connect()
        }
    }
}