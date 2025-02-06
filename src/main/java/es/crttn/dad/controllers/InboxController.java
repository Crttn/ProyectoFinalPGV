package es.crttn.dad.controllers;

import es.crttn.dad.CifradoHelper;
import es.crttn.dad.DatabaseHelper;
import es.crttn.dad.RootController;
import es.crttn.dad.models.Correo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InboxController implements Initializable {

    @FXML
    private TableView<Correo> tablaCorreos;

    @FXML
    private TableColumn<Correo, String> colRemitente;

    @FXML
    private TableColumn<Correo, String> colAsunto;

    @FXML
    private TableColumn<Correo, String> colFecha;

    @FXML
    private ListView<String> listCarpetas;

    @FXML
    private TextArea txtVistaCorreo;

    @FXML
    private BorderPane root;

    private ScheduledExecutorService scheduler;

    private ObservableList<Correo> inboxList;
    private ObservableList<Correo> sendList;

    public InboxController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/InboxView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        listCarpetas.getSelectionModel().select("Bandeja de entrada");

        colRemitente.setCellValueFactory(new PropertyValueFactory<>("remitente"));
        colAsunto.setCellValueFactory(new PropertyValueFactory<>("asunto"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        inboxList = FXCollections.observableArrayList();
        sendList = FXCollections.observableArrayList();

        tablaCorreos.setItems(inboxList);

        listCarpetas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            String userEmail = RootController.getMc().getUserIdLabel();
            String userPass = RootController.getMc().getUserPassLabel();

            iniciarActualizacionAutomatica("127.0.0.1", userEmail, userPass);

            if ("Bandeja de entrada".equals(newVal)) {
                colRemitente.setText("Remitente");
                inboxList.clear();  // Limpiar lista antes de recargar
                cargarCorreosDesdeBD(userEmail);
                tablaCorreos.setItems(inboxList); // Asegurar que se usa la lista correcta
            } else if ("Enviados".equals(newVal)) {
                colRemitente.setText("Destinatario");
                sendList.clear();
                cargarCorreosEnviados(userEmail);
                tablaCorreos.setItems(sendList); // Asegurar que se usa la lista de enviados
            } else {
                inboxList.clear();
                sendList.clear();
            }
        });

        // Listener para mostrar el contenido del correo seleccionado
        tablaCorreos.getSelectionModel().selectedItemProperty().addListener((obs, oldCorreo, newCorreo) -> {
            if (newCorreo != null) {
                txtVistaCorreo.setText(newCorreo.getContenido());
            }
        });
    }

    public BorderPane getRoot() {
        return root;
    }

    private void cargarCorreosDesdeBD(String userEmail) {
        int idUsuario = DatabaseHelper.obtenerIdUsuario(userEmail);

        if (idUsuario == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo encontrar el usuario en la base de datos.");
            alert.showAndWait();
            return;
        }

        inboxList.setAll(DatabaseHelper.obtenerCorreosRecibidos(idUsuario)); // Cargar correos desde la BD
        tablaCorreos.setItems(inboxList); // Mostrar en la tabla
    }

    public void iniciarActualizacionAutomatica(String host, String username, String password) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                cargarCorreos(host, username, password); // Llamamos a tu método original
            });
        }, 0, 60, TimeUnit.SECONDS); // Se ejecuta cada 30 segundos
    }

    private void cargarCorreos(String host, String username, String password) {
        Properties properties = new Properties();
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", "110");
        properties.put("mail.pop3.ssl.enable", "false");

        try {
            Session session = Session.getInstance(properties);
            Store store = session.getStore("pop3");
            store.connect(host, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();

            inboxList.clear(); // Limpiar lista antes de agregar nuevos correos

            int userID = DatabaseHelper.obtenerIdUsuario(username);

            for (Message message : messages) {
                Correo correo = new Correo(message);
                if (!DatabaseHelper.existeCorreoEnBD(correo)) {  // Verifica si ya está guardado
                    DatabaseHelper.guardarCorreoEnBD(userID, correo);    // Guarda en BD
                }
                inboxList.add(correo);  // Agrega al ObservableList para la tabla
            }

            tablaCorreos.refresh();

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void cargarCorreosEnviados(String userEmail) {
        int idUsuario = DatabaseHelper.obtenerIdUsuario(userEmail);

        if (idUsuario == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo encontrar el usuario en la base de datos.");
            alert.showAndWait();
            return;
        }

        sendList.setAll(DatabaseHelper.obtenerCorreosEnviados(idUsuario)); // Obtener correos enviados
        tablaCorreos.setItems(sendList); // Actualizar la tabla con los enviados
    }

    @FXML
    void onSendEmailAction(ActionEvent event) {
        // Crear el cuadro de diálogo
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enviar Correo");
        dialog.setHeaderText("Complete los campos para enviar el correo");

        // Campos de entrada
        TextField txtPara = new TextField();
        txtPara.setPromptText("Destinatario (ejemplo@dominio.com)");

        TextField txtAsunto = new TextField();
        txtAsunto.setPromptText("Asunto del correo");

        TextArea txtMensaje = new TextArea();
        txtMensaje.setPromptText("Escribe el mensaje aquí...");
        txtMensaje.setWrapText(true);

        // Diseño del cuadro de diálogo
        VBox content = new VBox(10, new Label("Para:"), txtPara, new Label("Asunto:"), txtAsunto, new Label("Mensaje:"), txtMensaje);
        dialog.getDialogPane().setContent(content);

        ButtonType enviarButton = new ButtonType("Enviar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(enviarButton, cancelarButton);

        // Manejar botón de envío
        dialog.setResultConverter(buttonType -> {
            if (buttonType == enviarButton) {
                enviarCorreo(txtPara.getText(), txtAsunto.getText(), txtMensaje.getText());
            }
            return null;
        });

        dialog.showAndWait();
    }

    private static void enviarCorreo(String destinatario, String asunto, String mensaje) {

        String user = RootController.getMc().getUserIdLabel();
        int idUsuario = DatabaseHelper.obtenerIdUsuario(user);

        if (idUsuario == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo encontrar el usuario en la base de datos.");
            alert.showAndWait();
            return;
        }

        try {
            Email email = new SimpleEmail();
            email.setHostName("127.0.0.1"); // Servidor SMTP
            email.setSmtpPort(25);
            email.setSSLOnConnect(false);
            email.setFrom(user + "@localhost");
            email.setSubject(asunto);
            email.setMsg(mensaje);
            email.addTo(destinatario);
            email.send();

            DatabaseHelper.guardarCorreoEnviado(idUsuario, destinatario, asunto, mensaje);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Correo enviado");
            alert.setHeaderText(null);
            alert.setContentText("¡El correo fue enviado con éxito!");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al enviar");
            alert.setHeaderText(null);
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void onRemFilterAction(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Filtrar Correos por Remitente");

        TextField txtRemitente = new TextField();
        txtRemitente.setPromptText("Ingresa el remitente");

        VBox content = new VBox(10, new Label("Remitente:"), txtRemitente);
        dialog.getDialogPane().setContent(content);

        ButtonType filtroButton = new ButtonType("Filtrar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(filtroButton, cancelarButton);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == filtroButton) {
                return txtRemitente.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(remitente -> {
            aplicarFiltroPorRemitente(remitente);
        });
    }

    private void aplicarFiltroPorRemitente(String filtro) {

        ObservableList<Correo> listaFiltradaBandeja = FXCollections.observableArrayList();
        ObservableList<Correo> listaFiltradaEnviados = FXCollections.observableArrayList();

        for (Correo correo : inboxList) {
            if (correo.getRemitente().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaBandeja.add(correo);
            }
        }
        for (Correo correo : sendList) {
            if (correo.getRemitente().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaEnviados.add(correo);
            }
        }

        inboxList.setAll(listaFiltradaBandeja);
        sendList.setAll(listaFiltradaEnviados);
    }


    @FXML
    void onAsuFilterAction(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Filtrar Correos por Asunto");

        TextField txtAsunto = new TextField();
        txtAsunto.setPromptText("Ingresa el asunto");

        VBox content = new VBox(10, new Label("Asunto:"), txtAsunto);
        dialog.getDialogPane().setContent(content);

        ButtonType filtroButton = new ButtonType("Filtrar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(filtroButton, cancelarButton);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == filtroButton) {
                return txtAsunto.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(asunto -> {
            aplicarFiltroPorAsunto(asunto);
        });
    }

    private void aplicarFiltroPorAsunto(String filtro) {

        ObservableList<Correo> listaFiltradaBandeja = FXCollections.observableArrayList();
        ObservableList<Correo> listaFiltradaEnviados = FXCollections.observableArrayList();

        for (Correo correo : inboxList) {
            if (correo.getAsunto().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaBandeja.add(correo);
            }
        }

        for (Correo correo : sendList) {
            if (correo.getAsunto().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaEnviados.add(correo);
            }
        }

        inboxList.setAll(listaFiltradaBandeja);
        sendList.setAll(listaFiltradaEnviados);
    }
}
