package com.w2120198.csa.cw.service;

import com.w2120198.csa.cw.dao.SensorDAO;
import com.w2120198.csa.cw.dao.SensorReadingDAO;
import com.w2120198.csa.cw.exception.SensorUnavailableException;
import com.w2120198.csa.cw.model.Sensor;
import com.w2120198.csa.cw.model.SensorReading;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SensorReadingService {

    private final SensorReadingDAO readingDAO = new SensorReadingDAO();
    private final SensorDAO sensorDAO = new SensorDAO();

    public Optional<List<SensorReading>> historyFor(String sensorId) {
        if (sensorDAO.getById(sensorId) == null) {
            return Optional.empty();
        }
        return Optional.of(readingDAO.getBySensorId(sensorId));
    }

    public Optional<SensorReading> record(String sensorId, SensorReading reading) {
        Sensor parent = sensorDAO.getById(sensorId);
        if (parent == null) {
            return Optional.empty();
        }
        if (Sensor.STATUS_MAINTENANCE.equals(parent.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is under MAINTENANCE and cannot accept readings.");
        }
        reading.setId(UUID.randomUUID().toString());
        readingDAO.add(sensorId, reading);

        // keep the parent sensor's currentValue in sync with the newest reading
        parent.setCurrentValue(reading.getValue());
        sensorDAO.update(parent);
        return Optional.of(reading);
    }
}
