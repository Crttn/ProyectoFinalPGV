package es.crttn.dad;

import es.crttn.dad.controllers.MenuController;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

public class RootController implements Initializable {

    private static MenuController mc;

    @FXML
    private BorderPane root;

    public RootController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RootView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        mc = new MenuController();

        getRoot().setCenter(mc.getRoot());

        // Datos del correo
        String from = "usuario@localhost";  // El usuario creado en Mercury
        String to = "destinatario@localhost";  // Destinatario (puede ser también localhost)
        String subject = "Prueba de Envío de Correo";
        String body = "Este es un correo de prueba enviado desde Java utilizando Jakarta Mail.";

        // Configuración de las propiedades para la conexión con el servidor SMTP
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "localhost");  // Servidor SMTP de Mercury (localhost)
        properties.put("mail.smtp.port", "25");  // Puerto SMTP de Mercury
        properties.put("mail.smtp.auth", "true");  // Habilitar autenticación
        properties.put("mail.smtp.starttls.enable", "false");  // Desactivar TLS si no lo usas (en Mercury no es necesario)

        // Autenticación (puedes usar el mismo nombre de usuario y contraseña creados en Mercury)
        String username = "usuario@localhost";  // Nombre de usuario (el mismo que creaste en Mercury)
        String password = "12345";  // La contraseña del usuario en Mercury

        // Crear la sesión con autenticación
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // Crear el mensaje de correo
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));  // Dirección del remitente
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));  // Destinatario
            message.setSubject(subject);  // Asunto
            message.setText(body);  // Cuerpo del mensaje

            // Enviar el correo
            Transport.send(message);
            System.out.println("Correo enviado con éxito.");

        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    public BorderPane getRoot() {
        return root;
    }

    public static MenuController getMc() {
        return mc;
    }
}


