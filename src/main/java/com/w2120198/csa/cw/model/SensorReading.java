package com.w2120198.csa.cw.model;

/**
 * A single measurement recorded by a sensor. Readings are addressed
 * via the sub-resource path {@code /sensors/{sensorId}/readings}; the
 * parent sensor context is therefore implicit and no {@code sensorId}
 * field is kept on the reading itself.
 */
public class SensorReading implements BaseModel {

    private String id;
    private long timestamp;
    private double value;

    public SensorReading() {
    }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
