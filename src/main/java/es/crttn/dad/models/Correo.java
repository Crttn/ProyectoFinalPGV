package es.crttn.dad.models;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.text.SimpleDateFormat;

public class Correo {
    private String remitente;
    private String asunto;
    private String fecha;  // La fecha se mantendrá como String con el formato yyyy-MM-dd
    private String contenido;

    // Constructor que acepta un objeto Message (para correos de correo electrónico)
    public Correo(Message message) throws MessagingException {
        this.remitente = ((InternetAddress) message.getFrom()[0]).getAddress();
        this.asunto = message.getSubject();

        // Convertir fecha de Message a String en formato yyyy-MM-dd (sin la hora)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.fecha = sdf.format(message.getSentDate()).substring(0, 10);  // Solo fecha (sin hora)

        try {
            this.contenido = message.getContent().toString();
        } catch (Exception e) {
            this.contenido = "(No se pudo cargar el contenido)";
        }
    }

    // Constructor adicional para crear un Correo a partir de datos recuperados de la base de datos
    public Correo(String remitente, String asunto, String fecha, String contenido) {
        this.remitente = remitente;
        this.asunto = asunto;

        // Si la fecha tiene hora (yyyy-MM-dd HH:mm:ss), tomamos solo la parte de la fecha (yyyy-MM-dd)
        if (fecha != null && fecha.length() > 10) {
            this.fecha = fecha.substring(0, 10);  // Mantener solo la parte de la fecha
        } else {
            this.fecha = fecha;  // Usar la fecha tal como está si ya está en el formato correcto
        }

        this.contenido = contenido;
    }

    public String getRemitente() { return remitente; }
    public String getAsunto() { return asunto; }

    // Obtener la fecha tal como está en el formato yyyy-MM-dd
    public String getFecha() {
        return fecha != null ? fecha : "";
    }

    public String getContenido() { return contenido; }
}
