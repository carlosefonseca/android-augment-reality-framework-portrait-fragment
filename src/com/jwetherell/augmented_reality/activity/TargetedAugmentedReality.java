package com.jwetherell.augmented_reality.activity;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;
import ar.com.carlosefonseca.common.utils.UnitUtils;
import com.jwetherell.augmented_reality.R;
import com.jwetherell.augmented_reality.camera.CameraSurface;
import com.jwetherell.augmented_reality.data.ARData;
import com.jwetherell.augmented_reality.ui.Marker;
import com.jwetherell.augmented_reality.ui.Radar;
import com.jwetherell.augmented_reality.ui.TargetMarker;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

/**
 * This class extends the SensorsActivity and is designed tie the AugmentedView
 * and zoom bar together.
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class TargetedAugmentedReality extends AugmentedReality implements OnTouchListener, AugmentedView.OnClosestMarkerListener {

    private static final String TAG = "beware." + TargetedAugmentedReality.class.getSimpleName();
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("@#");
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    public static final int DETAILS_HEIGHT_DP = 80;
    public static final int DETAILS_TEXT_SIZE_SP = 20;
    public static final int TRANSPARENT_DARK = Color.parseColor("#99000000");
    public static float D;
    public static int MAX_CANVAS = -1;
    private static String END_TEXT = FORMAT.format(TargetedAugmentedReality.MAX_ZOOM) + " km";

    protected static CameraSurface camScreen = null;
    protected static TextView endLabel = null;
    protected static AugmentedView augmentedView = null;

    public static float MAX_ZOOM = 10; // in KM

    public static boolean landscape = false;
    public static boolean useCollisionDetection = false;
    public static boolean showRadar = true;
    public static boolean showZoomBar = false;
    private TextView pointDetails;
    private int detailsHeight;
    private TargetMarker currentMarker;

    public static void metric() {
        UnitUtils.setSystem(UnitUtils.System.METRIC);
        END_TEXT = UnitUtils.stringForDistance((int) TargetedAugmentedReality.MAX_ZOOM);
        if (endLabel != null) endLabel.setText(END_TEXT);
    }

    public static void imperial() {
        UnitUtils.setSystem(UnitUtils.System.IMPERIAL);
        END_TEXT = UnitUtils.stringForDistance((int) TargetedAugmentedReality.MAX_ZOOM);
        if (endLabel != null) endLabel.setText(END_TEXT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        D = getResources().getDisplayMetrics().density;

        FrameLayout frameLayout = new FrameLayout(getActivity());

        camScreen = new CameraSurface(getActivity());
        frameLayout.addView(camScreen);

        Radar.circleImage = getRadarBitmap();
        Radar.RADIUS_DP = 53f;
        Radar.radarPosition = Radar.Position.TOP_RIGHT;


        augmentedView = new AugmentedView(getActivity());
        augmentedView.setOnTouchListener(this);
        augmentedView.setOnClosestMarkerListener(this);
        LayoutParams augLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        grabCanvasSize(augmentedView);
        frameLayout.addView(augmentedView, augLayout);

        int ten = (int) (10 * D);
        pointDetails = new AutoResizeTextView(getActivity());
        pointDetails.setBackgroundColor(TRANSPARENT_DARK);
        pointDetails.setGravity(Gravity.CENTER_VERTICAL);
        pointDetails.setTextColor(Color.WHITE);
        pointDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, DETAILS_TEXT_SIZE_SP);
        ((AutoResizeTextView)pointDetails).setMinTextSize(10);
        pointDetails.setPadding(ten, ten, ten, ten);
        pointDetails.setCompoundDrawablePadding(ten);
        pointDetails.setMaxLines(3);
        detailsHeight = (int) (DETAILS_HEIGHT_DP * D);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, detailsHeight, Gravity.BOTTOM);
        frameLayout.addView(pointDetails, lp);

        updateDataOnZoom();

        return frameLayout;
    }

    public static Bitmap createRecoloredBitmap(Bitmap source, int color) {
        Bitmap mask = source.extractAlpha();

        Bitmap targetBitmap = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawBitmap(mask, 0, 0, paint);

        return targetBitmap;
    }

    private Bitmap getRadarBitmap() {

        Bitmap immutableBmp = BitmapFactory.decodeResource(getResources(), R.drawable.radar_view_green);
        return createRecoloredBitmap(immutableBmp, Color.WHITE);
//        Bitmap bitmap = immutableBmp.copy(Bitmap.Config.ARGB_8888, true);
//        Canvas canvas = new Canvas(bitmap);
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setColor(TRANSPARENT_DARK);
//        int r = canvas.getClipBounds().right / 2;
//        canvas.drawCircle(r, r, r, paint);
//        return bitmap;
    }

    private void grabCanvasSize(final View view) {
        if (view.getViewTreeObserver() != null) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    CANVAS_HEIGHT = view.getHeight();
                    CANVAS_WIDTH = view.getWidth();
                    MAX_CANVAS = CANVAS_HEIGHT - detailsHeight;
                }
            });
        }
    }

    @Override
    public float getMaxZoom() {
        return MAX_ZOOM;
    }

    @Override
    public void setMaxZoom(float maxZoomInKm) {
        MAX_ZOOM = maxZoomInKm;
        updateDataOnZoom();
        if (camScreen != null) camScreen.invalidate();
        END_TEXT = UnitUtils.stringForDistance((int) MAX_ZOOM);
        //FORMAT.format(MAX_ZOOM) + " km";
        if (endLabel != null) {
            endLabel.setText(END_TEXT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSensorChanged(SensorEvent evt) {
        super.onSensorChanged(evt);

        if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER || evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            augmentedView.postInvalidate();
        }
    }

    /**
     * Called when the zoom bar has changed.
     */
    @Override
    protected void updateDataOnZoom() {
        float zoomLevel = MAX_ZOOM;
        ARData.setRadius(zoomLevel);
        ARData.setZoomLevel(FORMAT.format(zoomLevel));
        ARData.setZoomProgress(100);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouch(View view, MotionEvent me) {
        // See if the motion event is on a Marker
        for (Marker marker : ARData.getMarkers()) {
            if (marker.handleClick(me.getX(), me.getY())) {
                if (me.getAction() == MotionEvent.ACTION_UP) markerTouched(marker);
                return true;
            }
        }

        return getActivity().onTouchEvent(me);
    }

    @Override
    protected void markerTouched(Marker marker) {
        Log.w(TAG, "markerTouched() not implemented.");
    }

    @Override
    public void onClosestMarker(@Nullable Marker marker) {
        //noinspection ObjectEquality
        if (currentMarker == marker) return;
        currentMarker = (TargetMarker) marker;
        final boolean ok = currentMarker != null;

        BitmapDrawable drawable = ok ? new BitmapDrawable(getResources(), currentMarker.getBitmap()) : null;
        pointDetails.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

        pointDetails.setText(ok ? String.format("%s (%s)", currentMarker.getName(), formatDistance(currentMarker.getDistance())) : "");
    }

    public static String formatDistance(double distance) {
        return UnitUtils.stringForDistance((int) distance);
/*
        if (distance < 1000.0) {
            return DECIMAL_FORMAT.format(distance) + "m";
        } else {
            double d = distance / 1000.0;
            return DECIMAL_FORMAT.format(d) + "km";
        }
*/
    }
}
