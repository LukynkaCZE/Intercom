package cz.lukynka.intercom.test

import cz.lukynka.intercom.client.IntercomClient
import cz.lukynka.intercom.common.IntercomRegistry
import cz.lukynka.intercom.common.handlers.IntercomPacketHandler
import cz.lukynka.intercom.common.protocol.IntercomSerializer
import cz.lukynka.intercom.server.IntercomServer
import cz.lukynka.intercom.test.protocol.PlayerData
import cz.lukynka.intercom.test.protocol.RequestPlayerData
import cz.lukynka.prettylog.LogType

fun main() {
    val authorizationToken = "123abc"

    val serverHandler = IntercomPacketHandler(::createRegistry)
    val server = IntercomServer(serverHandler, authorizationToken)

    val aria = IntercomClient(IntercomPacketHandler(::createRegistry), "Aria", authorizationToken)
    val sonnet = IntercomClient(IntercomPacketHandler(::createRegistry), "Sonnet", authorizationToken)
    val canon = IntercomClient(IntercomPacketHandler(::createRegistry), "Canon", authorizationToken)

    listOf(aria, sonnet, canon).forEach { client ->
        client.successfullyConnectedToServer.subscribe { connection ->
            connection.sendRequest(RequestPlayerData("LukynkaCZE"), PlayerData::class).thenAccept { response ->
                client.logger.log("Received player data response: $response", LogType.RUNTIME)
            }
        }
    }

    server.start().thenAccept {
        aria.connect()
        sonnet.connect()
        canon.connect()
    }

}

fun createRegistry(): IntercomRegistry {
    val registry = IntercomRegistry()

    registry.add("player_data_request", RequestPlayerData::class, IntercomSerializer.fromStreamCodec(RequestPlayerData.STREAM_CODEC))
    registry.add("player_data_response", PlayerData::class, IntercomSerializer.fromStreamCodec(PlayerData.STREAM_CODEC))

    registry.addRequestHandler(RequestPlayerData::class) { request, _ ->
        PlayerData(request.username, 7, 14)
    }

    return registry
}