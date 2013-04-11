package edu.mit.media.wideworld;

import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class MainActivity extends FragmentActivity
{

    // ===========================================================
    // Constructors
    // ===========================================================
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
        
		/* TODO: REMOVE AS SOON AS POSSIBLE */
		ThreadPolicy tp = ThreadPolicy.LAX;
		StrictMode.setThreadPolicy(tp);

        // FrameLayout mapContainer = (FrameLayout) findViewById(R.id.map_container);
        // RelativeLayout parentContainer = (RelativeLayout) findViewById(R.id.parent_container);
        FragmentManager fm = this.getSupportFragmentManager();

        MapFragment mapFragment = new MapFragment();

        fm.beginTransaction().add(R.id.map_container, mapFragment).commit();
    }
    

    
}
