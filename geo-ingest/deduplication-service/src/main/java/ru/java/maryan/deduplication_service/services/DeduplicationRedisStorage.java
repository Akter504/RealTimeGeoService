package ru.java.maryan.deduplication_service.services;

public interface DeduplicationRedisStorage <T> {
    boolean save(T message);
}
