package ru.java.maryan.geo_api.controllers;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import ru.java.maryan.geo_api.dto.LocationDTO;
import ru.java.maryan.geo_api.dto.PlaceStatDTO;
import ru.java.maryan.geo_api.dto.SubscriberProfileDTO;
import ru.java.maryan.geo_api.exceptions.SubscriberNotFoundException;
import ru.java.maryan.geo_api.services.ClickHouseAnalyticsService;
import ru.java.maryan.geo_api.services.RedisAnalyticsService;

import java.util.List;

@Slf4j
@Controller
public class GeoGraphQLController {
    private final RedisAnalyticsService redisService;
    private final ClickHouseAnalyticsService clickHouseService;

    @Autowired
    public GeoGraphQLController(RedisAnalyticsService redisService,
                                ClickHouseAnalyticsService clickHouseService) {
        this.redisService = redisService;
        this.clickHouseService = clickHouseService;
    }

    @QueryMapping
    public SubscriberProfileDTO subscriberProfile(@Argument String imsi) {
        log.info("GraphQL Query: fetching profile for IMSI: {}", imsi);

        boolean exists = redisService.subscriberExists(imsi);
        if (!exists) {
            throw new SubscriberNotFoundException(imsi);
        }
        return new SubscriberProfileDTO(imsi);
    }

    @QueryMapping
    public Integer usersCountInRadius(@Argument Double lat, @Argument Double lon, @Argument Double radiusMeters) {
        log.info("GraphQL Query: counting users in radius {}m from [{}, {}]", radiusMeters, lat, lon);
        return redisService.countUsersInRadius(lat, lon, radiusMeters);
    }

    @SchemaMapping(typeName = "SubscriberProfile", field = "currentLocation")
    public LocationDTO getCurrentLocation(SubscriberProfileDTO profile) {
        log.debug("Resolving currentLocation for {}", profile.imsi());
        return redisService.getLastKnownLocation(profile.imsi());
    }

    @SchemaMapping(typeName = "SubscriberProfile", field = "topVisitedPlaces")
    public List<PlaceStatDTO> getTopVisitedPlaces(SubscriberProfileDTO profile,
                                                  @Argument Integer limit) {
        int safeLimit = limit != null ? limit : 5;
        log.debug("Resolving topVisitedPlaces for {} with limit {}", profile.imsi(), safeLimit);
        return clickHouseService.getTopPlaces(profile.imsi(), safeLimit);
    }

    @QueryMapping
    public Integer subscriberStatus(@Argument String imsi) {
        log.info("GraphQL Query: fetching status for IMSI: {}", imsi);

        boolean exists = redisService.subscriberExists(imsi);
        if (!exists) {
            throw new SubscriberNotFoundException(imsi);
        }
        return redisService.getSubscriberStatus(imsi);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleSubscriberNotFound(SubscriberNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .build();
    }

}
