package ru.java.maryan.filter_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.java.maryan.filter_service.services.FilterService;
import ru.java.maryan.geo_common.dto.geo_ingest.BaseStationMessage;
import ru.java.maryan.geo_common.services.MessageSender;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilterServiceTest {

    @Mock
    private MessageSender<BaseStationMessage> sender;

    @InjectMocks
    private FilterService filterService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filterService, "outputTopic", "topic-out");
    }

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

    @Test
    void shouldSendMessageWhenFilterPasses() {
        BaseStationMessage msg = createValidMessage();

        filterService.handle(msg);
        verify(sender, times(1)).send(msg, "topic-out");
    }

    @Test
    void shouldNotSendMessageWhenFilterFails() {
        BaseStationMessage invalidMsg = new BaseStationMessage(
                "", "351234567890123", "79991234567",
                "123", "4G", "sys", "456", 10L, 10L, "CALL", Instant.now(), -50
        );

        filterService.handle(invalidMsg);
        verify(sender, never()).send(any(), any());
    }
}