package cz.lukynka.intercom.common.protocol.encryption

import java.nio.charset.Charset
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object EncryptionUtil {

    const val TRANSFORMATION = "AES/CFB8/NoPadding"
    val DIGEST_CHARSET: Charset = Charset.forName("ISO_8859_1")
    var keyPair: KeyPair = generateKeyPair() ?: throw IllegalStateException("keypair is null")

    fun generateKeyPair(): KeyPair? {
        try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(1024)
            return keyGen.generateKeyPair()
        } catch (exception: Exception) {
            return null
        }
    }

    fun digestData(data: String, publicKey: PublicKey, secretKey: SecretKey): ByteArray? {
        return try {
            digestData("SHA-1", data.toByteArray(DIGEST_CHARSET), secretKey.encoded, publicKey.encoded)
        } catch (ex: Exception) {
            null
        }
    }

    fun getNewCrypto(): Crypto {

        val verificationToken = ByteArray(4)
        ThreadLocalRandom.current().nextBytes(verificationToken)

        return Crypto(keyPair.public, keyPair.private, verificationToken)
    }

    fun digestData(algorithm: String, vararg data: ByteArray): ByteArray? {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            data.forEach { array ->
                digest.update(array)
            }
            return digest.digest()
        } catch (exception: Exception) {
            return null
        }
    }

    fun publicRSAKeyFrom(data: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(data)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    fun getDecryptionCipherInstance(crypto: Crypto): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, crypto.sharedSecret, IvParameterSpec(crypto.sharedSecret!!.encoded))
        return cipher
    }

    fun getEncryptionCipherInstance(crypto: Crypto): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, crypto.sharedSecret, IvParameterSpec(crypto.sharedSecret!!.encoded))
        return cipher
    }

}