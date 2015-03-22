package uk.co.barnaby_taylor.ar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

/**
 * Created by Kanoshi on 22/03/2015.
 */
public class ArBox extends Drawable {
    private Bitmap ic_message;
    private Bitmap ic_launcher3;

    String fb_id;
    String fb_name;

    TextPaint messagePaint;

    public ArBox(Context context, String fb_id, String fb_name, TextPaint messagePaint) {
        ic_message = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_message);
        ic_launcher3 = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher3);
        this.fb_id = fb_id;
        this.fb_name = fb_name;
        this.messagePaint = messagePaint;
    }

    public void draw(Canvas canvas) {
        int left = canvas.getWidth() / 2 - 150;
        int top = canvas.getHeight() / 2 - 75;
        canvas.drawBitmap(ic_message, left, top, null);
        canvas.drawBitmap(ic_launcher3, left + 50, top + 50, null);
        canvas.drawText(fb_name, left + 170, top + 105, messagePaint);
    }

    public void setName(String name) {
        fb_name = name;
    }

    public void setAlpha(int alpha) {

    }

    public void setColorFilter(ColorFilter colorFilter) {

    }

    public int getOpacity() {
        return 1;
    }
}
