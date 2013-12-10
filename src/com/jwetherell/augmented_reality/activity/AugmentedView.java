package com.jwetherell.augmented_reality.activity;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import com.jwetherell.augmented_reality.data.ARData;
import com.jwetherell.augmented_reality.ui.Marker;
import com.jwetherell.augmented_reality.ui.Radar;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class extends the View class and is designed draw the zoom bar, radar
 * circle, and markers on the View.
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class AugmentedView extends View {

    private static final AtomicBoolean drawing = new AtomicBoolean(false);

    private static final Radar radar = new Radar();
    private static final float[] locationArray = new float[3];
    private static final List<Marker> cache = new ArrayList<Marker>();
    private static final TreeSet<Marker> updated = new TreeSet<Marker>();
    private static final int COLLISION_ADJUSTMENT = 50;
    public static final int MAX_MARKERS = 5;
    public static float density;
    private Marker closer;
    private Marker tmpMarker;
    private float closestDist;
    private float tmpDist;
    public static int screenMiddle;
    public static boolean computeClosestMarker = false;
    private OnClosestMarkerListener onClosestMarkerListener;

    public AugmentedView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        Radar.setDensity(density);
    }

    /** {@inheritDoc} */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) return;
        if (screenMiddle == 0) { screenMiddle = AugmentedReality.CANVAS_WIDTH / 2; }

        if (drawing.compareAndSet(false, true)) {
            // Get all the markers
            List<Marker> collection = ARData.getMarkers();

            // Prune all the markers that are out of the radar's radius (speeds
            // up drawing and collision detection)
            cache.clear();
            int i;
            for (i = 0; i < collection.size(); i++) {
                tmpMarker = collection.get(i);
                tmpMarker.update(canvas, 0, 0);
                if (tmpMarker.isOnRadar() && tmpMarker.isInView()) cache.add(tmpMarker);
            }

            // Only show MAX_MARKERS on the screen
//            collection = cache.subList(0, Math.min(cache.size(), MAX_MARKERS));

            if (AugmentedReality.useCollisionDetection) adjustForCollisions(canvas, collection);

            // Draw AR markers in reverse order since the last drawn should be the closest
            ListIterator<Marker> iter = collection.listIterator(collection.size());

            if (computeClosestMarker) {
                closestDist = Float.MAX_VALUE;
                closer = null;
                for (i = 0; i < cache.size(); i++) {
                    tmpMarker = cache.get(i);
                    tmpDist = Math.abs(screenMiddle - tmpMarker.getScreenPosition().getX());
                    tmpMarker.setClosest(false);
                    if (tmpDist < closestDist) {
                        closer = tmpMarker;
                        closestDist = tmpDist;
                    }
                }

                if (closer != null) { closer.setClosest(true); }
                if (onClosestMarkerListener != null) onClosestMarkerListener.onClosestMarker(closer);
            }

            while (iter.hasPrevious()) {
                tmpMarker = iter.previous();
                tmpMarker.draw(canvas);
            }

            // Radar circle and radar markers
            if (AugmentedReality.showRadar) radar.draw(canvas);
            drawing.set(false);
        }
    }

    private static void adjustForCollisions(Canvas canvas, List<Marker> collection) {
        updated.clear();

        // Update the AR markers for collisions
        for (Marker marker1 : collection) {
            if (updated.contains(marker1) || !marker1.isInView()) continue;

            int collisions = 1;
            for (Marker marker2 : collection) {
                if (marker1.equals(marker2) || updated.contains(marker2) || !marker2.isInView()) continue;

                if (marker1.isMarkerOnMarker(marker2)) {
                    marker2.getLocation().get(locationArray);
                    float y = locationArray[1];
                    float h = collisions * COLLISION_ADJUSTMENT;
                    locationArray[1] = y + h;
                    marker2.getLocation().set(locationArray);
                    marker2.update(canvas, 0, 0);
                    collisions++;
                    updated.add(marker2);
                }
            }
            updated.add(marker1);
        }
    }

    public void setOnClosestMarkerListener(OnClosestMarkerListener onClosestMarkerListener) {
        this.onClosestMarkerListener = onClosestMarkerListener;
        if (this.onClosestMarkerListener != null) computeClosestMarker = true;
    }

    public OnClosestMarkerListener getOnClosestMarkerListener() {
        return onClosestMarkerListener;
    }

    public interface OnClosestMarkerListener {
        public void onClosestMarker(@Nullable Marker marker);
    }
}
