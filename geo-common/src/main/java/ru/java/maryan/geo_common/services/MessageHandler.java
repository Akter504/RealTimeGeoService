package ru.java.maryan.geo_common.services;

public interface MessageHandler <T> {
    void handle(T message);
}
