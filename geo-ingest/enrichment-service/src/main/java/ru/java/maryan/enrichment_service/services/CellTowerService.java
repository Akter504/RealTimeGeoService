package ru.java.maryan.enrichment_service.services;

import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.java.maryan.enrichment_service.records.TacCsvRecord;
import ru.java.maryan.enrichment_service.records.TowerCsvRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CellTowerService {
    private static final Logger log = LoggerFactory.getLogger(CellTowerService.class);

    private final Map<String, TowerCsvRecord> towerCache = new ConcurrentHashMap<>();
    private final Map<String, TacCsvRecord> tacCache = new ConcurrentHashMap<>();

    @Value("${celltower.towers-file:RussiaCells.csv}")
    private String towersFileName;

    @Value("${celltower.tac-file:tacdb.csv}")
    private String tacFileName;

    @PostConstruct
    public void init() {
        loadTowers();
        loadTac();
    }

    private void loadTowers() {
        long start = System.currentTimeMillis();
        try {
            ClassPathResource resource = new ClassPathResource(towersFileName);
            if (!resource.exists()) {
                log.error("Towers file not found: {}", towersFileName);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {

                List<TowerCsvRecord> records = new CsvToBeanBuilder<TowerCsvRecord>(reader)
                        .withType(TowerCsvRecord.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSeparator(',')
                        .build()
                        .parse();

                for (TowerCsvRecord csvRecord : records) {
                    towerCache.put(csvRecord.getUniqueKey(), csvRecord);
                }

                log.info("Loaded {} towers in {} ms", towerCache.size(),
                        System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("Error loading towers from {}", towersFileName, e);
            throw new RuntimeException("Failed to load tower database", e);
        }
    }

    private void loadTac() {
        long start = System.currentTimeMillis();
        try {
            ClassPathResource resource = new ClassPathResource(tacFileName);
            if (!resource.exists()) {
                log.error("TAC file not found: {}", tacFileName);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {

                List<TacCsvRecord> records = new CsvToBeanBuilder<TacCsvRecord>(reader)
                        .withType(TacCsvRecord.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withSeparator(',')
                        .build()
                        .parse();

                for (TacCsvRecord csvRecord : records) {
                    tacCache.put(csvRecord.getTac().toString(), csvRecord);
                }

                log.info("Loaded {} TAC records in {} ms", tacCache.size(),
                        System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("Error loading TAC from {}", tacFileName, e);
            throw new RuntimeException("Failed to load TAC database", e);
        }
    }

    public TowerCsvRecord findTower(String key) {
        return towerCache.get(key);
    }

    public TacCsvRecord findTac(String tac) {
        return tacCache.get(tac);
    }
}