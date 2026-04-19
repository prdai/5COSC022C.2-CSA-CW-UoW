package com.w2120198.csa.cw.dao;

import com.w2120198.csa.cw.model.SensorReading;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorReadingDAO {

    private static final Map<String, List<SensorReading>> HISTORY = new HashMap<>();

    public List<SensorReading> getBySensorId(String sensorId) {
        synchronized (HISTORY) {
            List<SensorReading> list = HISTORY.get(sensorId);
            if (list == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(list);
        }
    }

    public void add(String sensorId, SensorReading reading) {
        synchronized (HISTORY) {
            // create the list on the first reading for this sensor, then append
            List<SensorReading> list = HISTORY.get(sensorId);
            if (list == null) {
                list = new ArrayList<>();
                HISTORY.put(sensorId, list);
            }
            list.add(reading);
        }
    }
}
