package ru.java.maryan.enrichment_service.records;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class TacCsvRecord {
    @CsvBindByName(column = "tac")
    private Integer tac;

    @CsvBindByName(column = "company")
    private String company;

    @CsvBindByName(column = "name")
    private String name;
}
