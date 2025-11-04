package com.example.eventmaster.model;

import com.google.firebase.Timestamp;

import java.util.Date;

/**
 * Represents an event in the Event Lottery System.
 * Contains event details like name, description, dates, organizer info, and capacity limits.
 */
public class Event {
    private String eventId;
    private String name;
    private String description;
    private String location;
    private Timestamp eventDate;
    private Timestamp registrationStartDate;
    private Timestamp registrationEndDate;
    private String posterUrl;
    private String organizerId;
    private String organizerName;
    private int capacity;
    private Integer waitingListLimit; // null means unlimited
    private boolean geolocationRequired;
    private double price;

    // No-arg constructor required for Firestore
    public Event() {
    }

    /**
     * Creates a new Event with the specified details.
     *
     * @param eventId               Unique identifier for the event
     * @param name                  Name of the event
     * @param description           Description of the event
     * @param location              Physical location of the event
     * @param eventDate             Date and time when the event occurs
     * @param registrationStartDate When registration opens
     * @param registrationEndDate   When registration closes
     * @param organizerId           ID of the organizer
     * @param organizerName         Name of the organizer
     * @param capacity              Maximum number of attendees
     * @param price                 Cost to attend the event
     */
    public Event(String eventId, String name, String description, String location,
                 Date eventDate, Date registrationStartDate, Date registrationEndDate,
                 String organizerId, String organizerName, int capacity, double price) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate != null ? new Timestamp(eventDate) : null;
        this.registrationStartDate = registrationStartDate != null ? new Timestamp(registrationStartDate) : null;
        this.registrationEndDate = registrationEndDate != null ? new Timestamp(registrationEndDate) : null;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.capacity = capacity;
        this.price = price;
        this.geolocationRequired = false;
    }

    // Getters and Setters

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getEventDate() {
        return eventDate != null ? eventDate.toDate() : null;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate != null ? new Timestamp(eventDate) : null;
    }

    public Timestamp getEventDateTimestamp() {
        return eventDate;
    }

    public void setEventDateTimestamp(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public Date getRegistrationStartDate() {
        return registrationStartDate != null ? registrationStartDate.toDate() : null;
    }

    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate != null ? new Timestamp(registrationStartDate) : null;
    }

    public Timestamp getRegistrationStartDateTimestamp() {
        return registrationStartDate;
    }

    public void setRegistrationStartDateTimestamp(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public Date getRegistrationEndDate() {
        return registrationEndDate != null ? registrationEndDate.toDate() : null;
    }

    public void setRegistrationEndDate(Date registrationEndDate) {
        this.registrationEndDate = registrationEndDate != null ? new Timestamp(registrationEndDate) : null;
    }

    public Timestamp getRegistrationEndDateTimestamp() {
        return registrationEndDate;
    }

    public void setRegistrationEndDateTimestamp(Timestamp registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Integer getWaitingListLimit() {
        return waitingListLimit;
    }

    public void setWaitingListLimit(Integer waitingListLimit) {
        this.waitingListLimit = waitingListLimit;
    }

    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}