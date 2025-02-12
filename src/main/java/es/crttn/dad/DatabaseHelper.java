package es.crttn.dad;

import es.crttn.dad.models.Correo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    // Constantes para conectar con la base de datos
    private static final String URL = "jdbc:mysql://localhost:3306/gestionemails";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";


    // Método para establecer una conexión con la base de datos
    public static Connection conectar() throws SQLException {
        // Utiliza DriverManager para obtener una conexión con la base de datos
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Método para registrar usuarios en la base de datos
    public static int registrarUsuario(String correo, String password) {

        // String para la contraseña cifrada
        String passwordCifrada;

        try {
            // Ciframos la contraseña antes de almacenarla
            passwordCifrada = CifradoHelper.cifrar(password);
        } catch (Exception e) {
            // En caso de error al cifrar, se imprime el error y se retorna -1 para manejar errores en otros métodos
            e.printStackTrace();
            return -1;
        }

        // Verificamos si el usuario ya existe en la base de datos antes de insertarlo
        if (obtenerIdUsuario(correo) != -1) {
            System.out.println("El usuario ya existe.");
            return -1;  // El usuario ya existe, no se puede registrar de nuevo
        }

        // Consulta SQL para insertar el usuario y la contraseña cifrada
        String sql = "INSERT INTO usuarios (email, password) VALUES (?, ?)";

        // Se establece la conexión con la base de datos y se prepara la consulta SQL,
        // indicando que queremos recuperar las claves generadas automáticamente (como IDs).
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Establecemos los valores de los parámetros de la consulta
            pstmt.setString(1, correo); // Almacena el nombre del usuario
            pstmt.setString(2, passwordCifrada);  // Almacenamos la contraseña cifrada en Base64
            pstmt.executeUpdate(); // Ejecuta la consulta

            // Obtenemos el ID del nuevo usuario generado por la base de datos
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);  // Retorna el ID del nuevo usuario
            }
        } catch (SQLException e) {
            // En caso de error de SQL, imprimimos el error
            e.printStackTrace();
        }
        return -1;  // Retorna -1 si algo salió mal
    }

    // Método para obtener el ID de un usuario
    public static int obtenerIdUsuario(String correo) {

        // Declaramos el id en -1 por si no existe el usuario
        int idUsuario = -1;

        // Creamos la consulta
        String query = "SELECT id FROM usuarios WHERE email = ?";

        // Establecemos la conexión con la base de datos y preparamos la consulta SQL para su ejecución
        try (Connection connection = conectar(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Establecer el valor del parámetro email en la consulta
            preparedStatement.setString(1, correo);

            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();

            // Verificamos si hay al menos una fila en el ResultSet
            if (resultSet.next()) {
                idUsuario = resultSet.getInt("id"); // Recuperamos el ID del usuario
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Imprimir el error si ocurre alguna excepción
        }

        return idUsuario; // Retornar el ID o -1 si da error
    }

    // Comprueba que existan los usuarios en la base de datos y retorna la contraseña en Base64
    public static String comprobarUsuario(String emailUser) {

        // String para almacenar la contraseña cifrada en Base64
        String passwordBase64 = "";

        // Consulta SQL para obtener la contraseña cifrada en Base64 del usuario
        String query = "SELECT password FROM usuarios WHERE email = ?";

        // Crea la conexión a la base de datos y preparamos la consulta SQL para su ejecución
        try (Connection connection = conectar(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Establecer el valor del parámetro email en la consulta
            preparedStatement.setString(1, emailUser); // Establecer el valor del parámetro email en la consulta

            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();

            // Si el usuario existe, recuperamos la contraseña cifrada en Base64
            if (resultSet.next()) {
                passwordBase64 = resultSet.getString("password"); // Aquí obtenemos la contraseña cifrada en Base64
            } else {
                System.out.println("Usuario no encontrado.");
            }
        } catch (SQLException e) {
            // En caso de error de SQL, imprimimos el error
            e.printStackTrace();
        }

        return passwordBase64; // Devolver la contraseña cifrada (Base64) o una cadena vacía si no se encontró el usuario
    }

    // Método para guardar correos recibidos en la base de datos
    public static void guardarCorreoRecibido(int idUsuario, Correo correo) {

        // Consulta sql para insertar un correo en la base de datos
        String sql = "INSERT INTO correos_recibidos (id_usuario, remitente, asunto, cuerpo, fecha) VALUES (?, ?, ?, ?, ?)";

        // Crea la conexión con la base de datos y la preparamos para realizar la inserción
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Establecer los valorres de los parámetros de la consulta
            pstmt.setInt(1, idUsuario);  // Agregar el id_usuario obligatorio
            pstmt.setString(2, correo.getRemitente());
            pstmt.setString(3, correo.getAsunto());
            pstmt.setString(4, correo.getContenido());
            pstmt.setString(5, correo.getFecha());

            // Ejecutamso la consulta
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para guardar un correo enviado
    public static void guardarCorreoEnviado(int idUsuario, String destinatario, String asunto, String cuerpo) {

        // Consulta SQL para insertar los datos del correo
        String sql = "INSERT INTO correos_enviados (id_usuario, destinatario, asunto, cuerpo, fecha) VALUES (?, ?, ?, ?, CURDATE())";

        // Establecemos la conexión con la base de datos y preparamos la consulta SQL para insertar los datos,
        // luego asignamos los valores a los parámetros y ejecutamos la actualización en la base de datos.
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);  // Asignamos el ID del usuario
            pstmt.setString(2, destinatario);  // Asignamos el destinatario
            pstmt.setString(3, asunto);  // Asignamos el asunto del correo
            pstmt.setString(4, cuerpo);  // Asignamos el cuerpo del correo
            pstmt.executeUpdate();  // Ejecutamos la actualización
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para obtener los correos recibidos y obtener una lista de correos
    public static List<Correo> obtenerCorreosRecibidos(int idUsuario) {

        // Crea una lista para almacenar objetos de "Correo"
        List<Correo> correosRecibidos = new ArrayList<>();

        // Consulta SQL para obtener los datos de la tabla correos_recibidos
        String query = "SELECT remitente, asunto, fecha, cuerpo FROM correos_recibidos WHERE id_usuario = ?";

        // Establecemos la conexión con la base de datos y preparamos la consulta SQL para insertar los datos
        try (Connection connection = conectar(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Establecer el valor del parámetro email en la consulta
            preparedStatement.setInt(1, idUsuario);

            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();

            // Itera sobre los resultados del ResultSet, recuperando los datos de cada correo
            while (resultSet.next()) {
                // Recupera los datos
                String remitente = resultSet.getString("remitente");
                String asunto = resultSet.getString("asunto");
                String fecha = resultSet.getString("fecha");
                String cuerpo = resultSet.getString("cuerpo");

                // Crea un objeto "Correo" con esos datos y lo agrega a la lista de correos recibidos
                Correo correo = new Correo(remitente, asunto, fecha, cuerpo);
                correosRecibidos.add(correo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Devuelve la lista de correos
        return correosRecibidos;
    }

    // Método para obtener los correos enviados y obtener una lista de correos
    public static List<Correo> obtenerCorreosEnviados(int idUsuario) {

        // Crea una lista para almacenar objetos de "Correo"
        List<Correo> correosEnviados = new ArrayList<>();

        // Consulta SQL para obtener los datos de la tabla correos_enviados
        String query = "SELECT destinatario, asunto, fecha, cuerpo FROM correos_enviados WHERE id_usuario = ?";

        // Establecemos la conexión con la base de datos y preparamos la consulta SQL para insertar los datos
        try (Connection connection = conectar(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Establecer el valor del parámetro email en la consulta
            preparedStatement.setInt(1, idUsuario);

            // Ejecutamos la consulta
            ResultSet resultSet = preparedStatement.executeQuery();

            // Itera sobre los resultados del ResultSet, recuperando los datos de cada correo
            while (resultSet.next()) {
                // Recuperamos los datos
                String destinatario = resultSet.getString("destinatario");
                String asunto = resultSet.getString("asunto");
                String fechaStr = resultSet.getString("fecha"); // Fecha en formato String
                String cuerpo = resultSet.getString("cuerpo");

                // Creamos un objeto correo y lo agrega a la lista de correos enviados
                Correo correo = new Correo(destinatario, asunto, fechaStr, cuerpo);
                correosEnviados.add(correo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Devolvemos la lista
        return correosEnviados;
    }

    // Método para comprobar si existen los correos en la base de datos
    public static boolean existeCorreoEnBD(Correo correo) {

        // Consutla sql para comprobar el correo con sus datos
        String sql = "SELECT COUNT(*) FROM correos_recibidos WHERE remitente = ? AND asunto = ? AND fecha = ?";

        // Cramos una conexion a la base de datos y la preparamos para buscar los datos
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Establecer el valor del parámetro email en la consulta
            pstmt.setString(1, correo.getRemitente());
            pstmt.setString(2, correo.getAsunto());
            pstmt.setString(3, correo.getFecha());

            // Ejecuta la consulta
            ResultSet rs = pstmt.executeQuery();

            // Busca por todos los datos
            if (rs.next()) {
                // Verifica si hay coincidencias en la base de datos
                return rs.getInt(1) > 0; // Si es así devuelve true
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Si no se encuentran correos devuelve false
        return false;
    }
}

