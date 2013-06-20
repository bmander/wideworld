package edu.mit.media.wideworld;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
        
        ActionBar actionBar = this.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab mapTab = actionBar.newTab();
        mapTab.setText("map");
        mapTab.setTabListener( new TabListener(){

			@Override
			public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        actionBar.addTab(mapTab);
        
        Tab navTab = actionBar.newTab();
        navTab.setText("nav");
        navTab.setTabListener( new TabListener(){

			@Override
			public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        actionBar.addTab(navTab);

        this.setContentView(R.layout.activity_main);

        // FrameLayout mapContainer = (FrameLayout) findViewById(R.id.map_container);
        // RelativeLayout parentContainer = (RelativeLayout) findViewById(R.id.parent_container);
        FragmentManager fm = this.getSupportFragmentManager();

        MapFragment mapFragment = new MapFragment();

        fm.beginTransaction().add(R.id.map_container, mapFragment).commit();
    }
    

    
}
