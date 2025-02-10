package es.crttn.dad;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class CifradoHelper {

    final static String FRASE = "ClaveDeCifradoAES";

    // Método para cifrar contraseñas en Base64
    public static String cifrar(String password) throws Exception {

        // Generamos uan clasve de 16 bytes a partir de una frase


        // Se utiliza SHA-256 para generar un resumen de la frase y obtener una clave de 32 bytes
        MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Función de hash criptográfico
        byte[] key = Arrays.copyOf(digest.digest(FRASE.getBytes(StandardCharsets.UTF_8)), 16); // Usamos solo los primeros 16 bytes para AES (128 bits)

        // Creamos una clave secreta usando los 16 primeros bytes generados con SHA-256
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // Creamos un objeto Cipher para el cifrado con el algoritmo AES en modo ECB con relleno PKCS5
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey); // Inicializamos el cifrador en modo encriptación

        // Ciframos la contraseña
        byte[] passwordCifrada = cipher.doFinal(password.getBytes());

        // Convertimos el resultado cifrado a Base64 para que pueda ser almacenado como texto legible
        return Base64.getEncoder().encodeToString(passwordCifrada);
    }

    // Método para descifrar la contraseña cifrada en Base64
    public static String descifrar(String passwordCifradaBase64) throws Exception {

        // Generamos la misma clave de 16 bytes a partir de la misma frase que se usó para cifrar

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = Arrays.copyOf(digest.digest(FRASE.getBytes(StandardCharsets.UTF_8)), 16); // Usamos solo los primeros 16 bytes (128 bits)
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");  // Creamos la clave secreta usando AES

        // Creamos un objeto Cipher para descifrar con AES en modo ECB y con relleno PKCS5
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);  // Inicializamos el cifrador en modo descifrado

        // Decodificamos el texto cifrado en Base64 para obtener los bytes originales cifrados
        byte[] passwordCifradaBytes = Base64.getDecoder().decode(passwordCifradaBase64);

        // Desciframos la contraseña utilizando el método doFinal()
        byte[] passwordDescifrada = cipher.doFinal(passwordCifradaBytes);

        // Convertimos los bytes descifrados de vuelta a una cadena de texto
        return new String(passwordDescifrada);
    }

}
