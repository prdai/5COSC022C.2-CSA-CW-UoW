package com.w2120198.csa.cw.model;

/**
 * A hardware sensor deployed in a {@link Room}. The {@code status}
 * field takes one of the string values {@code ACTIVE},
 * {@code MAINTENANCE}, or {@code OFFLINE}; the string form is
 * preserved to match the coursework specification POJO.
 */
public class Sensor implements BaseModel {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String STATUS_OFFLINE = "OFFLINE";

    private String id;
    private String type;
    private String status;
    private double currentValue;
    private String roomId;

    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
