package es.crttn.dad;

import es.crttn.dad.models.Correo;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DatabaseHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/gestionemails";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    private static CifradoHelper cfh = new CifradoHelper();

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static int registrarUsuario(String correo, String password) {
        String passwordCifrada = "";
        try {
            // Ciframos la contraseña antes de almacenarla
            passwordCifrada = CifradoHelper.cifra(password);
        } catch (Exception e) {
            // En caso de error al cifrar, se imprime el error y se retorna -1
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
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, correo);
            pstmt.setString(2, passwordCifrada);  // Almacenamos la contraseña cifrada en Base64
            pstmt.executeUpdate();

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

    public static String comprobarUsuario(String emailUser) {
        String passwordBase64 = "";

        // Consulta SQL para obtener la contraseña cifrada en Base64 del usuario
        String query = "SELECT password FROM usuarios WHERE email = ?";

        try (Connection connection = conectar();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, emailUser); // Establecer el valor del parámetro email en la consulta

            ResultSet resultSet = preparedStatement.executeQuery(); // Ejecutar la consulta

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

    public static int obtenerIdUsuario(String correo) {
        int idUsuario = -1;

        String query = "SELECT id FROM usuarios WHERE email = ?";
        try (Connection connection = conectar();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, correo); // Establecer el valor del parámetro email en la consulta

            ResultSet resultSet = preparedStatement.executeQuery(); // Ejecutar la consulta

            if (resultSet.next()) {
                idUsuario = resultSet.getInt("id"); // Recuperamos el ID del usuario
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Imprimir el error si ocurre alguna excepción
        }

        return idUsuario; // Retornar -1 si no se encuentra el usuario
    }


    public static void guardarCorreoEnviado(int idUsuario, String destinatario, String asunto, String cuerpo) {
        String sql = "INSERT INTO correos_enviados (id_usuario, destinatario, asunto, cuerpo, fecha) VALUES (?, ?, ?, ?, CURDATE())";

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setString(2, destinatario);
            pstmt.setString(3, asunto);
            pstmt.setString(4, cuerpo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Correo> obtenerCorreosRecibidos(int idUsuario) {
        List<Correo> correosRecibidos = new ArrayList<>();

        String query = "SELECT remitente, asunto, fecha, cuerpo FROM correos_recibidos WHERE id_usuario = ?";

        try (Connection connection = conectar();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, idUsuario);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String remitente = resultSet.getString("remitente");
                String asunto = resultSet.getString("asunto");
                String fecha = resultSet.getString("fecha");
                String cuerpo = resultSet.getString("cuerpo");

                Correo correo = new Correo(remitente, asunto, fecha, cuerpo);
                correosRecibidos.add(correo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return correosRecibidos;
    }


    public static List<Correo> obtenerCorreosEnviados(int idUsuario) {
        List<Correo> correosEnviados = new ArrayList<>();

        String query = "SELECT c.destinatario, c.asunto, c.fecha, c.cuerpo " +
                "FROM correos_enviados c " +
                "WHERE c.id_usuario = ?";

        try (Connection connection = conectar();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, idUsuario);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String destinatario = resultSet.getString("destinatario");
                String asunto = resultSet.getString("asunto");
                String fechaStr = resultSet.getString("fecha"); // Fecha en formato String
                String cuerpo = resultSet.getString("cuerpo");

                Correo correo = new Correo(destinatario, asunto, fechaStr, cuerpo);
                correosEnviados.add(correo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return correosEnviados;
    }

    public static boolean existeCorreoEnBD(Correo correo) {
        String sql = "SELECT COUNT(*) FROM correos_recibidos WHERE remitente = ? AND asunto = ? AND fecha = ?";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correo.getRemitente());
            pstmt.setString(2, correo.getAsunto());
            pstmt.setString(3, correo.getFecha());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void guardarCorreoEnBD(int idUsuario, Correo correo) {
        String sql = "INSERT INTO correos_recibidos (id_usuario, remitente, asunto, cuerpo, fecha) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idUsuario);  // Agregar el id_usuario obligatorio
            pstmt.setString(2, correo.getRemitente());
            pstmt.setString(3, correo.getAsunto());
            pstmt.setString(4, correo.getContenido());
            pstmt.setString(5, correo.getFecha());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

