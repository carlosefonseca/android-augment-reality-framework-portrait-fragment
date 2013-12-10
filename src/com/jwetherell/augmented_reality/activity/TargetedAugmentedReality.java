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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.jwetherell.augmented_reality.R;
import com.jwetherell.augmented_reality.camera.CameraSurface;
import com.jwetherell.augmented_reality.data.ARData;
import com.jwetherell.augmented_reality.ui.Marker;
import com.jwetherell.augmented_reality.ui.Radar;
import com.jwetherell.augmented_reality.ui.TargetMarker;
import com.jwetherell.augmented_reality.widget.VerticalSeekBar;
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
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private static final CharSequence START_TEXT = "0 m";
    private static final int ZOOMBAR_BACKGROUND_COLOR = Color.argb(125, 55, 55, 55);
    public static final int DETAILS_HEIGHT_DP = 80;
    public static final int DETAILS_TEXT_SIZE_SP = 20;
    public static final int TRANSPARENT_DARK = Color.parseColor("#99000000");
    public static float D;
    public static int MAX_CANVAS = -1;
    private static String END_TEXT = FORMAT.format(TargetedAugmentedReality.MAX_ZOOM) + " km";
    private static final int END_TEXT_COLOR = Color.WHITE;

    protected static CameraSurface camScreen = null;
    @Nullable
    protected static VerticalSeekBar myZoomBar = null;
    protected static TextView endLabel = null;
    protected static LinearLayout zoomLayout = null;
    protected static AugmentedView augmentedView = null;

    public static float MAX_ZOOM = 10; // in KM
    public static float ONE_PERCENT = MAX_ZOOM / 100f;
    public static float TEN_PERCENT = 10f * ONE_PERCENT;
    public static float TWENTY_PERCENT = 2f * TEN_PERCENT;
    public static float EIGHTY_PERCENTY = 4f * TWENTY_PERCENT;

    public static boolean landscape = false;
    public static boolean useCollisionDetection = false;
    public static boolean showRadar = true;
    public static boolean showZoomBar = false;
    private TextView pointDetails;
    private int detailsHeight;
    private TargetMarker currentMarker;


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


        if (showZoomBar) {
            zoomLayout = new LinearLayout(getActivity());
            zoomLayout.setVisibility((showZoomBar) ? LinearLayout.VISIBLE : LinearLayout.GONE);
            zoomLayout.setOrientation(LinearLayout.VERTICAL);
            zoomLayout.setPadding(5, 5, 5, 5);
            zoomLayout.setBackgroundColor(ZOOMBAR_BACKGROUND_COLOR);

            endLabel = new TextView(getActivity());
            endLabel.setText(END_TEXT);
            endLabel.setTextColor(END_TEXT_COLOR);
            LinearLayout.LayoutParams zoomTextParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                                                                     LayoutParams.WRAP_CONTENT);
            zoomTextParams.gravity = Gravity.CENTER;
            zoomLayout.addView(endLabel, zoomTextParams);

            myZoomBar = new VerticalSeekBar(getActivity());
            myZoomBar.setMax(100);
            myZoomBar.setProgress(50);
            myZoomBar.setOnSeekBarChangeListener(myZoomBarOnSeekBarChangeListener);
            LinearLayout.LayoutParams zoomBarParams;
            zoomBarParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            zoomBarParams.gravity = Gravity.CENTER_HORIZONTAL;
            zoomLayout.addView(myZoomBar, zoomBarParams);

            frameLayout.addView(zoomLayout, getZoomUILayoutParams());
        }

        pointDetails = new TextView(getActivity());
        pointDetails.setBackgroundColor(TRANSPARENT_DARK);
        pointDetails.setGravity(Gravity.CENTER_VERTICAL);
        pointDetails.setTextColor(Color.WHITE);
        pointDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, DETAILS_TEXT_SIZE_SP);
        int ten = (int) (10 * D);
        pointDetails.setPadding(ten, ten, ten, ten);
        pointDetails.setCompoundDrawablePadding(ten);
        pointDetails.setMaxLines(2);
        detailsHeight = (int) (DETAILS_HEIGHT_DP * D);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, detailsHeight, Gravity.BOTTOM);
        frameLayout.addView(pointDetails, lp);

        updateDataOnZoom();

