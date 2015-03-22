package uk.co.barnaby_taylor.ar;

import java.util.ArrayList;

/**
 * Created by barnabytaylor on 21/03/15.
 */
public class Filter {
    ArrayList<ArrayList<Float>> values;
    int size;

    public Filter() {
        size = 6;
        values = new ArrayList<>();

        for (int i=0; i<3; i++) {
            values.add(new ArrayList<Float>());
        }
    }

    public double lowPass(double currentValue, double newValue, int smoothing, int gate) {
        double candidate = (currentValue - newValue) / smoothing;
        if (Math.abs(candidate - newValue) > newValue / gate) {
            newValue += (currentValue - newValue) / smoothing;
            return newValue;
        }

        return currentValue;
    }

    public float[] lowPassArray(float[] currentValues, float[] newValues, int smoothing, int gate, boolean compass) {
        float[] output = new float[newValues.length];

        for (int i = 0; i < currentValues.length; i++) {
            if (compass) {
                output[i] = (filter(newValues[i], values.get(i), smoothing, gate));
            } else {
                output[i] = (filterMean(newValues[i], values.get(i)));
            }
        }

        return output;
    }

    private float filterMean(float f, ArrayList<Float> values) {

        if (values.size() >= size) {
            values.remove(0);
        }
        values.add(f);

        float sum = 0;
        for (float number : values) {
            sum += 1/number;
        }

        return values.size()/sum;
    }

    private float filter(float f, ArrayList<Float> values, int smoothing, int gate) {
        if (values.size() >= size) {
            values.remove(0);
        }
        values.add(f);

        float value;
        if (Math.abs(values.get(values.size() - 1) - values.get(0)) > gate) {
            value = values.get(values.size() - 1) + (values.get(0) -
                    values.get(values.size() - 1) / smoothing);
        } else {
            value = values.get(0);
        }

        return value;
    }
}