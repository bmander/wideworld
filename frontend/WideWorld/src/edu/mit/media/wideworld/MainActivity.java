package edu.mit.media.wideworld;

import java.util.List;

import org.osmdroid.util.GeoPoint;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class MainActivity extends FragmentActivity
{
    RouteServer routeServer;
	
	boolean useTransit;
	private GeoPoint orig;
	private GeoPoint dest;
	
	static int TERMINUS_GPS = 1;
	static int TERMINUS_ADDRESS = 2;
	static int TERMINUS_BLANK = 3;
	static int TERMINUS_MAP = 4;
	
	int orig_state = TERMINUS_BLANK;
	int dest_state = TERMINUS_BLANK;
	
	LocationManager locationManager;
	
    // ===========================================================
    // Constructors
    // ===========================================================
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
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
					//ft.attach(mapFragment);
					mapFragment.getView().setVisibility(View.VISIBLE);
				}
				
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
				Fragment mapFragment = superthis.getFragmentManager().findFragmentByTag("map");
//				ft.detach( mapFragment );	
				mapFragment.getView().setVisibility(View.GONE);
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
					//ft.attach(navFragment);
					navFragment.getView().setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
				Fragment navFragment = superthis.getFragmentManager().findFragmentByTag("nav");
//				ft.detach( navFragment );	
				navFragment.getView().setVisibility(View.GONE);
			}
        	
        });
        
        actionBar.addTab(navTab);
        actionBar.addTab(mapTab);
    }
    
	public void onCheckboxClicked(View view) {
	    // Is the view now checked?
	    this.useTransit = ((CheckBox) view).isChecked();
	    	    
	}


	public void setDestinationFromMap(GeoPoint pt) {
		Log.v("DEBUG", "set destination from map");
		dest = pt;
		dest_state = TERMINUS_MAP;
		
		ControlFragment navFragment = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFragment != null){
			//navFragment.setDestinationFromMap( pt );
		}
	}

	public void findAndDisplayRoute() {
		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if(mapFrag != null){
			mapFrag.findAndDisplayRoute();
		}
	}

	public void setOriginFromAddress(Address selected) {
		orig = new GeoPoint( selected.getLatitude(), selected.getLongitude() );
		orig_state = TERMINUS_ADDRESS;
		
		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if(mapFrag!=null){
			mapFrag.setOriginIcon( orig );
		}
		ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFrag!=null){
			navFrag.orig.setFromAddress( selected );
		}

	}

	public void setOriginFromMyLocation() {
		orig_state = MainActivity.TERMINUS_GPS;
		
		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if(mapFrag!=null){
			mapFrag.removeOriginIcon();
		}
		ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFrag!=null){
			navFrag.orig.setFromMyLocation();
		}
	}

	public void setOriginFromMap(GeoPoint pt) {
		Log.v("DEBUG", "set origin from map");
		orig = pt;
		orig_state = TERMINUS_MAP;
		
		ControlFragment navFragment = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFragment != null){
			navFragment.orig.setFromMap( pt );
		}
		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if(mapFrag!=null){
			mapFrag.setOriginIcon( orig );
		}
	}

	public void clearOrigin() {
		orig_state = MainActivity.TERMINUS_BLANK;
		orig = null;
		
		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if( mapFrag!=null){
			mapFrag.removeOriginIcon();
		}
		ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFrag!=null){
			navFrag.orig.clear();
		}
	}

	public GeoPoint getOrigin() {
		if( orig_state == TERMINUS_GPS ){
			return getBestCachedGPSLocation();
		} else {
			return orig;
		}
	}

	public GeoPoint getDestination() {
		if( dest_state == TERMINUS_GPS ){
			return getBestCachedGPSLocation();
		} else {
			return dest;
		}
	}

	private GeoPoint getBestCachedGPSLocation() {
		List<String> providers = locationManager.getAllProviders();
		Location bestLocation = null;
		for(int i=0; i<providers.size(); i++){
			String provider = providers.get(i);
			Location location = locationManager.getLastKnownLocation( provider );
			if( bestLocation==null || location.getAccuracy()<bestLocation.getAccuracy() ){
				bestLocation = location;
			}
		}
		if( bestLocation==null ){
			return null;
		}
		return new GeoPoint( bestLocation.getLatitude(), bestLocation.getLongitude() );
	}

	public void startGetRoute() {
		ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFrag!=null){
			navFrag.startGetRoute();
		}
	}

	public void finishGetRoute() {
		ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
		if(navFrag!=null){
			navFrag.finishGetRoute();
		}
	}



    
}
