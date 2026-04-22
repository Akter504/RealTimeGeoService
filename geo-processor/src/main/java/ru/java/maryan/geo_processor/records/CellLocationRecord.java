package ru.java.maryan.geo_processor.records;

public record CellLocationRecord(String lac, String cellId) {
    public boolean matches(String lac, String cellId) {
        return this.lac.equals(lac) && this.cellId.equals(cellId);
    }
}
