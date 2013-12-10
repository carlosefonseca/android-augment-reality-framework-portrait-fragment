package com.jwetherell.augmented_reality.ui;

import android.graphics.Bitmap;
import com.jwetherell.augmented_reality.activity.TargetedAugmentedReality;
import com.jwetherell.augmented_reality.common.Vector;

/**
 * This class will represent a physical location and will calculate it's
 * visibility and draw it's text and visual representation accordingly. This
 * should be extended if you want to change the way a Marker is viewed.
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class CenteredMarker extends Marker {

    public Bitmap radarIcon;

    private boolean fixedAtCenter = true;


    public void setFixedAtCenter(boolean fixed) {
        fixedAtCenter = fixed;
    }

    public CenteredMarker(String name, double latitude, double longitude, double altitude, int color) {
        super(name, latitude, longitude, altitude, color);
    }

    @Override
    public synchronized Vector getScreenPosition() {
        Vector screenPosition = super.getScreenPosition();
        if (fixedAtCenter) screenPosition.setY(TargetedAugmentedReality.CANVAS_HEIGHT / 2);
        return screenPosition;
    }

    /*private synchronized void updateView() {
        isInView = false;

        // If it's not on the radar, can't be in view3
        if (!isOnRadar) return;

        // If it's not in the same side as our viewing angle
        locationXyzRelativeToCameraView.get(locationArray);
        if (locationArray[2] >= -1f) return;

        locationXyzRelativeToCameraView.get(locationArray);
        float x = locationArray[0];
        float y = locationArray[1];

        float width = getWidth();
        float height = getHeight();

        if (AugmentedReality.landscape) {
            x -= height / 2;
            y += width / 2;
        } else {
            x -= width / 2;
            y -= height / 2;
        }

        float ulX = x;
        float ulY = y;

        float lrX = x;
        float lrY = y;
        if (AugmentedReality.landscape) {
            lrX += height;
            lrY -= width;
        } else {
            lrX += width;
            lrY += height;
        }

        boolean withinY1 = fixedAtCenter || ulY >= -1 && lrY <= AugmentedReality.CANVAS_WIDTH;
        boolean withinY2 = fixedAtCenter || lrY >= -1 && ulY <= AugmentedReality.CANVAS_HEIGHT;

        if (AugmentedReality.landscape && (lrX >= -1 && ulX <= AugmentedReality.CANVAS_WIDTH && withinY1)) {
            isInView = true;
        } else if (lrX >= -1 && ulX <= AugmentedReality.CANVAS_WIDTH && withinY2) {
            isInView = true;
        }
        *//*
         * Log.w("updateView", "name "+this.name); Log.w("updateView",
         * "ul (x="+(ulX)+" y="+(ulY)+")"); Log.w("updateView",
         * "lr (x="+(lrX)+" y="+(lrY)+")"); Log.w("updateView",
         * "cam (w="+(cam.getWidth())+" h="+(cam.getHeight())+")"); if
         * (!isInView) Log.w("updateView", "isInView "+isInView); else
         * Log.e("updateView", "isInView "+isInView);
         *//*
    }*/
}
