package es.crttn.dad.controllers;

import es.crttn.dad.DatabaseHelper;
import es.crttn.dad.RootController;
import es.crttn.dad.models.Correo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

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
    private Label emailLabel;

    @FXML
    private BorderPane root;

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

        // Obtiene el item de la lista de carpetas
        listCarpetas.getSelectionModel().select("Bandeja de entrada");

        // Relaciona las columnas de tabla con los datos del modelo
        colRemitente.setCellValueFactory(new PropertyValueFactory<>("remitente"));
        colAsunto.setCellValueFactory(new PropertyValueFactory<>("asunto"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        // Inicializa las listas de correos
        inboxList = FXCollections.observableArrayList();
        sendList = FXCollections.observableArrayList();

        // Carga los datos de inboxList en la tabla
        tablaCorreos.setItems(inboxList);

        // Crea un listener para cambiar entra la bandeja de entrada y enviados
        listCarpetas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {

            // Obtiene las credenciales introducidas en en loginMenu
            String userEmail = RootController.getMc().getUserIdLabel();
            String userPass = RootController.getMc().getUserPassLabel();

            // Coloca correo de usuario en la interfaz
            emailLabel.setText(userEmail + "@localhost");

            // Carga los corres desde el POP3
            cargarCorreos("127.0.0.1", userEmail, userPass);

            // Cambia los datos de las listas según la opción seleccionada
            if ("Bandeja de entrada".equals(newVal)) {
                colRemitente.setText("Remitente");  // Actuliza el nombre de la columna
                inboxList.clear();
                cargarCorreos("127.0.0.1", userEmail, userPass); // Carga los corres desde el POP3
                cargarCorreosRecibidos(userEmail); // Carga los correos desde la base de datos
                tablaCorreos.setItems(inboxList); // Asegurar que se usa la lista correcta
            } else if ("Enviados".equals(newVal)) {
                colRemitente.setText("Destinatario");
                sendList.clear();
                cargarCorreosEnviados(userEmail);
                tablaCorreos.setItems(sendList);
            } else {
                // Si no hay opción seleccionada se muestra la tabla vacia
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

    // Carga los correos desde la base de datos
    private void cargarCorreosRecibidos(String userEmail) {

        // Recupera el id del usuario
        int idUsuario = DatabaseHelper.obtenerIdUsuario(userEmail);

        // Si el usuario es -1 muestra un mensaje de error
        if (idUsuario == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo encontrar el usuario en la base de datos.");
            alert.showAndWait();
            return;
        }

        inboxList.setAll(DatabaseHelper.obtenerCorreosRecibidos(idUsuario)); // Cargar correos desde la BD
        tablaCorreos.setItems(inboxList); // Mostrar en la tabla con los datos
    }

    // Metodo principal para cargar correos desde el servidor POP3
    private void cargarCorreos(String host, String username, String password) {

        // Se crean las properties de configuración para la conexión POP3
        Properties properties = new Properties();
        properties.put("mail.pop3.host", host); // Dirección del servidor
        properties.put("mail.pop3.port", "110"); // Puerto
        properties.put("mail.pop3.ssl.enable", "false"); // Desactiva el ssl pq daba problemas

        try {
            // Crea una sesión de correo con las propiedades establecidas
            Session session = Session.getInstance(properties);
            Store store = session.getStore("pop3"); // Obtiene un almacén POP3
            store.connect(host, username, password); // Conecta al servidor con las credenciales

            // Abre la carpeta de entrada (bandeja de entrada) en modo solo lectura
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Obtiene todos los mensajes de la bandeja de entrada
            Message[] messages = inbox.getMessages();

            // Limpiar lista antes de agregar nuevos correos
            inboxList.clear();

            // Obtiene el ID del usuario de la base de datos
            int userID = DatabaseHelper.obtenerIdUsuario(username);

            // Recorre los mensajes recuperados
            for (Message message : messages) {
                Correo correo = new Correo(message); // Crea un objeto Correo con el mensaje
                if (!DatabaseHelper.existeCorreoEnBD(correo)) {  // Verifica si ya está guardado
                    DatabaseHelper.guardarCorreoRecibido(userID, correo); // Guarda en BD
                }
            }

            // Cierra la bandeja de entrada sin eliminar correos
            inbox.close(false);
            store.close(); // Cierra la conexión con el servidor
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga los correos Enviados desde la base de datos
    private void cargarCorreosEnviados(String userEmail) {
        // Recupera el id del usuario
        int idUsuario = DatabaseHelper.obtenerIdUsuario(userEmail);

        // Comprueba que el usuario existe
        if (idUsuario == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo encontrar el usuario en la base de datos.");
            alert.showAndWait();
            return;
        }

        sendList.setAll(DatabaseHelper.obtenerCorreosEnviados(idUsuario)); // Obtener correos enviados desde la base de datos
        tablaCorreos.setItems(sendList); // Actualizar la tabla con los enviados
    }

    // Método para enviar correos
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

        // Manejar el botón de envío en el cuadro de diálogo
        dialog.setResultConverter(buttonType -> {
            if (buttonType == enviarButton) { // Verifica si el botón presionado es el de enviar
                // Se llama al método para enviar coreos
                enviarCorreo(txtPara.getText(), txtAsunto.getText(), txtMensaje.getText());
            }
            return null;
        });

        //  Muestra el dialogo
        dialog.showAndWait();
    }

    private static void enviarCorreo(String destinatario, String asunto, String mensaje) {

        // Recupera el nombre del usuario
        String user = RootController.getMc().getUserIdLabel();
        // Recupera el id del usuario desde la base de datos
        int idUsuario = DatabaseHelper.obtenerIdUsuario(user);

        // Comprueba si el usuario existe
        if (idUsuario == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo encontrar el usuario en la base de datos.");
            alert.showAndWait();
            return;
        }


        try {
            // Crea un nuevo correo usando la biblioteca SimpleMail
            Email email = new SimpleEmail();

            // Configurar el servidor SMTP
            email.setHostName("127.0.0.1"); // Servidor SMTP
            email.setSmtpPort(25); // Puerto
            email.setSSLOnConnect(false); // Autentificación descativada porque daba problemas

            // Configurar los datos del correo
            email.setFrom(user + "@localhost"); // Remitente del correo + @localhost pq todos son locales
            email.setSubject(asunto); // Asunto del correo
            email.setMsg(mensaje); // Cuerpo del mensaje
            email.addTo(destinatario); // Destinatario del correo

            // Envia el correo
            email.send();

            // Se guarda el correo en la base de datos
            DatabaseHelper.guardarCorreoEnviado(idUsuario, destinatario, asunto, mensaje);

            // Mostrar una alerta informando que el correo fue enviado con éxito
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Correo enviado");
            alert.setHeaderText(null);
            alert.setContentText("¡El correo fue enviado con éxito!");
            alert.showAndWait();
        } catch (Exception e) {
            // Muestra un mensaje en caso de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al enviar");
            alert.setHeaderText(null);
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Método para filtrar correos por Remitente
    @FXML
    void onRemFilterAction(ActionEvent event) {

        // Crear el cuadro de diálogo
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Filtrar Correos por Remitente");

        // Campo de entrada
        TextField txtRemitente = new TextField();
        txtRemitente.setPromptText("Ingresa el remitente");

        // Diseño del cuadro de diálogo
        VBox content = new VBox(10, new Label("Remitente:"), txtRemitente);
        dialog.getDialogPane().setContent(content);

        // Botones
        ButtonType filtroButton = new ButtonType("Filtrar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(filtroButton, cancelarButton);

        dialog.setResultConverter(buttonType -> {
            // Verifica el botón presionado
            if (buttonType == filtroButton) {
                // Si es precionado el botón de filtro devuelve el texto ingresado
                return txtRemitente.getText();
            }
            return null;
        });

        // Muestra el diálogo y, si el usuario ha proporcionado un remitente, aplica el filtro
        dialog.showAndWait().ifPresent(remitente -> {
            // Llama al método para aplicar el filtro por remitente con el valor ingresado
            aplicarFiltroPorRemitente(remitente);
        });
    }

    // Aplica un filtro de correos por Remitente
    private void aplicarFiltroPorRemitente(String filtro) {

        // Se crean dos listas observables vacías para almacenar los correos filtrados
        ObservableList<Correo> listaFiltradaBandeja = FXCollections.observableArrayList();
        ObservableList<Correo> listaFiltradaEnviados = FXCollections.observableArrayList();

        // Filtra los correos en la bandeja de entrada (inboxList)
        for (Correo correo : inboxList) {
            // Compara el remitente de cada correo con el filtro
            if (correo.getRemitente().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaBandeja.add(correo); // Agrega el correo a la lista filtrada si coincide
            }
        }

        // Filtra los correos en la bandeja de entrada (sendList)
        for (Correo correo : sendList) {
            // Compara el remitente de cada correo con el filtro
            if (correo.getRemitente().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaEnviados.add(correo); // Agrega el correo a la lista filtrada si coincide
            }
        }

        // Actualiza las listas observables con los correos filtrados
        inboxList.setAll(listaFiltradaBandeja);
        sendList.setAll(listaFiltradaEnviados);
    }

    // Método para filtrar correos por Asunto
    @FXML
    void onAsuFilterAction(ActionEvent event) {
        // Crea el cuadro de diálogo
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Filtrar Correos por Asunto");

        // Campo de entrada
        TextField txtAsunto = new TextField();
        txtAsunto.setPromptText("Ingresa el asunto");

        // Diseño del diálogo
        VBox content = new VBox(10, new Label("Asunto:"), txtAsunto);
        dialog.getDialogPane().setContent(content);

        // Botones
        ButtonType filtroButton = new ButtonType("Filtrar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(filtroButton, cancelarButton);

        dialog.setResultConverter(buttonType -> {
            // Verifica el botón presionado
            if (buttonType == filtroButton) {
                // Si es precionado el botón de filtro devuelve el texto ingresado
                return txtAsunto.getText();
            }
            return null;
        });

        // Muestra el diálogo y, si el usuario ha proporcionado un remitente, aplica el filtro
        dialog.showAndWait().ifPresent(asunto -> {
            // Llama al método para aplicar el filtro por asunto con el valor ingresado
            aplicarFiltroPorAsunto(asunto);
        });
    }

    // Aplica un filtro de correos por Asunto
    private void aplicarFiltroPorAsunto(String filtro) {

        // Se crean dos listas observables vacías para almacenar los correos filtrados
        ObservableList<Correo> listaFiltradaBandeja = FXCollections.observableArrayList();
        ObservableList<Correo> listaFiltradaEnviados = FXCollections.observableArrayList();

        // Filtra los correos en la bandeja de entrada (inboxList)
        for (Correo correo : inboxList) {
            // Compara el asunto de cada correo con el filtro
            if (correo.getAsunto().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaBandeja.add(correo); // Agrega el correo a la lista filtrada si coincide
            }
        }

        // Filtra los correos en la bandeja de entrada (sendList)
        for (Correo correo : sendList) {
            // Compara el asunto de cada correo con el filtro
            if (correo.getAsunto().toLowerCase().contains(filtro.toLowerCase())) {
                listaFiltradaEnviados.add(correo); // Agrega el correo a la lista filtrada si coincide
            }
        }

        // Actualiza las listas observables con los correos filtrados
        inboxList.setAll(listaFiltradaBandeja);
        sendList.setAll(listaFiltradaEnviados);
    }
}
