package edu.mit.media.wideworld;

import org.osmdroid.util.GeoPoint;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class MainActivity extends FragmentActivity
{
    RouteServer routeServer;
	
	boolean useTransit;
	GeoPoint orig;
	GeoPoint dest;

    // ===========================================================
    // Constructors
    // ===========================================================
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        routeServer = new RouteServer("wideworld.media.mit.edu", "bos");
        
        ActionBar actionBar = this.getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab mapTab = actionBar.newTab();
        mapTab.setText("map");
        final Activity superthis = this;
        mapTab.setTabListener( new TabListener(){

			@Override
			public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabSelected(Tab arg0, FragmentTransaction ft) {
				Fragment mapFragment = superthis.getFragmentManager().findFragmentByTag("map");
				
				if( mapFragment == null ){
					mapFragment = new MapFragment();
					ft.add( android.R.id.content, mapFragment, "map" );
				} else {
					ft.attach(mapFragment);
				}
				
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
				Fragment mapFragment = superthis.getFragmentManager().findFragmentByTag("map");
				ft.detach( mapFragment );				
			}
        	
        });
        
        Tab navTab = actionBar.newTab();
        navTab.setText("nav");
        navTab.setTabListener( new TabListener(){

			@Override
			public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTabSelected(Tab arg0, FragmentTransaction ft) {
				Fragment navFragment = superthis.getFragmentManager().findFragmentByTag("nav");
				
				if( navFragment == null ) {
					navFragment = new ControlFragment();
					ft.add( android.R.id.content, navFragment, "nav" );
				} else {
					ft.attach(navFragment);
				}
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
				Fragment navFragment = superthis.getFragmentManager().findFragmentByTag("nav");
				ft.detach( navFragment );	
			}
        	
        });
        
        actionBar.addTab(navTab);
        actionBar.addTab(mapTab);
    }
    
	public void onCheckboxClicked(View view) {
	    // Is the view now checked?
	    this.useTransit = ((CheckBox) view).isChecked();
	    	    
	}

    
}
