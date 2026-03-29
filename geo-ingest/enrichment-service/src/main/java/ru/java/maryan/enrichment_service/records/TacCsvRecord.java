package ru.java.maryan.enrichment_service.records;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class TacCsvRecord {
    @CsvBindByName(column = "tac")
    private Integer tac;

    @CsvBindByName(column = "company")
    private String company;

    @CsvBindByName(column = "name")
    private String name;
}
