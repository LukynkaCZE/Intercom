package cz.lukynka.intercom.common.protocol.encryption

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

class Crypto(
    val publicKey: PublicKey,
    val privateKey: PrivateKey,
    val verifyToken: ByteArray,
    var sharedSecret: SecretKey? = null,
    var isConnectionEncrypted: Boolean = false,
)