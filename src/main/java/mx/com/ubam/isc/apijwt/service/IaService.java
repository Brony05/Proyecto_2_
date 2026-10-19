package mx.com.ubam.isc.apijwt.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class IaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODELO = "llama3.2:3b";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public IaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Genera el plan clínico (diagnóstico + tratamiento + consejos) para el DOCTOR.
     * La respuesta usa marcadores ===SECCION=== para que el frontend pueda partir el contenido.
     */
    public String generarRecomendacionTratamiento(String diagnostico, String sintomas, String historialNotas) {
        String prompt = String.format("""
            Eres un asistente clínico de un psiquiatra. El doctor diagnosticó: "%s"
            Síntomas reportados: "%s"
            %s
            Genera un plan COMPLETO usando EXACTAMENTE estas tres secciones, con estos marcadores exactos:

            ===DIAGNOSTICO===
            Explicación clínica breve del diagnóstico (2-3 líneas).

            ===TRATAMIENTO===
            - Tipo de terapia recomendada
            - Frecuencia y duración estimada de las sesiones
            - Farmacología sugerida solo si aplica (indicar que debe confirmarla el psiquiatra)

            ===CONSEJOS===
            4 o 5 consejos prácticos que el paciente puede empezar hoy (sueño, relajación, ejercicio, alimentación, red de apoyo).

            Responde SOLO con las tres secciones, sin introducciones ni despedidas.
            """,
                diagnostico,
                sintomas == null || sintomas.isEmpty() ? "No especificados" : sintomas,
                (historialNotas != null && !historialNotas.isBlank())
                        ? "Historial relevante: \"" + historialNotas + "\"" : "");

        String respuesta = llamarOllama(prompt, 500);

        if (respuesta == null || respuesta.isBlank() || respuesta.startsWith("⚠️")) {
            return generarRespuestaManual(diagnostico);
        }
        return normalizarSecciones(respuesta, diagnostico);
    }

    /**
     * Asegura que la respuesta tenga las tres secciones con los marcadores canónicos,
     * sin markdown que rompa el parseo del frontend.
     */
    private String normalizarSecciones(String respuesta, String diagnostico) {
        String texto = respuesta
                .replace("**", "")
                .replaceAll("(?i)===\s*DIAGN[ÓO]STICO\s*===", "===DIAGNOSTICO===")
                .replaceAll("(?i)===\s*TRATAMIENTO\s*===", "===TRATAMIENTO===")
                .replaceAll("(?i)===\s*CONSEJOS?\s*===", "===CONSEJOS===")
                .trim();

        if (!texto.toUpperCase().contains("===DIAGNOSTICO===")) {
            texto = "===DIAGNOSTICO===\n" + diagnostico + "\n\n" + texto;
        }
        if (!texto.toUpperCase().contains("===TRATAMIENTO===")) {
            texto = texto + "\n\n===TRATAMIENTO===\n• Seguimiento clínico según evolución del paciente.";
        }
        if (!texto.toUpperCase().contains("===CONSEJOS===")) {
            texto = texto + "\n\n===CONSEJOS===\n• Mantener hábitos de sueño regulares\n• Realizar actividad física moderada";
        }
        return texto;
    }

    /**
     * Plan de respaldo cuando Ollama no está disponible (nunca deja la nota vacía).
     */
    private String generarRespuestaManual(String diagnostico) {
        return String.format("""
            ===DIAGNOSTICO===
            %s

            ===TRATAMIENTO===
            • Terapia psicológica semanal (50 minutos)
            • Evaluación psiquiátrica de seguimiento cada 1-2 meses
            • Registro de síntomas entre sesiones

            ===CONSEJOS===
            1. Mantén un horario regular de sueño
            2. Realiza 30 minutos de ejercicio físico al día
            3. Practica respiración profunda (4-7-8) ante episodios de ansiedad
            4. Reduce el consumo de alcohol y cafeína
            5. Apóyate en tu red social de confianza
            """, diagnostico);
    }

    /**
     * Explica un diagnóstico en lenguaje sencillo para el PACIENTE.
     */
    public String explicarDiagnostico(String diagnostico, String nivelDetalle) {
        boolean simple = !"detallado".equalsIgnoreCase(nivelDetalle);
        String prompt = String.format("""
            Eres un asistente de salud mental cálido y claro, para pacientes.
            Explica el diagnóstico "%s" de forma %s y esperanzadora.
            Estructura tu respuesta así:
            1. Qué significa en palabras sencillas
            2. Síntomas más comunes
            3. Que es tratable y puede mejorar con apoyo profesional
            Máximo 150 palabras. Tono amable, sin tecnicismos %s.
            """,
                diagnostico,
                simple ? "muy sencilla, como para un adolescente" : "clara pero con algo más de detalle",
                simple ? "ni datos de medicamentos" : "");
        return llamarOllama(prompt, 300);
    }

    /**
     * Genera consejos de bienestar por categoría (para el paciente).
     */
    public String generarConsejos(String categoria) {
        String prompt = String.format("""
            Eres un experto en bienestar mental para pacientes.
            Genera 5 consejos prácticos sobre: %s
            Deben ser accionables desde hoy, con base en evidencia y con un tono alentador.
            Respóndelos como lista numerada, máximo 150 palabras.
            """, categoria);
        return llamarOllama(prompt, 300);
    }

    /**
     * Chat libre. La persona del bot depende del rol del usuario autenticado:
     *  - DOCTOR  -> asistente clínico (soporte en decisión médica)
     *  - PACIENTE -> asistente empático de bienestar (sin prescripciones)
     */
    public String chatRespuesta(String mensajeUsuario, String contexto, boolean esDoctor) {
        String prompt;
        if (esDoctor) {
            prompt = String.format("""
                Eres el asistente clínico de IA de una doctora en un consultorio psiquiátrico.
                Apoyas a la doctora con información profesional y precisa.

                Contexto del caso: %s

                Pregunta de la doctora: %s

                Responde en máximo 150 palabras, con criterio clínico (diagnóstico diferencial,
                opciones de manejo, escalas o guías aplicables). Aclara que la decisión final
                siempre es de la doctora. Si el tema exige urgencia, dilo de forma directa.
                """, contexto == null || contexto.isBlank() ? "No proporcionado" : contexto, mensajeUsuario);
        } else {
            prompt = String.format("""
                Eres el asistente de IA personal de un paciente del consultorio psiquiátrico.
                Eres cálido, claro y alentador. Hablas con lenguaje sencillo, nunca técnico.

                Contexto del paciente: %s

                Mensaje del paciente: %s

                Responde en máximo 120 palabras. Reglas:
                - NUNCA prescribas ni sugieras dosis de medicamentos.
                - Anima al paciente a comentar síntomas nuevos con su doctor.
                - Si detectas señales de crisis o riesgo, indícale con calma llamar ahora
                  a la Línea de la Vida 800 911 2000 o al 911.
                """, contexto == null || contexto.isBlank() ? "No proporcionado" : contexto, mensajeUsuario);
        }
        return llamarOllama(prompt, 300);
    }

    /**
     * Método que llama a Ollama vía API REST directa.
     */
    private String llamarOllama(String prompt, int maxTokens) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODELO);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "num_predict", maxTokens
            ));

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                String respuesta = (String) responseMap.get("response");
                System.out.println("✅ Respuesta recibida de Ollama");
                return respuesta;
            }
            System.err.println("❌ Error HTTP al llamar a Ollama: " + response.statusCode());
            return "⚠️ Error al comunicarse con el servicio de IA. Código: " + response.statusCode();

        } catch (java.net.ConnectException e) {
            System.err.println("❌ No se pudo conectar a Ollama. ¿Está ejecutándose?");
            return "⚠️ No se pudo conectar al servicio de IA. Asegúrate de que Ollama esté ejecutándose.\n" +
                   "Para iniciarlo, abre una terminal y ejecuta: ollama run llama3.2:3b";
        } catch (Exception e) {
            System.err.println("❌ Error llamando a Ollama: " + e.getMessage());
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
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
