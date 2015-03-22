package uk.co.barnaby_taylor.ar;

import java.util.ArrayList;

/**
 * Created by barnabytaylor on 21/03/15.
 */
public class Filter
{
    ArrayList<ArrayList<Float>> values;
    int size;

    public Filter() {
        size = 8;
        values = new ArrayList<>();
        for (int i=0; i<3; i++) {
            values.add(new ArrayList<Float>());
        }
    }

    public double lowPass(double currentValue, double newValue, int smoothing, int gate)
    {
        double candidate = (currentValue - newValue) / smoothing;
        if (Math.abs(candidate - newValue) > newValue / gate) {
            newValue += (currentValue - newValue) / smoothing;
            return newValue;
        }

        return currentValue;
    }

    public float[] lowPassArray(float[] currentValues, float[] newValues, int smoothing, int gate)
    {
        float[] output = new float[newValues.length];
        for (int i = 0; i < currentValues.length; i++)
        {
            output[i] = filter(newValues[i],values.get(i),smoothing);
        }

        return output;
    }

    private float filter(float f, ArrayList<Float> values,int smoothing) {
        if (values.size() >= size) {
            values.remove(0);
        }
        values.add(f);

        float sum = 0;
        for (float number : values) {
            sum += number;
        }

        return sum/values.size();
    }
}
