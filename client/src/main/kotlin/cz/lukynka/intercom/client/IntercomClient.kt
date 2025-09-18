package cz.lukynka.intercom.client

import cz.lukynka.intercom.common.IntercomPacketRegistry
import cz.lukynka.intercom.common.handlers.IntercomMessageHandler

data class IntercomClient(val registry: IntercomPacketRegistry, val messageHandler: IntercomMessageHandler) {

    //TODO register a handler to the defgault packets (common)
    //TODO add configuration (common)
    //TODO

}