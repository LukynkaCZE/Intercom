package cz.lukynka.intercom.common.request

import cz.lukynka.intercom.common.protocol.IntercomSerializable

data class IntercomResponse<Request : IntercomSerializable, Response : IntercomSerializable>(val response: Response)