//        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        return frameLayout;
    }

    private Bitmap getRadarBitmap() {
        Bitmap immutableBmp = BitmapFactory.decodeResource(getResources(), R.drawable.radar_view_green);
        Bitmap bitmap = immutableBmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(TRANSPARENT_DARK);
        int r = canvas.getClipBounds().right / 2;
        canvas.drawCircle(r, r, r, paint);
        return bitmap;
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

    private FrameLayout.LayoutParams getZoomUILayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.RIGHT);
    }

    public float getMaxZoom() {
        return MAX_ZOOM;
    }

    public void setMaxZoom(float maxZoomInKm) {
        MAX_ZOOM = maxZoomInKm;
        ONE_PERCENT = MAX_ZOOM / 100f;
        TEN_PERCENT = 10f * ONE_PERCENT;
        TWENTY_PERCENT = 2f * TEN_PERCENT;
        EIGHTY_PERCENTY = 4f * TWENTY_PERCENT;
        if (myZoomBar != null) {
            updateDataOnZoom();
            camScreen.invalidate();
        }
        END_TEXT = FORMAT.format(TargetedAugmentedReality.MAX_ZOOM) + " km";
        if (endLabel != null) {
            endLabel.setText(END_TEXT);
        }
    }

    public void setZoomRatio(float ratio) {
        if (myZoomBar != null) {
            myZoomBar.setProgress((int) (getMaxZoom() * ratio));
            updateDataOnZoom();
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

    private OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new OnSeekBarChangeListener() {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateDataOnZoom();
            camScreen.invalidate();
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // Ignore
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            updateDataOnZoom();
            camScreen.invalidate();
        }
    };

    private static float calcZoomLevel() {
        if (myZoomBar == null) return MAX_ZOOM;
        int myZoomLevel = getProgress();
        float myout = 0;

        float percent = 0;
        if (myZoomLevel <= 25) {
            percent = myZoomLevel / 25f;
            myout = ONE_PERCENT * percent;
        } else if (myZoomLevel > 25 && myZoomLevel <= 50) {
            percent = (myZoomLevel - 25f) / 25f;
            myout = ONE_PERCENT + (TEN_PERCENT * percent);
        } else if (myZoomLevel > 50 && myZoomLevel <= 75) {
            percent = (myZoomLevel - 50f) / 25f;
            myout = TEN_PERCENT + (TWENTY_PERCENT * percent);
        } else {
            percent = (myZoomLevel - 75f) / 25f;
            myout = TWENTY_PERCENT + (EIGHTY_PERCENTY * percent);
        }

        return myout;
    }

    /**
     * Called when the zoom bar has changed.
     */
    protected void updateDataOnZoom() {
        float zoomLevel = calcZoomLevel();
        ARData.setRadius(zoomLevel);
        ARData.setZoomLevel(FORMAT.format(zoomLevel));
        ARData.setZoomProgress(getProgress());
    }

    private static int getProgress() {
        return myZoomBar != null ? myZoomBar.getProgress() : 100;
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

    protected void markerTouched(Marker marker) {
        Log.w(TAG, "markerTouched() not implemented.");
    }

    @Override
    public void onClosestMarker(@Nullable Marker marker) {
        if (currentMarker == marker) return;
        currentMarker = (TargetMarker) marker;
        pointDetails.setText(currentMarker != null ? currentMarker.getName() : "");
        BitmapDrawable drawable = currentMarker != null ? new BitmapDrawable(getResources(), currentMarker.getBitmap()) : null;
        pointDetails.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }
}
