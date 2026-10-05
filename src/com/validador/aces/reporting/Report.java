package com.validador.aces.reporting;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa un reporte generado, compuesto por un título, la fecha de
 * generación y una secuencia ordenada de {@link ReportSection}.
 *
 * <p>Es el tipo de retorno agnóstico de formato de {@link ReportGenerator},
 * usado por los generadores concretos (Excel, PDF, HTML, texto plano) como
 * entrada para producir la salida específica de cada medio.</p>
 */
public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String title;
    private final LocalDateTime generatedDate;
    private final List<ReportSection> sections;

    /**
     * Crea un reporte vacío con el título indicado; la fecha de generación
     * se establece automáticamente al momento de la construcción.
     *
     * @param title título del reporte (ej. "Reporte de Auditoría ACES")
     * @throws IllegalArgumentException si {@code title} es null o vacío
     */
    public Report(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título del reporte no puede ser null ni vacío");
        }
        this.title = title;
        this.generatedDate = LocalDateTime.now();
        this.sections = new ArrayList<>();
    }

    /**
     * Agrega una sección al reporte, al final de la secuencia actual.
     *
     * @param section sección a agregar
     * @throws IllegalArgumentException si {@code section} es null
     */
    public void addSection(ReportSection section) {
        if (section == null) {
            throw new IllegalArgumentException("La sección no puede ser null");
        }
        this.sections.add(section);
    }

    /** @return título del reporte. */
    public String getTitle() {
        return title;
    }

    /** @return fecha y hora en que se generó el reporte. */
    public LocalDateTime getGeneratedDate() {
        return generatedDate;
    }

    /** @return lista no modificable de secciones, en el orden en que fueron agregadas. */
    public List<ReportSection> getSections() {
        return Collections.unmodifiableList(sections);
    }

    /**
     * Busca la primera sección cuyo título coincide exactamente con el indicado.
     *
     * @param sectionTitle título a buscar
     * @return la sección encontrada, o null si ninguna coincide
     */
    public ReportSection findSectionByTitle(String sectionTitle) {
        if (sectionTitle == null) {
            return null;
        }
        for (ReportSection section : sections) {
            if (section.getTitle().equals(sectionTitle)) {
                return section;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Report{title='" + title + "', generatedDate=" + generatedDate + ", sectionCount=" + sections.size() + '}';
    }
}