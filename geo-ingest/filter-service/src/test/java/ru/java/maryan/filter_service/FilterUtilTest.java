package ru.java.maryan.filter_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.filter_service.utils.FilterUtil;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FilterUtilTest {

    private BaseStationMessage createValidMessage() {
        return new BaseStationMessage(
                "250011234567890", // imsi (starts with 250, 15 digits)
                "351234567890123", // imei (15 digits)
                "79991234567",    // msisdn (will be cleaned to 11 digits)
                "12345",           // cellId (digits)
                "LTE",              // rat (Assuming valid in RatType)
                "sys-1",
                "67890",           // lac (digits)
                1024L,                  // volumeBytes
                60L,                    // durationSec
                "ATTACH",            // eventType (Assuming valid in RelevantEventType)
                Instant.now(),     // timestamp (now)
                -70                // signalStrength (Assuming valid for 4G)
        );
    }

    private BaseStationMessage withImei(String newImei) {
        BaseStationMessage msg = createValidMessage();
        return new BaseStationMessage(msg.imsi(), newImei, msg.msisdn(), msg.cellId(), msg.rat(),
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(), msg.eventType(),
                msg.timestamp(), msg.signalStrength());
    }

    private BaseStationMessage withCellIdAndLac(String cellId, String lac) {
        BaseStationMessage msg = createValidMessage();
        return new BaseStationMessage(msg.imsi(), msg.imei(), msg.msisdn(), cellId, msg.rat(),
                msg.sourceSystem(), lac, msg.volumeBytes(), msg.durationSec(), msg.eventType(),
                msg.timestamp(), msg.signalStrength());
    }

    private BaseStationMessage withRatAndSignal(String rat, Integer signal) {
        BaseStationMessage msg = createValidMessage();
        return new BaseStationMessage(msg.imsi(), msg.imei(), msg.msisdn(), msg.cellId(), rat,
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(), msg.eventType(),
                msg.timestamp(), signal);
    }

    private BaseStationMessage withEventType(String eventType) {
        BaseStationMessage msg = createValidMessage();
        return new BaseStationMessage(msg.imsi(), msg.imei(), msg.msisdn(), msg.cellId(), msg.rat(),
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(), eventType,
                msg.timestamp(), msg.signalStrength());
    }

    @Test
    void shouldPassValidMessage() {
        BaseStationMessage msg = createValidMessage();
        assertTrue(FilterUtil.filter(msg), "A valid message did not pass the filter");
    }

    @Test
    void shouldRejectWhenImsiDoesNotStartWith250() {
        BaseStationMessage msg = createValidMessage();

        BaseStationMessage invalidImsiMsg = new BaseStationMessage(
                "251011234567890", msg.imei(), msg.msisdn(),
                msg.cellId(), msg.rat(), msg.sourceSystem(),
                msg.lac(), msg.volumeBytes(), msg.durationSec(),
                msg.eventType(), msg.timestamp(), msg.signalStrength()
        );
        assertFalse(FilterUtil.filter(invalidImsiMsg), "The non RU IMSI should be discarded");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"250123", "2501234567890123456"}) // слишком короткий и длинный
    void shouldRejectInvalidImsi(String invalidImsi) {
        BaseStationMessage msg = createValidMessage();
        BaseStationMessage badMsg = new BaseStationMessage(
                invalidImsi, msg.imei(), msg.msisdn(), msg.cellId(), msg.rat(),
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(),
                msg.eventType(), msg.timestamp(), msg.signalStrength()
        );
        assertFalse(FilterUtil.filter(badMsg));
    }

    @Test
    void shouldAcceptMsisdnWithPlusAndFormatIt() {
        BaseStationMessage msg = createValidMessage();
        assertTrue(FilterUtil.filter(msg));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "12345678901234567"})
    void shouldRejectInvalidLengthMsisdn(String invalidMsisdn) {
        BaseStationMessage msg = createValidMessage();
        BaseStationMessage badMsg = new BaseStationMessage(
                msg.imsi(), msg.imei(), invalidMsisdn, msg.cellId(), msg.rat(),
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(),
                msg.eventType(), msg.timestamp(), msg.signalStrength()
        );
        assertFalse(FilterUtil.filter(badMsg));
    }

    @Test
    void shouldRejectTooOldTimestamp() {
        BaseStationMessage msg = createValidMessage();
        BaseStationMessage badMsg = new BaseStationMessage(
                msg.imsi(), msg.imei(), msg.msisdn(), msg.cellId(), msg.rat(),
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(),
                msg.eventType(), Instant.now().minus(Duration.ofHours(25)), msg.signalStrength()
        );
        assertFalse(FilterUtil.filter(badMsg), "Messages older than 24 hours should be discarded.");
    }

    @Test
    void shouldRejectFutureTimestamp() {
        BaseStationMessage msg = createValidMessage();
        BaseStationMessage badMsg = new BaseStationMessage(
                msg.imsi(), msg.imei(), msg.msisdn(), msg.cellId(), msg.rat(),
                msg.sourceSystem(), msg.lac(), msg.volumeBytes(), msg.durationSec(),
                msg.eventType(), Instant.now().plus(Duration.ofMinutes(10)), msg.signalStrength()
        );
        assertFalse(FilterUtil.filter(badMsg), "The message from the future should be discarded");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "12345678901234",
            "123456789012345678",
            "12345678901234A"
    })
    void shouldRejectInvalidImei(String badImei) {
        BaseStationMessage badMsg = withImei(badImei);
        assertFalse(FilterUtil.filter(badMsg), "A message with an invalid IMEI: " + badImei + " must be discarded");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789012345", "1234567890123456", "12345678901234567"})
    void shouldAcceptValidImeiLengths(String goodImei) {
        BaseStationMessage goodMsg = withImei(goodImei);
        assertTrue(FilterUtil.filter(goodMsg), "The IMEI must be 15 to 17 digits long");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123A", "abc", "-123"})
    void shouldRejectInvalidCellId(String badCellId) {
        BaseStationMessage badMsg = withCellIdAndLac(badCellId, "12345");
        assertFalse(FilterUtil.filter(badMsg), "Invalid CellID: " + badCellId + " must be discarded");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123A", "abc"})
    void shouldRejectInvalidLac(String badLac) {
        BaseStationMessage badMsg = withCellIdAndLac("12345", badLac);
        assertFalse(FilterUtil.filter(badMsg), "Invalid LAC: " + badLac + " must be discarded");
    }

    @Test
    void shouldRejectUnknownRat() {
        BaseStationMessage badMsg = withRatAndSignal("UNKNOWN_RAT", -70);
        assertFalse(FilterUtil.filter(badMsg), "The unknown type of network RAT must be discarded");
    }

    @Test
    void shouldRejectNullSignalStrength() {
        BaseStationMessage badMsg = withRatAndSignal("LTE", null);
        assertFalse(FilterUtil.filter(badMsg), "Null SignalStrength must be discarded");
    }

    @Test
    void shouldRejectWeakSignal() {
        int weakSignal = -150;
        BaseStationMessage badMsg = withRatAndSignal("LTE", weakSignal);
        assertFalse(FilterUtil.filter(badMsg), "A signal that is too weak should be discarded.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN_EVENT", "event"})
    void shouldRejectUnknownEventType(String badEventType) {
        BaseStationMessage badMsg = withEventType(badEventType);
        assertFalse(FilterUtil.filter(badMsg), "An unknown EventType should be discarded.");
    }
}