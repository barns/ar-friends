package uk.co.barnaby_taylor.ar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;

public class OverlayView extends View implements SensorEventListener,
        LocationListener {

    public static final String DEBUG_TAG = "OverlayView Log";

    private final Context context;
    private Handler handler;
    private Location lastLocation;

    private final static Location russia = new Location("manual");
    static {
        russia.setLatitude(69.522991);
        russia.setLongitude(94.152833);
    }

    private final static Location canada = new Location("manual");
    static {
        canada.setLatitude(48.064872);
        canada.setLongitude(-76.485341);
    }

    AccelFilter accelFilter = new AccelFilter(10);
    Filter compFilter = new Filter();

    private final static Location ghana = new Location("manual");
    static {
        ghana.setLatitude(9.386210);
        ghana.setLongitude(1.426747);
    }

    private SensorManager sensors = null;

    GPSTracker gps;

    private float verticalFOV;
    private float horizontalFOV;

    private boolean isAccelAvailable;
    private boolean isCompassAvailable;
    private Sensor accelSensor;
    private Sensor compassSensor;

    private TextPaint contentPaint;
    private TextPaint messagePaint;
    private int numberOfPeople = 3;

    private Canvas canvas;

    private Paint targetPaint;

    //TODO create instance of person for each retrieved from server.

    private Person[] persons = new Person[numberOfPeople];

    public OverlayView(Context context) {
        super(context);
        this.context = context;
        this.handler = new Handler();

        persons[0] = new Person("Barnaby");
        persons[0].setLocation(russia);
        persons[1] = new Person("Liam");
        persons[1].setLocation(canada);
        persons[2] = new Person("Daniel");
        persons[2].setLocation(ghana);

        /*for (int i = 0; i < numberOfPeople; i++) {
            persons[i] = new Person(personName);
        }*/

        sensors = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

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
                SensorManager.SENSOR_DELAY_FASTEST);
        isCompassAvailable = sensors.registerListener(this, compassSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void startGPS() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

        gps = new GPSTracker(this.context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(DEBUG_TAG, "onDraw");
        super.onDraw(canvas);
        Resources res = getResources();
        // Draw something fixed (for now) over the camera view

        this.canvas = canvas;

        //Uncomment below for debug text
        /*StringBuilder text = new StringBuilder(persons[0].getAccelData()).append("\n");
        text.append(persons[0].getCompassData()).append("\n");

        if (gps == null) {
            text.append(
                    String.format("GPS NOT INITIALISED\n"));
        } else if (gps.canGetLocation()) {
            //lastLocation = gps.getLocation();
            //}
            //if (lastLocation != null) {
            String.format("GPS = (%.10f, %.10f)",
                    gps.getLatitude(),
                    gps.getLongitude());

            text.append(String.format("Bearing to MW: %.3f", persons[0].getBearingTo(gps)))
                    .append("\n");
        } else text.append(
                String.format("NO GPS SIGNAL\n"));*/


        for (Person person : persons) {
            // compute rotation matrix
            float rotation[] = new float[9];
            float identity[] = new float[9];
            if (person.getLastAccelerometer() != null && person.getLastCompass() != null) {
                boolean gotRotation = SensorManager.getRotationMatrix(rotation,
                        identity, person.getAccelArray(), person.getCompassArray());
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

                    //Uncomment below for debug text
                    /*text.append(
                            String.format("Orientation (%.3f, %.3f, %.3f)",
                                    Math.toDegrees(orientation[0]), Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2])))
                            .append("\n");*/

                    canvas.save();

                    // use roll for screen rotation
                    canvas.rotate((float) (0.0f - Math.toDegrees(orientation[2])));

                    // Translate, but normalize for the FOV of the camera -- basically, pixels per degree, times degrees == pixels
                    person.setDx((float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(orientation[0]) - person.getBearingTo(gps))));
                    person.setDy((float) ((canvas.getHeight() / verticalFOV) * Math.toDegrees(orientation[1])));

                    // wait to translate the dx so the horizon doesn't get pushed off
                    canvas.translate(0.0f, 0.0f - person.getDy());

                    // now translate the dx
                    canvas.translate(0.0f - person.getDx(), 0.0f);

                    int boxMidX = canvas.getWidth() / 2 - 300;
                    int boxMidY = canvas.getHeight() / 2 - 150;
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_message), boxMidX, boxMidY, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher3), boxMidX + 50, boxMidY + 50, null);
                    canvas.drawText(person.getName(), boxMidX + 170, boxMidY + 105, messagePaint);
                    canvas.restore();
                }
            }
        }

        canvas.save();
        canvas.translate(15.0f, 15.0f);
        //Uncomment below for debug text
        /*StaticLayout textBox = new StaticLayout(text.toString(), contentPaint,
                480, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        textBox.draw(canvas);*/
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

        for (Person person : persons) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    person.filter("accelerometer", event, msg);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    person.filter("compass", event, msg);
                    break;
            }
        }

        this.invalidate();
    }

    public boolean onTouchEvent(MotionEvent event) {
        float xCoord = event.getX();
        float yCoord = event.getY();
        //TODO make a friend

        return true;
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
        sensors.unregisterListener(this);
    }

    // this is not an override
    public void onResume() {
        startSensors();
        startGPS();
    }
}