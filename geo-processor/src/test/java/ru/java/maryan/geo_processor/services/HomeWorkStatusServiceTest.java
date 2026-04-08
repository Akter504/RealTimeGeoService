package ru.java.maryan.geo_processor.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeWorkStatusServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Resource homeSqlResource;

    @Mock
    private Resource workSqlResource;

    @Test
    void testServiceInitialization() throws Exception {
        // Given
        when(homeSqlResource.getContentAsString(StandardCharsets.UTF_8)).thenReturn("SELECT * FROM home");
        when(workSqlResource.getContentAsString(StandardCharsets.UTF_8)).thenReturn("SELECT * FROM work");

        // When
        HomeWorkStatusService service = new HomeWorkStatusService(jdbcTemplate, homeSqlResource, workSqlResource);

        // Then
        assertEquals(service.getClass().getSimpleName(), "HomeWorkStatusService");
    }
}


