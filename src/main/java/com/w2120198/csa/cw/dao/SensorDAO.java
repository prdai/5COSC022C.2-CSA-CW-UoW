package com.w2120198.csa.cw.dao;

import com.w2120198.csa.cw.model.Sensor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorDAO {

    private static final List<Sensor> SENSORS = Collections.synchronizedList(new ArrayList<>());

    public List<Sensor> getAll() {
        synchronized (SENSORS) {
            return new ArrayList<>(SENSORS);
        }
    }

    public Sensor getById(String id) {
        if (id == null) {
            return null;
        }
        synchronized (SENSORS) {
            for (Sensor sensor : SENSORS) {
                if (id.equals(sensor.getId())) {
                    return sensor;
                }
            }
            return null;
        }
    }

    public void add(Sensor sensor) {
        synchronized (SENSORS) {
            SENSORS.add(sensor);
        }
    }

    public void update(Sensor sensor) {
        synchronized (SENSORS) {
            for (int i = 0; i < SENSORS.size(); i++) {
                if (SENSORS.get(i).getId().equals(sensor.getId())) {
                    SENSORS.set(i, sensor);
                    return;
                }
            }
        }
    }
}
