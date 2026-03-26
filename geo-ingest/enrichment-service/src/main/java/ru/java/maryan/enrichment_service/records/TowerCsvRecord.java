package ru.java.maryan.enrichment_service.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class TowerCsvRecord {
    @CsvBindByName(column = "radio")
    private String rat;

    @CsvBindByName(column = "mcc")
    private Integer mcc;

    // GSM, UMTS, LTE (MNC).
    @CsvBindByName(column = "net")
    private Integer mnc;

    @CsvBindByName(column = "area")
    private Integer lac;

    @CsvBindByName(column = "cell")
    private Integer cellId;

    @CsvBindByName(column = "unit")
    private Integer unit;

    @CsvBindByName(column = "lon")
    private Double lon;

    @CsvBindByName(column = "lat")
    private Double lat;

    @CsvBindByName(column = "range")
    private Integer range;

    @CsvBindByName(column = "samples")
    private Integer samples;

    @CsvBindByName(column = "changeable")
    private Integer changeable;

    @CsvBindByName(column = "created")
    private Integer created;

    @CsvBindByName(column = "updated")
    private Integer updated;

    @CsvBindByName(column = "averageSignal")
    private Integer averageSignal;

    @JsonIgnore
    public String getUniqueKey() {
        return mcc + "-" + mnc + "-" + lac + "-" + cellId;
    }
}
