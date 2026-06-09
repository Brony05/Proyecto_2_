package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void enviarRecordatorio(String para, String asunto, String mensaje) {
        try {
            System.out.println("📧 Intentando enviar correo a: " + para);
            System.out.println("📧 Desde: " + fromEmail);
            
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(para);
            email.setSubject(asunto);
            email.setText(mensaje);
            
            mailSender.send(email);
            System.out.println("✅ Correo enviado exitosamente a: " + para);
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}