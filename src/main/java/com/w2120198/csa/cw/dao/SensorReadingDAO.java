package com.w2120198.csa.cw.dao;

import com.w2120198.csa.cw.model.SensorReading;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SensorReadingDAO {

    private static final Map<String, List<SensorReading>> HISTORY = new ConcurrentHashMap<>();

    public List<SensorReading> getBySensorId(String sensorId) {
        List<SensorReading> list = HISTORY.get(sensorId);
        if (list == null) {
            return new ArrayList<>();
        }
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public void add(String sensorId, SensorReading reading) {
        List<SensorReading> list = HISTORY.computeIfAbsent(
                sensorId, k -> Collections.synchronizedList(new ArrayList<>()));
        list.add(reading);
    }
}
