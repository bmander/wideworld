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
import android.view.View;
import android.widget.CheckBox;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class MainActivity extends FragmentActivity
{
	class TerminusManager{
		static final int GPS = 1;
		static final int ADDRESS = 2;
		static final int BLANK = 3;
		static final int MAP = 4;
		
		static final int ORIG = 1;
		static final int DEST = 2;
		
		MainActivity activity;
		GeoPoint pt=null;
		int type;
		int end;
		
		TerminusManager(MainActivity activity, int end){
			this.type = BLANK;
			this.end = end;
			this.activity = activity;
		}
		
		public void clear() {
			this.type = TerminusManager.BLANK;
			this.pt = null;
			
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if( mapFrag!=null){
				if( end == ORIG ){
					mapFrag.removeOriginIcon();
				} else if (end == DEST ){
					mapFrag.removeDestinationIcon();
				}
			}
			ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
			if(navFrag!=null){
				if( end == ORIG){
					navFrag.orig.clear();
				} else if (end==DEST){
					navFrag.dest.clear();
				}
			}
		}
		
		public void setFromAddress(Address selected) {
			this.pt = new GeoPoint( selected.getLatitude(), selected.getLongitude() );
			this.type = TerminusManager.ADDRESS;
			
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if(mapFrag!=null){
				if( this.end == ORIG ){
					mapFrag.setOriginIcon( pt );
				} else if( this.end == DEST ){
					mapFrag.setDestinationIcon( pt );
				}
			}
			ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
			if(navFrag!=null){
				if( this.end == ORIG ) {
					navFrag.orig.setFromAddress( selected );
				} else if (this.end == DEST) {
					navFrag.dest.setFromAddress( selected );
				}
			}

		}
		
		public void setFromMyLocation() {
			type = TerminusManager.GPS;
			
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if(mapFrag!=null){
				if( this.end == ORIG ){
					mapFrag.removeOriginIcon();
				} else if (this.end == DEST) {
					mapFrag.removeDestinationIcon();
				}
			}
			ControlFragment navFrag = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
			if(navFrag!=null){
				if( this.end == ORIG ){
					navFrag.orig.setFromMyLocation();
				} else {
					navFrag.dest.setFromMyLocation();
				}
			}
		}
		
		public void setFromMap(GeoPoint pt) {
			this.pt = pt;
			type = TerminusManager.MAP;
			
			ControlFragment navFragment = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
			if(navFragment != null){
				if( this.end == ORIG ){
					navFragment.orig.setFromMap( pt );
				} else if( this.end == DEST ){
					navFragment.dest.setFromMap( pt );
				}
			}
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if(mapFrag!=null){
				if( this.end == ORIG ){
					mapFrag.setOriginIcon( pt );
				} else if (this.end == DEST){
					mapFrag.setDestinationIcon( pt );
				}
			}
		}
		
	}

	static final double BIKE_SPEED_SLOW = 2.0; // meters/second
	static final double BIKE_SPEED_AVERAGE = 3.1; // meters/second
	static final double BIKE_SPEED_FAST = 4.5; // meters/second
	
    RouteServer routeServer;
	
	boolean useTransit;
	TerminusManager orig;
	TerminusManager dest;
	
	LocationManager locationManager;

	private double bike_speed;
	
    // ===========================================================
    // Constructors
    // ===========================================================
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
                
        orig = new TerminusManager(this, TerminusManager.ORIG);
        dest = new TerminusManager(this, TerminusManager.DEST);
        bike_speed = BIKE_SPEED_SLOW;
        
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


	public void findAndDisplayRoute() {
		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if(mapFrag != null){
			mapFrag.findAndDisplayRoute();
		}
	}

	public GeoPoint getOrigin() {
		if( orig.type == TerminusManager.GPS ){
			return getBestCachedGPSLocation();
		} else {
			return orig.pt;
		}
	}

	public GeoPoint getDestination() {
		if( dest.type == TerminusManager.GPS ){
			return getBestCachedGPSLocation();
		} else {
			return dest.pt;
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

	public double getBikeSpeed() {
		return this.bike_speed;
	}

	public void setBikeSpeed(double bikeSpeed) {
		this.bike_speed = bikeSpeed;
	}



    
}
