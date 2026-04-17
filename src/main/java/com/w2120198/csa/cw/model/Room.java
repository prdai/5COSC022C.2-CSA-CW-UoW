package com.w2120198.csa.cw.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Physical space on campus. The embedded {@code sensorIds} collection
 * mirrors the one-to-many link to {@link Sensor}; it is maintained by
 * the service as sensors are registered and deleted, not populated
 * from client payloads.
 */
public class Room implements BaseModel {

    private String id;
    private String name;
    private int capacity;
    private List<String> sensorIds = new ArrayList<>();

    public Room() {
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = (sensorIds != null) ? sensorIds : new ArrayList<>();
    }
}
