package es.crttn.dad;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class CifradoHelper {

    // Método para cifrar la contraseña
    public static String cifra(String password) throws Exception {
        // Generamos la clave de 16 bytes a partir de una frase
        String frase = "ClaveDeCifradoAES";  // Esta puede ser cualquier cadena de texto
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = Arrays.copyOf(digest.digest(frase.getBytes(StandardCharsets.UTF_8)), 16); // Usamos solo los primeros 16 bytes
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // Creamos un objeto Cipher para cifrado
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Ciframos la contraseña
        byte[] passwordCifrada = cipher.doFinal(password.getBytes());

        // Convertimos el resultado a Base64 para almacenarlo como texto
        return Base64.getEncoder().encodeToString(passwordCifrada);
    }

    // Método para descifrar la contraseña
    public static String descifra(String passwordCifradaBase64) throws Exception {
        // Generamos la misma clave de 16 bytes a partir de la misma frase
        String frase = "ClaveDeCifradoAES";  // Debe ser la misma clave que se usó para cifrar
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = Arrays.copyOf(digest.digest(frase.getBytes(StandardCharsets.UTF_8)), 16); // Usamos solo los primeros 16 bytes
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // Creamos un objeto Cipher para descifrado
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decodificamos el texto Base64 para obtener los bytes cifrados
        byte[] passwordCifradaBytes = Base64.getDecoder().decode(passwordCifradaBase64);

        // Desciframos la contraseña
        byte[] passwordDescifrada = cipher.doFinal(passwordCifradaBytes);

        // Convertimos los bytes de vuelta a una cadena
        return new String(passwordDescifrada);
    }

    private Cipher obtieneCipher(boolean paraCifrar) throws Exception {
        final String frase = "FraseLargaConDiferentesLetrasNumerosYCaracteresEspeciales_áÁéÉíÍóÓúÚüÜñÑ1234567890!#%$&()=%_NO_USAR_ESTA_FRASE!_";

        // Crear un mensaje hash usando SHA-256 (esto da 32 bytes)
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(frase.getBytes("UTF-8"));

        // Tomar solo los primeros 16 bytes de los 32 que genera SHA-256
        final byte[] claveAES = Arrays.copyOf(digest.digest(), 16); // Asegura que la clave tenga exactamente 16 bytes

        final SecretKeySpec key = new SecretKeySpec(claveAES, "AES");

        final Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
        if (paraCifrar) {
            aes.init(Cipher.ENCRYPT_MODE, key);
        } else {
            aes.init(Cipher.DECRYPT_MODE, key);
        }

        return aes;
    }
}
