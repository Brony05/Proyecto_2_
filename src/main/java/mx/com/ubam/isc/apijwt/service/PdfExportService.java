package mx.com.ubam.isc.apijwt.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;
import mx.com.ubam.isc.apijwt.model.Cita;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private static final BaseColor COLOR_PRIMARY = new BaseColor(102, 126, 234);
    private static final BaseColor COLOR_SECONDARY = new BaseColor(118, 75, 162);
    private static final BaseColor COLOR_HEADER = new BaseColor(52, 58, 64);
    private static final BaseColor COLOR_ROW_ODD = new BaseColor(248, 249, 250);
    private static final BaseColor COLOR_ROW_EVEN = new BaseColor(255, 255, 255);
    private static final BaseColor COLOR_STATUS_AGENDADA = new BaseColor(0, 123, 255);
    private static final BaseColor COLOR_STATUS_PROGRESO = new BaseColor(255, 193, 7);
    private static final BaseColor COLOR_STATUS_COMPLETADA = new BaseColor(40, 167, 69);
    private static final BaseColor COLOR_STATUS_CANCELADA = new BaseColor(220, 53, 69);
    private static final BaseColor COLOR_STATUS_REAGENDADA = new BaseColor(23, 162, 184);
    private static final BaseColor COLOR_MONEY = new BaseColor(0, 128, 0);

    public void exportarCitas(List<Cita> citas, HttpServletResponse response) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        
        document.open();

        // Título
        Paragraph title = new Paragraph("REPORTE DE CITAS - CONSULTORIO PSIQUIÁTRICO", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRIMARY));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));
        
        // Fecha
        Paragraph fecha = new Paragraph("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
            FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_HEADER));
        fecha.setAlignment(Element.ALIGN_RIGHT);
        document.add(fecha);
        document.add(new Paragraph(" "));
        
        // Calcular estadísticas financieras
        double ingresosReales = citas.stream()
                .filter(c -> "COMPLETADA".equals(c.getEstado()))
                .mapToDouble(c -> c.getPrecio() != null ? c.getPrecio() : 0)
                .sum();
        
        double ingresosProyectados = citas.stream()
                .filter(c -> "AGENDADA".equals(c.getEstado()) || "REAGENDADA".equals(c.getEstado()))
                .mapToDouble(c -> c.getPrecio() != null ? c.getPrecio() : 0)
                .sum();
        
        double perdidas = citas.stream()
                .filter(c -> "CANCELADA".equals(c.getEstado()))
                .mapToDouble(c -> c.getPrecio() != null ? c.getPrecio() : 0)
                .sum();
        
        long totalAgendadas = citas.stream().filter(c -> "AGENDADA".equals(c.getEstado())).count();
        long totalReagendadas = citas.stream().filter(c -> "REAGENDADA".equals(c.getEstado())).count();
        long totalCompletadas = citas.stream().filter(c -> "COMPLETADA".equals(c.getEstado())).count();
        long totalCanceladas = citas.stream().filter(c -> "CANCELADA".equals(c.getEstado())).count();
        long totalEnProgreso = citas.stream().filter(c -> "EN_PROGRESO".equals(c.getEstado())).count();
        
        // Tabla de resumen financiero
        PdfPTable financialTable = new PdfPTable(4);
        financialTable.setWidthPercentage(100);
        financialTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        
        addFinancialCell(financialTable, "💰 INGRESOS REALES", "$" + String.format("%,.2f", ingresosReales), COLOR_MONEY);
        addFinancialCell(financialTable, "📊 INGRESOS PROYECTADOS", "$" + String.format("%,.2f", ingresosProyectados), COLOR_PRIMARY);
        addFinancialCell(financialTable, "❌ PÉRDIDAS", "$" + String.format("%,.2f", perdidas), COLOR_STATUS_CANCELADA);
        addFinancialCell(financialTable, "✅ TOTAL NETO", "$" + String.format("%,.2f", ingresosReales - perdidas), COLOR_STATUS_COMPLETADA);
        
        document.add(financialTable);
        document.add(new Paragraph(" "));
        
        // Tabla de resumen de citas
        PdfPTable summaryTable = new PdfPTable(6);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{16f, 17f, 17f, 17f, 17f, 16f});
        
        addSummaryCell(summaryTable, "📋 Total Citas", String.valueOf(citas.size()), COLOR_PRIMARY);
        addSummaryCell(summaryTable, "✅ Agendadas", String.valueOf(totalAgendadas), COLOR_STATUS_AGENDADA);
        addSummaryCell(summaryTable, "🔄 Reagendadas", String.valueOf(totalReagendadas), COLOR_STATUS_REAGENDADA);
        addSummaryCell(summaryTable, "▶ En Progreso", String.valueOf(totalEnProgreso), COLOR_STATUS_PROGRESO);
        addSummaryCell(summaryTable, "✔ Completadas", String.valueOf(totalCompletadas), COLOR_STATUS_COMPLETADA);
        addSummaryCell(summaryTable, "❌ Canceladas", String.valueOf(totalCanceladas), COLOR_STATUS_CANCELADA);
        
        document.add(summaryTable);
        document.add(new Paragraph(" "));
        
        // Tabla de citas
        PdfPTable table = new PdfPTable(11);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 7f, 5f, 10f, 7f, 10f, 8f, 8f, 8f, 8f, 10f});
        
        // Encabezados
        String[] headers = {"ID", "FECHA", "HORA", "SERVICIO", "PRECIO", "CLIENTE", "ACOMPAÑANTE", "RELACIÓN", "TELÉFONO", "ESTADO", "REGISTRADO POR"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE)));
            cell.setBackgroundColor(COLOR_PRIMARY);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        
        // Datos
        for (Cita cita : citas) {
            table.addCell(createCell(String.valueOf(cita.getId()), Element.ALIGN_CENTER));
            table.addCell(createCell(cita.getFecha() != null ? cita.getFecha().toString() : "N/A", Element.ALIGN_CENTER));
            table.addCell(createCell(cita.getHora() != null ? cita.getHora().toString() : "N/A", Element.ALIGN_CENTER));
            table.addCell(createCell(cita.getServicio() != null ? cita.getServicio() : "N/A", Element.ALIGN_CENTER));
            
            double precio = cita.getPrecio() != null ? cita.getPrecio() : 0;
            table.addCell(createCell("$" + String.format("%,.2f", precio), Element.ALIGN_RIGHT));
            
            String cliente = (cita.getCliente() != null && cita.getCliente().getNombre() != null) ? cita.getCliente().getNombre() : "N/A";
            table.addCell(createCell(cliente, Element.ALIGN_LEFT));
            
            String acompanante = cita.getAcompananteNombre() != null ? cita.getAcompananteNombre() : "-";
            table.addCell(createCell(acompanante, Element.ALIGN_LEFT));
            
            String relacion = cita.getAcompananteRelacion() != null ? cita.getAcompananteRelacion() : "-";
            table.addCell(createCell(relacion, Element.ALIGN_CENTER));
            
            String telefono = (cita.getCliente() != null && cita.getCliente().getTelefono() != null) ? cita.getCliente().getTelefono() : "N/A";
            table.addCell(createCell(telefono, Element.ALIGN_CENTER));
            
            String estado = cita.getEstado() != null ? cita.getEstado() : "AGENDADA";
            BaseColor estadoColor;
            if ("AGENDADA".equals(estado)) estadoColor = COLOR_STATUS_AGENDADA;
            else if ("EN_PROGRESO".equals(estado)) estadoColor = COLOR_STATUS_PROGRESO;
            else if ("COMPLETADA".equals(estado)) estadoColor = COLOR_STATUS_COMPLETADA;
            else if ("CANCELADA".equals(estado)) estadoColor = COLOR_STATUS_CANCELADA;
            else estadoColor = COLOR_STATUS_REAGENDADA;
            
            Chunk estadoChunk = new Chunk("  " + estado + "  ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE));
            estadoChunk.setBackground(estadoColor);
            Paragraph estadoPara = new Paragraph(estadoChunk);
            estadoPara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell estadoCell = new PdfPCell(estadoPara);
            estadoCell.setPadding(4);
            estadoCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(estadoCell);
            
            String usuario = (cita.getUsuario() != null && cita.getUsuario().getUsername() != null) ? cita.getUsuario().getUsername() : "Sistema";
            table.addCell(createCell(usuario, Element.ALIGN_CENTER));
        }
        
        document.add(table);
        document.close();
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_citas_" + System.currentTimeMillis() + ".pdf");
        response.getOutputStream().write(baos.toByteArray());
        response.getOutputStream().flush();
    }
    
    private void addFinancialCell(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        cell.setBackgroundColor(COLOR_ROW_ODD);
        cell.setPadding(10);
        
        Paragraph labelPara = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);
        
        Paragraph valuePara = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);
        
        table.addCell(cell);
    }
    
    private void addSummaryCell(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        cell.setBackgroundColor(COLOR_ROW_ODD);
        cell.setPadding(8);
        
        Paragraph labelPara = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);
        
        Paragraph valuePara = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);
        
        table.addCell(cell);
    }
    
    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_HEADER)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}