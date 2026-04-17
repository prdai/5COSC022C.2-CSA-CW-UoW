package com.w2120198.csa.cw.dao;

import com.w2120198.csa.cw.model.Room;
import com.w2120198.csa.cw.model.Sensor;
import com.w2120198.csa.cw.model.SensorReading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store. The three collections act as the whole
 * persistence layer; a process restart wipes them. The static seed
 * data gives the discovery flow something to show immediately.
 */
public final class MockDatabase {

    public static final List<Room> ROOMS = new ArrayList<>();
    public static final List<Sensor> SENSORS = new ArrayList<>();
    public static final Map<String, List<SensorReading>> READINGS_BY_SENSOR = new ConcurrentHashMap<>();

    static {
        Room library = new Room("LIB-301", "Library Quiet Study", 40);
        Room lectureHall = new Room("LEC-101", "Lecture Hall A", 120);
        library.setSensorIds(new ArrayList<>(Arrays.asList("TEMP-001", "CO2-001")));
        lectureHall.setSensorIds(new ArrayList<>(Arrays.asList("OCC-001")));
        ROOMS.add(library);
        ROOMS.add(lectureHall);

        SENSORS.add(new Sensor("TEMP-001", "Temperature", Sensor.STATUS_ACTIVE, 21.3d, "LIB-301"));
        SENSORS.add(new Sensor("CO2-001", "CO2", Sensor.STATUS_ACTIVE, 412.0d, "LIB-301"));
        SENSORS.add(new Sensor("OCC-001", "Occupancy", Sensor.STATUS_MAINTENANCE, 0.0d, "LEC-101"));
    }

    private MockDatabase() {
    }
}
