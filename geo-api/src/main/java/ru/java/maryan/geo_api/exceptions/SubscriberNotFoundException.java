package ru.java.maryan.geo_api.exceptions;

public class SubscriberNotFoundException extends RuntimeException {
    public SubscriberNotFoundException(String imsi) {
        super("Subscriber with IMSI " + imsi + " not found in the system.");
    }
}
