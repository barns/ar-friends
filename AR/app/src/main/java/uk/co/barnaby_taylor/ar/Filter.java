package uk.co.barnaby_taylor.ar;

/**
 * Created by barnabytaylor on 21/03/15.
 */
public class Filter
{
    public float[] lowPass(float[] previousData, float[] newData, int smoothing)
    {
        float[] returnData = new float[previousData.length];
        for (int i = 0; i < previousData.length; i++)
        {
            newData[i] += (previousData[i] - newData[i]) / smoothing;
        }
        return newData;
    }
}
