package com.jwetherell.augmented_reality.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import com.jwetherell.augmented_reality.activity.AugmentedReality;
import com.jwetherell.augmented_reality.activity.AugmentedView;
import com.jwetherell.augmented_reality.activity.TargetedAugmentedReality;
import com.jwetherell.augmented_reality.ui.objects.PaintableBox;
import com.jwetherell.augmented_reality.ui.objects.PaintableIcon;
import com.jwetherell.augmented_reality.ui.objects.PaintableLine;
import com.jwetherell.augmented_reality.ui.objects.PaintablePosition;

public class TargetMarker extends CenteredIconMarker {

    public static final int MARGIN = 10;
    static PaintableBox target;
    static PaintablePosition targetContainer;
    static PaintableLine targetLine;
    static PaintablePosition targetLineContainer;
    public static Bitmap ArMarker;
    private Bitmap otherBitmap;
    private float lineHeight = -1;
    private float side = -1;

    public TargetMarker(String name, double latitude, double longitude, double altitude, Bitmap bitmap) {
        super(name, latitude, longitude, altitude, Color.GREEN, bitmap);
        if (ArMarker != null) {
            this.bitmap = ArMarker;
            this.otherBitmap = bitmap;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void drawIcon(Canvas canvas) {
        if (canvas == null || bitmap == null) throw new NullPointerException();

        if (gpsSymbol == null) gpsSymbol = new PaintableIcon(bitmap, bitmap.getWidth(), bitmap.getHeight());

        getScreenPosition().get(locationArray);
        float x = locationArray[0];
        float y = locationArray[1];
        if (AugmentedReality.landscape) {
            x -= gpsSymbol.getWidth() / 2;
            y -= gpsSymbol.getHeight();
        } else {
            y -= gpsSymbol.getHeight() / 2;
        }
        float currentAngle = 0;
        if (AugmentedReality.landscape) currentAngle = -90;

        if (isClosest()) {
            if (side < 0) {
                side = Math.max(gpsSymbol.getHeight(), gpsSymbol.getWidth()) + MARGIN;
                lineHeight = TargetedAugmentedReality.MAX_CANVAS - y - side / 2;
            }
            float si = side / 2;

            target = PaintableBox.setup(target, side, side, Color.WHITE, Color.TRANSPARENT, false);
            target.setStrokeWidth(4 * AugmentedView.density);
            targetContainer = PaintablePosition.setup(targetContainer, target, x - si, y - si, currentAngle, 1);

            targetLine = PaintableLine.setup(targetLine, Color.WHITE, 0, lineHeight);
            targetLine.setStrokeWidth(2 * AugmentedView.density);
            targetLineContainer = PaintablePosition.setup(targetLineContainer, targetLine, x, y + side / 2, currentAngle, 1);

            targetContainer.paint(canvas);
            targetLineContainer.paint(canvas);
        }
        symbolContainer = PaintablePosition.setup(symbolContainer, gpsSymbol, x, y, currentAngle, 1);
        symbolContainer.paint(canvas);
    }

    @Override
    protected synchronized void drawText(Canvas canvas) {
        // No text.
    }

    public Bitmap getBitmap() {
        return otherBitmap != null ? otherBitmap : bitmap;
    }

    @Override
    public String toString() {
        return "TargetMarker " + name + " [" + distance + "]";
    }
}
