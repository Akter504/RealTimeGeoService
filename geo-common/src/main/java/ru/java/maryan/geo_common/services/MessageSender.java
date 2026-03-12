package ru.java.maryan.geo_common.services;

public interface MessageSender <T> {
    void send(T message);
    void send(T message, String topic);
}

