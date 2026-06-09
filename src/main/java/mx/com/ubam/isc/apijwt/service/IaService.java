package mx.com.ubam.isc.apijwt.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class IaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODELO = "llama3.2:3b";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public IaService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

  
  
    
    /**
     * Genera recomendaciones de tratamiento usando IA con mejor prompt
     */
    public String generarRecomendacionTratamiento(String diagnostico, String sintomas, String historialNotas) {
        String prompt = String.format("""
            Eres un psiquiatra con amplia experiencia. Un paciente ha sido diagnosticado con: "%s"
            
            Síntomas que presenta: "%s"
            
            Basado en esta información, genera un plan de tratamiento COMPLETO y DETALLADO siguiendo EXACTAMENTE este formato:
            
            ===DIAGNOSTICO===
            [Escribe aquí una explicación clara del diagnóstico en 2-3 líneas]
            
            ===TRATAMIENTO===
            [Escribe aquí el plan de tratamiento incluyendo:]
            - Tipo de terapia recomendada
            - Frecuencia de sesiones
            - Duración estimada del tratamiento
            - Si aplica, medicamentos sugeridos (siempre consultar con psiquiatra)
            
            ===CONSEJOS===
            [Escribe aquí 4-5 consejos prácticos que el paciente puede aplicar desde hoy, como:]
            - Técnicas de relajación
            - Hábitos de sueño
            - Ejercicio físico
            - Alimentación
            - Manejo del estrés
            
            IMPORTANTE: Responde ÚNICAMENTE con las tres secciones en el formato indicado. No agregues introducciones ni despedidas. Sé específico y práctico.
            """, diagnostico, sintomas);

        String respuesta = llamarOllama(prompt);
        
        if (respuesta == null || respuesta.trim().isEmpty()) {
            return generarRespuestaManual(diagnostico);
        }
        
        return respuesta;
    }

    private String generarRespuestaManual(String diagnostico) {
        // Respuesta de respaldo si la IA no funciona
        return String.format("""
            ===DIAGNOSTICO===
            %s
            
            ===TRATAMIENTO===
            • Terapia psicológica semanal (50 minutos)
            • Evaluación por psiquiatra cada 1-2 meses
            • Seguimiento regular de síntomas
            
            ===CONSEJOS===
            1. Mantén un horario regular de sueño
            2. Realiza ejercicio físico 30 minutos diarios
            3. Practica respiración profunda (4-7-8) cuando sientas ansiedad
            4. Evita alcohol y cafeína en exceso
            5. Mantén una red de apoyo social
            """, diagnostico);
    } 
    
    
    
    
    
    
    
    /**
     * Explica un diagnóstico en términos simples para el paciente
     */
    public String explicarDiagnostico(String diagnostico, String nivelDetalle) {
        String nivel = nivelDetalle.equals("simple") ? "muy simple, como si se lo explicaras a un adolescente" : "detallada pero clara";
        String prompt = String.format("""
            Eres un asistente de salud mental. Explica el diagnóstico '%s' de manera %s y empática.
            
            Incluye:
            - Qué significa el diagnóstico en términos simples
            - Síntomas comunes
            - Que es tratable
            - Un mensaje de esperanza
            
            Responde en máximo 150 palabras.
            """, diagnostico, nivel);

        return llamarOllama(prompt);
    }

    /**
     * Genera consejos de bienestar según categoría
     */
    public String generarConsejos(String categoria) {
        String prompt = String.format("""
            Eres un experto en bienestar mental. Genera 5 consejos prácticos sobre: %s
            
            Los consejos deben ser:
            - Accionables (que el usuario pueda hacer hoy)
            - Basados en evidencia
            - Escritos de forma amigable y alentadora
            
            Responde en formato de lista numerada.
            """, categoria);

        return llamarOllama(prompt);
    }

    /**
     * Chat libre con el paciente
     */
    public String chatRespuesta(String mensajeUsuario, String contextoPaciente) {
        String prompt = String.format("""
            Eres un asistente de salud mental empático y profesional.
            
            Contexto del paciente: %s
            
            Pregunta del paciente: %s
            
            Responde de manera útil, compasiva y breve (máximo 100 palabras).
            Si el paciente muestra signos de crisis, recomienda contactar a un profesional de inmediato.
            """, contextoPaciente, mensajeUsuario);

        return llamarOllama(prompt);
    }

    /**
     * Método que llama a Ollama vía API REST directa
     */
    private String llamarOllama(String prompt) {
        try {
            System.out.println("🤖 Enviando prompt a Ollama...");
            System.out.println("📝 Prompt: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");

            // Construir el cuerpo de la petición
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODELO);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "num_predict", 500
            ));

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // Crear la petición HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Enviar y obtener respuesta
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parsear la respuesta JSON
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                String respuesta = (String) responseMap.get("response");
                System.out.println("✅ Respuesta recibida de Ollama");
                return respuesta;
            } else {
                System.err.println("❌ Error HTTP: " + response.statusCode());
                return "⚠️ Error al comunicarse con el servicio de IA. Código: " + response.statusCode();
            }

        } catch (java.net.ConnectException e) {
            System.err.println("❌ No se pudo conectar a Ollama. ¿Está ejecutándose?");
            return "⚠️ No se pudo conectar al servicio de IA. Asegúrate de que Ollama esté ejecutándose.\n" +
                   "Para iniciarlo, abre una terminal y ejecuta: ollama run llama3.2:3b";
        } catch (Exception e) {
            System.err.println("❌ Error llamando a Ollama: " + e.getMessage());
            e.printStackTrace();
            return "⚠️ Error al procesar la solicitud de IA: " + e.getMessage();
        }
    }

    /**
     * Verifica si Ollama está disponible
     */
    public boolean isOllamaDisponible() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}