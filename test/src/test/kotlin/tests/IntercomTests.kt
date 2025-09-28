package tests

import TestUtil
import cz.lukynka.intercom.client.IntercomClient
import cz.lukynka.intercom.common.handlers.IntercomPacketHandler
import cz.lukynka.intercom.server.IntercomServer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import packets.ClientboundTestHelloPacket
import packets.ServerboundTestHiPacket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals

class IntercomTests {

    @Test
    fun testClientServerConnection() {
        val server = IntercomServer(IntercomPacketHandler(TestUtil.registry), TestUtil.AUTH_TOKEN)
        val client = IntercomClient(IntercomPacketHandler(TestUtil.registry), "Aria", TestUtil.AUTH_TOKEN)
        val countDownLatch = CountDownLatch(1)
        var authorized = false

        server.start().thenAccept {
            client.connect()
        }

        client.successfullyConnectedToServer.subscribe {
            countDownLatch.countDown()
            authorized = true
        }

        countDownLatch.await(5, TimeUnit.SECONDS)
        assertTrue(authorized)

        client.disconnect()
        server.shutdown()
    }

    @Test
    fun testCustomPacketSending() {
        val serverHandler = IntercomPacketHandler(TestUtil.registry)
        val server = IntercomServer(serverHandler, TestUtil.AUTH_TOKEN)

        val clientHandler = IntercomPacketHandler(TestUtil.registry)
        val client = IntercomClient(clientHandler, "Aria", TestUtil.AUTH_TOKEN)

        val countDownLatch = CountDownLatch(1)
        var received: Int = -1
        val sent: Int = Random.nextInt(0, Int.MAX_VALUE)

        server.start().thenAccept {
            client.connect()
        }

        server.clientAuthorized.subscribe { connectedClient ->
            connectedClient.connection.sendPacket(ClientboundTestHelloPacket(sent))
        }

        TestUtil.registry.addPacketHandler(ClientboundTestHelloPacket::class) { packet, handler ->
            handler.sendPacket(ServerboundTestHiPacket(packet.sequence))
        }

        TestUtil.registry.addPacketHandler(ServerboundTestHiPacket::class) { packet, _ ->
            countDownLatch.countDown()
            received = packet.sequence
        }

        countDownLatch.await(5, TimeUnit.SECONDS)
        assertEquals(sent, received)

        client.disconnect()
        server.shutdown()
    }
}