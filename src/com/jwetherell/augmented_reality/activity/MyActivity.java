package com.jwetherell.augmented_reality.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.jwetherell.augmented_reality.R;

public class MyActivity extends FragmentActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activity);
    }
}
