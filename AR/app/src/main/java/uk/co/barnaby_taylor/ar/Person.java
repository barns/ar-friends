package uk.co.barnaby_taylor.ar;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;

/**
 * Created by barnabytaylor on 22/03/15.
 */
public class Person
{
    private int smoothing;
    private int gate;
    private float[] lastAccelerometer;
    private float[] lastCompass;
    private String accelData = "Accelerometer Data";
    private String compassData = "Compass Data";
    private float[] accelArray;
    private float[] compassArray;
    private Filter accelFilter = new Filter();
    private Filter compFilter = new Filter();
    private String name;
    private float bearingTo = 0.0f;
    private Location location;
    private float dx;
    private float dy;

    public Person(String name) {
        smoothing = 100;
        gate = 20;
        this.name = name;
    }

    public void setLastAccelerometer(float[] accel) {
        lastAccelerometer = accel;
    }

    public void setLastCompass(float[] comp) {
        lastCompass = comp;
    }

    public void filter(String sensorType, SensorEvent event, StringBuilder msg) {
        if (sensorType == "accelerometer") {
            setLastAccelerometer(event.values.clone());
            accelData = msg.toString();
            if (accelArray != null) {
                accelArray = accelFilter.lowPassArray(accelArray, event.values, smoothing, gate,
                        false);
            } else {
                accelArray = event.values;
            }
        } else if (sensorType == "compass") {
            setLastCompass(event.values.clone());
            compassData = msg.toString();
            if (compassArray != null) {
                compassArray = compFilter.lowPassArray(compassArray, event.values, smoothing,
                        gate, true);
            } else {
                compassArray = event.values;
            }
        }
    }

    public float[] getLastAccelerometer() {
        return lastAccelerometer;
    }

    public float[] getLastCompass() {
        return lastCompass;
    }

    public float[] getAccelArray() {
        return accelArray;
    }

    public float[] getCompassArray() {
        return compassArray;
    }

    public String getAccelData() {
        return accelData;
    }

    public String getCompassData() {
        return compassData;
    }

    public String getName() {
        return name;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public float getBearingTo(GPSTracker gps) {
        return gps.getLocation().bearingTo(this.location);
    }

    public void setDx(float dx) {
        this.dx = dx;
    }

    public void setDy(float dy) {
        this.dy = dy;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }

    public void setName(String name) {
        this.name = name;
    }
}