package com.validador.aces.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sección tabular genérica de un {@link Report}, independiente del formato
 * de salida final. Cada sección tiene un título, encabezados de columna
 * opcionales y una lista de filas de celdas en texto.
 *
 * <p>Esta representación agnóstica de formato permite que distintos
 * {@link ReportGenerator} concretos (Excel, PDF, HTML, texto plano) rendericen
 * la misma sección de la forma apropiada a su medio de salida, sin duplicar
 * la lógica de qué datos componen cada sección.</p>
 */
public class ReportSection {

    private final String title;
    private final List<String> headers;
    private final List<List<String>> rows;

    /**
     * Crea una sección con encabezados y sin filas iniciales.
     *
     * @param title   título descriptivo de la sección (ej. "Resumen Ejecutivo")
     * @param headers encabezados de columna; puede ser una lista vacía si la
     *                sección no requiere encabezados (ej. texto narrativo)
     * @throws IllegalArgumentException si {@code title} es null/vacío, o si {@code headers} es null
     */
    public ReportSection(String title, List<String> headers) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título de la sección no puede ser null ni vacío");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Los encabezados no pueden ser null (use lista vacía si no aplica)");
        }
        this.title = title;
        this.headers = Collections.unmodifiableList(new ArrayList<>(headers));
        this.rows = new ArrayList<>();
    }

    /**
     * Agrega una fila de datos a la sección.
     *
     * @param row valores de la fila, en el mismo orden que {@link #getHeaders()}
     * @throws IllegalArgumentException si {@code row} es null, o si la sección
     *                                   tiene encabezados definidos y el tamaño de
     *                                   {@code row} no coincide con la cantidad de encabezados
     */
    public void addRow(List<String> row) {
        if (row == null) {
            throw new IllegalArgumentException("La fila no puede ser null");
        }
        if (!headers.isEmpty() && row.size() != headers.size()) {
            throw new IllegalArgumentException(
                "La fila tiene " + row.size() + " valores, pero la sección define " + headers.size() + " encabezados");
        }
        this.rows.add(new ArrayList<>(row));
    }

    /** @return título descriptivo de la sección. */
    public String getTitle() {
        return title;
    }

    /** @return lista no modificable de encabezados de columna (puede estar vacía). */
    public List<String> getHeaders() {
        return headers;
    }

    /** @return lista no modificable de filas; cada fila es, a su vez, una lista no modificable de celdas. */
    public List<List<String>> getRows() {
        List<List<String>> unmodifiableRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            unmodifiableRows.add(Collections.unmodifiableList(row));
        }
        return Collections.unmodifiableList(unmodifiableRows);
    }

    /** @return cantidad de filas en la sección. */
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public String toString() {
        return "ReportSection{title='" + title + "', headers=" + headers + ", rowCount=" + rows.size() + '}';
    }
}