package uk.co.barnaby_taylor.ar;

/**
 * Created by barnabytaylor on 21/03/15.
 */
public class Filter
{
    public double lowPass(double currentValue, double newValue, int smoothing, int gate)
    {
        double candidate = (currentValue - newValue) / smoothing;
        if (Math.abs(candidate - newValue) > newValue / gate) {
            newValue = currentValue + (currentValue - newValue) / smoothing;
            return newValue;
        }

        return currentValue;
    }

    public float[] lowPassArray(float[] currentValues, float[] newValues, int smoothing, int gate)
    {
        for (int i = 0; i < currentValues.length; i++)
        {
            double candidate = (currentValues[i] - newValues[i]) / smoothing;
            if (Math.abs(candidate - newValues[i]) > newValues[i] / gate) {
                newValues[i] = currentValues[i] + (currentValues[i] - newValues[i]) / smoothing;
            } else {
                newValues[i] = currentValues[i];
            }
        }

        return newValues;
    }
}
