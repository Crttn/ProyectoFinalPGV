package es.crttn.dad.controllers;

import es.crttn.dad.App;
import es.crttn.dad.CifradoHelper;
import es.crttn.dad.DatabaseHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
    private TextField userPassLabel;

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

    @FXML
    void onLoginAction(ActionEvent event) throws Exception {
        String user = userIdLabel.getText();
        String password = userPassLabel.getText();

        // Recuperar la contraseña cifrada desde la base de datos (en Base64)
        String passwordCifradaBase64 = DatabaseHelper.comprobarUsuario(user); // Esto debería devolver la contraseña cifrada en Base64
        String passwordFinal;

        try {
            // Descifrar la contraseña cifrada usando el método descifra
            passwordFinal = CifradoHelper.descifra(passwordCifradaBase64);
        } catch (IllegalArgumentException e) {
            System.out.println("Error al decodificar la contraseña: " + e.getMessage());
            return; // Salir del método si hay un error al descifrar
        } catch (Exception e) {
            System.out.println("Error inesperado: " + e.getMessage());
            return; // Salir del método si ocurre un error inesperado
        }

        // Verificar el ID de usuario en la base de datos
        int idUsuario = DatabaseHelper.obtenerIdUsuario(user);

        // Si el usuario no existe, registrarlo
        if (idUsuario == -1) {
            // Cifrar la contraseña antes de registrarla
            DatabaseHelper.registrarUsuario(user, password); // Registramos usuario y contraseña cifrada
            System.out.println("Nuevo usuario registrado con éxito.");
            App.getRc().getRoot().setCenter(ic.getRoot()); // Cambiar la pantalla o vista
        }
        // Si el usuario existe, comparar la contraseña
        else if (password.equals(passwordFinal)) {
            System.out.println("Inicio de sesión exitoso.");
            App.getRc().getRoot().setCenter(ic.getRoot()); // Cambiar la pantalla o vista
        } else {
            System.out.println("Error: Usuario o contraseña incorrectos.");
        }
    }






    public String getUserIdLabel() {
        return userIdLabel.getText();
    }

    public String getUserPassLabel() {
        return userPassLabel.getText();
    }
}
