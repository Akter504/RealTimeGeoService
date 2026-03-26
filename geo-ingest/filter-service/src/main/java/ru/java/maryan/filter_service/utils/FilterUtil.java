package ru.java.maryan.filter_service.utils;

import lombok.extern.slf4j.Slf4j;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.filter_service.enums.RatType;
import ru.java.maryan.filter_service.enums.RelevantEventType;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
public final class FilterUtil {

    private FilterUtil() {}

    public static boolean filter(BaseStationMessage msg) {
        if (!checkEventType(msg.eventType())) return false;
        if (!checkImsiAndMsisdn(msg.imsi(), msg.msisdn())) return false;
        if (!checkImei(msg.imei())) return false;
        if (!checkCellIdWithLac(msg.cellId(), msg.lac())) return false;
        if (!checkRat(msg.rat(), msg.signalStrength())) return false;
        return checkTimestamp(msg.timestamp());
    }

    private static boolean checkTimestamp(Instant timestamp) {
        if (timestamp == null) {
            log.warn("Timestamp is null");
            return false;
        }

        Instant now = Instant.now();
        if (timestamp.isBefore(now.minus(Duration.ofHours(1)))) {
            log.warn("Timestamp too old: {}", timestamp);
            return false;
        }
        if (timestamp.isAfter(now)) {
            log.warn("Timestamp from future: {}", timestamp);
            return false;
        }
        return true;
    }

    private static boolean checkRat(String ratStr, Integer signalStrength) {
        Optional<RatType> ratOpt = RatType.fromString(ratStr);

        if (ratOpt.isEmpty()) {
            log.warn("Unsupported or missing RAT: {}", ratStr);
            return false;
        }

        RatType rat = ratOpt.get();

        if (signalStrength != null) {
            int minSignal = rat.getMinSignalStrength();
            if (signalStrength < minSignal) {
                log.warn("Signal too weak for {}: {} < {}", rat, signalStrength, minSignal);
                return false;
            }
        } else {
            log.warn("Signal strength is null");
            return false;
        }

        return true;
    }
    
    private static final String DIGITS_REGEX = "\\d+";
    private static boolean checkCellIdWithLac(String cellId, String lac) {
        if (cellId == null || cellId.isBlank()) {
            log.warn("CellId is empty or blank");
            return false;
        }

        if (!cellId.matches(DIGITS_REGEX)) {
            log.warn("CellId contains non-digits: {}", cellId);
            return false;
        }

        if (lac == null || lac.isBlank()) {
            log.warn("Lac is empty or blank");
            return false;
        }

        if (!lac.matches(DIGITS_REGEX)) {
            log.warn("Lac contains non-digits: {}", lac);
            return false;
        }

        return true;
    }

    private static final String IMSI_REGEX = "\\d{15}";
    private static final String MSISDN_REGEX = "\\D";
    private static final String MCC_FOR_RUSSIA = "250";
    private static final int MIN_MSISDN_LENGTH = 10;
    private static final int MAX_MSISDN_LENGTH = 15;
    private static boolean checkImsiAndMsisdn(String imsi, String msisdn) {
        return isValidImsi(imsi) || isValidMsisdn(msisdn);
    }

    private static boolean isValidImsi(String imsi) {
        if (imsi == null || imsi.isBlank()) {
            log.warn("IMSI is empty or blank");
            return false;
        }

        if (!imsi.matches(IMSI_REGEX)) {
            log.warn("Wrong format for IMSI (need 15 digits): {}", imsi);
            return false;
        }

        // TODO: При масштабировании убрать
        if (!imsi.startsWith(MCC_FOR_RUSSIA)) {
            log.warn("Geo-service does not support non-Russian subscribers: {}", imsi);
            return false;
        }

        return true;
    }

    private static boolean isValidMsisdn(String msisdn) {
        if (msisdn == null || msisdn.isBlank()) {
            log.warn("MSISDN is empty or blank");
            return false;
        }

        String cleanMsisdn = msisdn.replaceAll(MSISDN_REGEX, "");
        if (cleanMsisdn.length() < MIN_MSISDN_LENGTH || cleanMsisdn.length() > MAX_MSISDN_LENGTH) {
            log.warn("MSISDN wrong length (should be {}-{} digits): {}",
                    MIN_MSISDN_LENGTH, MAX_MSISDN_LENGTH, msisdn);
            return false;
        }

        return true;
    }

    private static final String IMEI_REGEX = "\\d{15,17}";
    private static boolean checkImei(String imei) {
        if (imei == null || imei.isBlank()) {
            log.warn("Imei is empty or is blank: {}", imei);
            return false;
        }
        if (!imei.matches(IMEI_REGEX)) {
            log.warn("Wrong format for Imei (need 15-17 digits): {}", imei);
            return false;
        }
        return true;
    }

    private static boolean checkEventType(String eventTypeStr) {
        Optional<RelevantEventType> eventOpt = RelevantEventType.fromString(eventTypeStr);

        if (eventOpt.isEmpty()) {
            log.warn("Unknown event type: {}", eventTypeStr);
            return false;
        }
        return true;
    }

}
