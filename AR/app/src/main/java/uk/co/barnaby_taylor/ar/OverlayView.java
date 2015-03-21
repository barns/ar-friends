package uk.co.barnaby_taylor.ar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;

public class OverlayView extends View implements SensorEventListener,
        LocationListener {

    public static final String DEBUG_TAG = "OverlayView Log";

    private final Context context;
    private Handler handler;

    // Mount Washington, NH: 44.27179, -71.3039, 6288 ft (highest peak
    private final static Location mountWashington = new Location("manual");
    static {
        mountWashington.setLatitude(44.27179d);
        mountWashington.setLongitude(-71.3039d);
        //mountWashington.setLatitude(43.998d);
        //mountWashington.setLongitude(-71.2d);
        mountWashington.setAltitude(1916.5d);
    }

    private final static Location teamDesk = new Location("manual");
    static {
        teamDesk.setLatitude(51.31345);
        teamDesk.setLongitude(0.08219);
    }

    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    String gyroData = "Gyro Data";

    private LocationManager locationManager = null;
    private SensorManager sensors = null;

    GPSTracker gps;
    private Location lastLocation;
    private float[] lastAccelerometer;
    private float[] lastCompass;

    private float verticalFOV;
    private float horizontalFOV;

    private boolean isAccelAvailable;
    private boolean isCompassAvailable;
    private boolean isGyroAvailable;
    private Sensor accelSensor;
    private Sensor compassSensor;
    private Sensor gyroSensor;

    private TextPaint contentPaint;
    private TextPaint messagePaint;

    private Paint targetPaint;

    public OverlayView(Context context) {
        super(context);
        this.context = context;
        this.handler = new Handler();
        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        sensors = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        startSensors();
        startGPS();

        // get some camera parameters
        Camera camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        verticalFOV = params.getVerticalViewAngle();
        horizontalFOV = params.getHorizontalViewAngle();
        camera.release();

        // paint for text
        contentPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setTextAlign(Align.LEFT);
        contentPaint.setTextSize(20);
        contentPaint.setColor(Color.RED);

        messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        messagePaint.setTextAlign(Align.LEFT);
        messagePaint.setTextSize(50);
        messagePaint.setColor(Color.BLACK);

        // paint for target

        targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setColor(Color.GREEN);

    }

    private void startSensors() {
        isAccelAvailable = sensors.registerListener(this, accelSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isCompassAvailable = sensors.registerListener(this, compassSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isGyroAvailable = sensors.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void startGPS() {
        Criteria criteria = new Criteria();
        // criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // while we want fine accuracy, it's unlikely to work indoors where we
        // do our testing. :)
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

        String best = locationManager.getBestProvider(criteria, true);

        Log.v(DEBUG_TAG, "Best provider: " + best);

        locationManager.requestLocationUpdates(best, 50, 0, this);

        gps = new GPSTracker(this.context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(DEBUG_TAG, "onDraw");
        super.onDraw(canvas);
        Resources res = getResources();
        // Draw something fixed (for now) over the camera view

        float curBearingToMW = 0.0f;

        StringBuilder text = new StringBuilder(accelData).append("\n");
        text.append(compassData).append("\n");
        text.append(gyroData).append("\n");

        if (gps == null) {
            text.append(
                    String.format("GPS NOT INITIALISED\n"));
        } else if (gps.canGetLocation()) {
            //lastLocation = gps.getLocation();
            //}
            //if (lastLocation != null) {
            String.format("GPS = (%.10f, %.10f) @ (%.5f meters up)",
                    gps.getLatitude(),//lastLocation.getLatitude(),
                    gps.getLongitude(),//lastLocation.getLongitude(),
                    0.0);//lastLocation.getAltitude())).append("\n");

            //curBearingToMW = lastLocation.bearingTo(teamDesk);
            curBearingToMW = gps.getLocation().bearingTo(teamDesk);

            text.append(String.format("Bearing to MW: %.3f", curBearingToMW))
                    .append("\n");
        } else text.append(
                String.format("NO GPS SIGNAL\n"));


        // compute rotation matrix
        float rotation[] = new float[9];
        float identity[] = new float[9];
        if (lastAccelerometer != null && lastCompass != null) {
            boolean gotRotation = SensorManager.getRotationMatrix(rotation,
                    identity, lastAccelerometer, lastCompass);
            if (gotRotation) {
                float cameraRotation[] = new float[9];
                // remap such that the camera is pointing straight down the Y
                // axis
                SensorManager.remapCoordinateSystem(rotation,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        cameraRotation);

                // orientation vector
                float orientation[] = new float[3];
                SensorManager.getOrientation(cameraRotation, orientation);

                text.append(
                        String.format("Orientation (%.3f, %.3f, %.3f)",
                                Math.toDegrees(orientation[0]), Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2])))
                        .append("\n");

                // draw horizon line (a nice sanity check piece) and the target (if it's on the screen)
                canvas.save();

                // use roll for screen rotation
                canvas.rotate((float)(0.0f- Math.toDegrees(orientation[2])));

                // Translate, but normalize for the FOV of the camera -- basically, pixels per degree, times degrees == pixels
                float dx = (float) ( (canvas.getWidth()/ horizontalFOV) * (Math.toDegrees(orientation[0])-curBearingToMW));
                float dy = (float) ( (canvas.getHeight()/ verticalFOV) * Math.toDegrees(orientation[1])) ;

                // wait to translate the dx so the horizon doesn't get pushed off
                canvas.translate(0.0f, 0.0f-dy);


                // make our line big enough to draw regardless of rotation and translation
                canvas.drawLine(0f - canvas.getHeight(), canvas.getHeight()/2, canvas.getWidth()+canvas.getHeight(), canvas.getHeight()/2, targetPaint);

                // now translate the dx
                canvas.translate(0.0f-dx, 0.0f);
                // draw our point -- we've rotated and translated this to the right spot already

                int boxMidX = canvas.getWidth()/2 - 300;
                int boxMidY = canvas.getHeight()/2 - 300;
                canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_message), boxMidX, boxMidY, null);
                canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher3), boxMidX + 50, boxMidY + 50, null);
                canvas.drawText("Liam Higgins", boxMidX + 170, boxMidY + 105, messagePaint);
                canvas.drawCircle(canvas.getWidth()/2, canvas.getHeight()/2, 8.0f, targetPaint);
                canvas.restore();


            }
        }

        canvas.save();
        canvas.translate(15.0f, 15.0f);
        StaticLayout textBox = new StaticLayout(text.toString(), contentPaint,
                480, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        textBox.draw(canvas);
        canvas.restore();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        Log.d(DEBUG_TAG, "onAccuracyChanged");

    }

    public void onSensorChanged(SensorEvent event) {
        // Log.d(DEBUG_TAG, "onSensorChanged");

        StringBuilder msg = new StringBuilder(event.sensor.getName())
                .append(" ");
        for (float value : event.values) {
            msg.append("[").append(String.format("%.3f", value)).append("]");
        }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                lastAccelerometer = event.values.clone();
                accelData = msg.toString();
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroData = msg.toString();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                lastCompass = event.values.clone();
                compassData = msg.toString();
                break;
        }

        this.invalidate();
    }

    public void onLocationChanged(Location location) {
        // store it off for use when we need it
        lastLocation = location;
        Log.d(DEBUG_TAG, "onLocationChanged");
    }

    public void onProviderDisabled(String provider) {
        // ...
    }

    public void onProviderEnabled(String provider) {
        // ...
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ...
    }

    // this is not an override
    public void onPause() {
        locationManager.removeUpdates(this);
        sensors.unregisterListener(this);
    }

    // this is not an override
    public void onResume() {
        startSensors();
        startGPS();
    }
}