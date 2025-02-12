package es.crttn.dad.controllers;

import es.crttn.dad.App;
import es.crttn.dad.CifradoHelper;
import es.crttn.dad.DatabaseHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    InboxController ic;

    @FXML
    private BorderPane root;

    @FXML
    private TextField userIdLabel;

    @FXML
    private PasswordField userPassField;

    public MenuController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ic = new InboxController();
    }

    public BorderPane getRoot() {
        return root;
    }

    // Método para manejar el login de usuarios
    @FXML
    void onLoginAction(ActionEvent event) throws Exception {

        // Se obtienen las credenciales del usuario
        String user = userIdLabel.getText();
        String password = userPassField.getText();

        // Recuperar la contraseña cifrada desde la base de datos (en Base64)
        String passwordCifradaBase64 = DatabaseHelper.comprobarUsuario(user); // Esto debería devolver la contraseña cifrada en Base64
        String passwordFinal; // String para la contrasela sin descifrar

        // Utilizamos returns para no tener que inicializar passwordFinal
        try {
            // Descifrar la contraseña cifrada usando el método descifrar
            passwordFinal = CifradoHelper.descifrar(passwordCifradaBase64);
        } catch (IllegalArgumentException e) {
            System.out.println("Error al decodificar la contraseña: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.out.println("Error inesperado: " + e.getMessage());
            return;
        }

        // Verificar el ID de usuario en la base de datos
        int idUsuario = DatabaseHelper.obtenerIdUsuario(user);

        // Si el usuario no existe, registrarlo
        if (idUsuario == -1) {
            // Si el registro fue exitoso ingresa al Inbox
            if(DatabaseHelper.registrarUsuario(user, password) != -1 ) {
                System.out.println("Nuevo usuario registrado con éxito.");
                App.getRc().getRoot().setCenter(ic.getRoot()); // Cambiar la vista a Inbox
            }
        }

        // Si el usuario existe, comparar la contraseña
        else if (password.equals(passwordFinal)) {
            System.out.println("Inicio de sesión exitoso.");
            App.getRc().getRoot().setCenter(ic.getRoot()); // Cambiar la vista a Inbox
        } else {
            System.out.println("Error: Usuario o contraseña incorrectos.");
        }
    }

    // Metodo para obtener el usuario desde otro controlador
    public String getUserIdLabel() {
        return userIdLabel.getText();
    }

    // Metodo para obtener la contraseña desde otro controlador
    public String getUserPassLabel() {
        return userPassField.getText();
    }
}
