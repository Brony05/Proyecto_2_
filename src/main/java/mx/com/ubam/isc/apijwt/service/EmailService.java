package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envía el correo de forma ASÍNCRONA (hilo aparte) para que el endpoint
     * que agendar/cancelar/reagendar una cita no se quede esperando al SMTP.
     * Así el usuario recibe la respuesta del backend de inmediato y se
     * evitan dobles clicks por pantalla congelada.
     */
    @Async("emailExecutor")
    public void enviarRecordatorio(String para, String asunto, String mensaje) {
        try {
            System.out.println("📧 [ASYNC] Enviando correo a: " + para);
            System.out.println("📧 [ASYNC] Desde: " + fromEmail);
            
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(para);
            email.setSubject(asunto);
            email.setText(mensaje);
            
            mailSender.send(email);
            System.out.println("✅ [ASYNC] Correo enviado exitosamente a: " + para);
            
        } catch (Exception e) {
            System.err.println("❌ [ASYNC] Error al enviar correo: " + e.getMessage());
        }
    }
}