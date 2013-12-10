package com.jwetherell.augmented_reality.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.jwetherell.augmented_reality.ui.objects.PaintableIcon;

/**
 * This class extends Marker and draws an icon instead of a circle for it's
 * visual representation.
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class CenteredIconMarker extends CenteredMarker {

    protected Bitmap bitmap = null;

    public CenteredIconMarker(String name, double latitude, double longitude, double altitude, int color, Bitmap bitmap) {
        super(name, latitude, longitude, altitude, color);
        this.bitmap = bitmap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drawIcon(Canvas canvas) {
        if (canvas == null || bitmap == null) throw new NullPointerException();

        if (gpsSymbol == null) gpsSymbol = new PaintableIcon(bitmap, 96, 96);
        super.drawIcon(canvas);
    }
}
