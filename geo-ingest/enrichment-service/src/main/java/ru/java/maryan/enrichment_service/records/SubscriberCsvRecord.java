package ru.java.maryan.enrichment_service.records;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class SubscriberCsvRecord {
    @CsvBindByName(column = "imsi")
    private String imsi;

    @CsvBindByName(column = "msisdn")
    private String msisdn;
}
