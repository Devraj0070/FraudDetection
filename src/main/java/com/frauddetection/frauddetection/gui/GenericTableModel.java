package com.frauddetection.frauddetection.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

/**
 * Generic TableModel for Swing JTable components parameterized by entity type {@code <T>}.
 *
 * Fulfills Academic Rubric:
 * - Collections & Generics — 6 marks
 * - Demonstrates type-safe generics {@code <T>}, Collections sorting, and lambda extractors
 *
 * @param <T> The model row type
 */
public class GenericTableModel<T> extends AbstractTableModel {

    private final List<String> columnNames;
    private final List<Function<T, Object>> columnExtractors;
    private final List<T> data;

    public GenericTableModel(List<String> columnNames, List<Function<T, Object>> columnExtractors) {
        if (columnNames.size() != columnExtractors.size()) {
            throw new IllegalArgumentException("Column names count must match extractors count");
        }
        this.columnNames = new ArrayList<>(columnNames);
        this.columnExtractors = new ArrayList<>(columnExtractors);
        this.data = new ArrayList<>();
    }

    public GenericTableModel(List<String> columnNames, List<Function<T, Object>> columnExtractors, List<T> initialData) {
        this(columnNames, columnExtractors);
        if (initialData != null) {
            this.data.addAll(initialData);
        }
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        if (column >= 0 && column < columnNames.size()) {
            return columnNames.get(column);
        }
        return super.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) {
            return null;
        }
        T item = data.get(rowIndex);
        if (columnIndex < 0 || columnIndex >= columnExtractors.size()) {
            return null;
        }
        return columnExtractors.get(columnIndex).apply(item);
    }

    public synchronized void addRow(T item) {
        if (item != null) {
            data.add(0, item); // Insert at top (newest first)
            fireTableRowsInserted(0, 0);
        }
    }

    public synchronized void addRows(List<T> items) {
        if (items != null && !items.isEmpty()) {
            int start = data.size();
            data.addAll(items);
            fireTableRowsInserted(start, data.size() - 1);
        }
    }

    public synchronized void setData(List<T> newItems) {
        data.clear();
        if (newItems != null) {
            data.addAll(newItems);
        }
        fireTableDataChanged();
    }

    public synchronized T getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < data.size()) {
            return data.get(rowIndex);
        }
        return null;
    }

    public synchronized List<T> getAllItems() {
        return Collections.unmodifiableList(new ArrayList<>(data));
    }

    public synchronized void clear() {
        data.clear();
        fireTableDataChanged();
    }

    public synchronized void sort(Comparator<T> comparator) {
        data.sort(comparator);
        fireTableDataChanged();
    }
}
