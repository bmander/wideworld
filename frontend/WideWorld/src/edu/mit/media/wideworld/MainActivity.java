package edu.mit.media.wideworld;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.PathOverlay;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

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
		String desc=null;
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
			this.desc = null;
			
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if( mapFrag!=null){
				if( end == ORIG ){
					mapFrag.removeOriginIcon();
				} else if (end == DEST ){
					mapFrag.removeDestinationIcon();
				}
			}

		}
		
		public void setFromAddress(Address selected) {
			this.pt = new GeoPoint( selected.getLatitude(), selected.getLongitude() );
			this.type = TerminusManager.ADDRESS;
			this.desc = selected.getAddressLine(0);
			
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if(mapFrag!=null){
				if( this.end == ORIG ){
					mapFrag.setOriginIcon( pt );
				} else if( this.end == DEST ){
					mapFrag.setDestinationIcon( pt );
				}
			}


		}
		
		public void setFromMyLocation() {
			type = TerminusManager.GPS;
			this.desc = null;
			
			MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
			if(mapFrag!=null){
				if( this.end == ORIG ){
					mapFrag.removeOriginIcon();
				} else if (this.end == DEST) {
					mapFrag.removeDestinationIcon();
				}
			}

		}
		
		public void setFromMap(GeoPoint pt) {
			this.pt = pt;
			type = TerminusManager.MAP;
			this.desc = null;
			
			ControlFragment navFragment = (ControlFragment) getFragmentManager().findFragmentByTag("nav");
			if(navFragment != null){
				if( this.end == ORIG ){
					navFragment.orig.setFromTerminus();
				} else if( this.end == DEST ){
					navFragment.dest.setFromTerminus();
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
	
	ArrayList<PathOverlay> mPathOverlays=null;
	
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
        if( savedInstanceState != null){
        	// reincarnate the origin
        	int origLat = savedInstanceState.getInt("origLat");
        	int origLon = savedInstanceState.getInt("origLon");
        	int origType = savedInstanceState.getInt("origType");
        	String origDesc = savedInstanceState.getString("origDesc");
        	GeoPoint origPt = new GeoPoint(origLat, origLon);
        	orig.pt = origPt;
        	orig.type = origType;
        	orig.desc = origDesc;
        	
        	// reincarnate the destination
        	int destLat = savedInstanceState.getInt("destLat");
        	int destLon = savedInstanceState.getInt("destLon");
        	int destType = savedInstanceState.getInt("destType");
        	String destDesc = savedInstanceState.getString("destDesc");
        	GeoPoint destPt = new GeoPoint(destLat, destLon);
        	dest.pt = destPt;
        	dest.type = destType;
        	dest.desc = destDesc;
        	
        	bike_speed = savedInstanceState.getDouble("bikeSpeed");
        }
        
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
					ft.show(mapFragment);
				}
				
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
				Fragment mapFragment = superthis.getFragmentManager().findFragmentByTag("map");
				ft.hide(mapFragment);
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
					ft.show( navFragment );
				}
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
				Fragment navFragment = superthis.getFragmentManager().findFragmentByTag("nav");
				ft.hide( navFragment );
			}
        	
        });
        
        actionBar.addTab(navTab,0,false);
        actionBar.addTab(mapTab,1,false);
        
        /* ensure that both fragments, should they somehow live in the fragment manager, are hidden */
        FragmentManager fm = this.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment navFrag = fm.findFragmentByTag("nav");
        if(navFrag != null){
        	ft.hide(navFrag);
        }
        Fragment mapFrag = fm.findFragmentByTag("map");
        if(mapFrag != null){
        	ft.hide(mapFrag);
        }
        ft.commit();
        
        if( savedInstanceState != null ){
        	int tabState = savedInstanceState.getInt( "tabState" );
        	if( tabState!=0 ){
        		int tabPos = tabState-1; //tabState is the tab number plus one, so that 0 stays reserved for no info
        		actionBar.setSelectedNavigationItem(tabPos);
        	} else {
        		actionBar.setSelectedNavigationItem(0);
        	}
        } else {
        	actionBar.setSelectedNavigationItem(0);
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	
    	int tabState = this.getActionBar().getSelectedTab().getPosition()+1;
    	outState.putInt("tabState", tabState );
    	
    	// put origin terminus into instance state
    	outState.putInt("origLat", orig.pt==null ? 0 : orig.pt.getLatitudeE6());
    	outState.putInt("origLon", orig.pt==null ? 0 : orig.pt.getLongitudeE6());
    	outState.putInt("origType", orig.type);
    	outState.putString("origDesc", orig.desc);
    	
    	// put destination terminus into instance state
    	outState.putInt("destLat", dest.pt==null ? 0 : dest.pt.getLatitudeE6());
    	outState.putInt("destLon", dest.pt==null ? 0 : dest.pt.getLongitudeE6());
    	outState.putInt("destType", dest.type);
    	outState.putString("destDesc", dest.desc);
    	
    	// but biking speed into state
    	outState.putDouble("bikeSpeed", bike_speed);
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.v("DEBUG", "pausing activity");
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    	Log.v("DEBUG", "stopping activity");
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	Log.v("DEBUG", "destroying activity");
    }
    
	public void onCheckboxClicked(View view) {
	    // Is the view now checked?
	    this.useTransit = ((CheckBox) view).isChecked();
	    	    
	}


	protected void findAndDisplayRoute() {		
		
		if( this.getOrigin()==null || this.getDestination()==null ){
			return;
		}
		
		// get the orig and dest points
		GeoPoint orig = this.getOrigin();
		double lat1 = orig.getLatitudeE6()/1E6;
		double lng1 = orig.getLongitudeE6()/1E6;
		GeoPoint dest = this.getDestination();
		double lat2 = dest.getLatitudeE6()/1E6;
		double lng2 = dest.getLongitudeE6()/1E6;
		
		double bike_speed = this.getBikeSpeed();
				
		Log.v("DEBUG", "start get route...");
		this.startGetRoute();
		final Activity superthis = this;
		this.routeServer.getRoute(this.routeServer.new Request(lat1, lng1, lat2, lng2, useTransit, bike_speed), new RouteServer.FetchRouteCallback(){
			public void onResponse(RouteServer.Response resp){

				// prepare the path list to accept new paths
				mPathOverlays = new ArrayList<PathOverlay>();
				
				// get list of legs
				JSONArray plan;
				try {
					plan = resp.rawResponse.getJSONArray("plan");
				
					// figure out trip time from first and last leg
					JSONObject firstLeg = plan.getJSONObject(0);
					JSONObject lastLeg = plan.getJSONObject(plan.length()-1);
					
					JSONArray firstLegLocs = firstLeg.getJSONArray("locs");
					JSONArray lastLegLocs = lastLeg.getJSONArray("locs");
					
					JSONObject firstLoc = firstLegLocs.getJSONObject(0);
					JSONObject lastLoc = lastLegLocs.getJSONObject(lastLegLocs.length()-1);
					
					int firstLocTime = firstLoc.getInt("time");
					int lastLocTime = lastLoc.getInt("time");
					
					Log.v("DEBUG", "total time: "+(lastLocTime-firstLocTime)+"s" );
					
					// for each leg
					for(int i=0; i<plan.length(); i++){
						JSONObject leg = plan.getJSONObject(i);
						int pathColor = Color.GRAY;
						if( leg.getString("type").equals("transit") ) {
							pathColor = Color.RED;
						} else if (leg.getString("type").equals("walk") && leg.getString("mode").equals("walk")){
							pathColor = Color.BLUE;
						} else if (leg.getString("type").equals("walk") && leg.getString("mode").equals("bikeshare")){
						    pathColor = Color.GREEN;
						}
						
						// create path overlay
						PathOverlay pathoverlay = new PathOverlay(pathColor,superthis);
						pathoverlay.getPaint().setStrokeWidth(5.0f);
	
						// populate path overlay
						JSONArray leg_locs = leg.getJSONArray("locs");
						for(int j=0; j<leg_locs.length(); j++){
							JSONObject loc = leg_locs.getJSONObject(j);
							double lng = loc.getDouble("lon");
							double lat = loc.getDouble("lat");
							pathoverlay.addPoint(new GeoPoint(lat,lng));
						}	
	
						// add path overlay to map
						mPathOverlays.add(pathoverlay);
	
					}

					Message msg = new Message();
					msg.arg1 = firstLocTime;
					msg.arg2 = lastLocTime;
					msg.obj = this;
					MapFragment mapFragment = (MapFragment) superthis.getFragmentManager().findFragmentByTag("map");
					if( mapFragment != null ){
						MapFragment.mHandler.sendMessage(msg);
					}
					
					Toast toast = Toast.makeText(superthis, "total time: "+(lastLocTime-firstLocTime)/60+"m", Toast.LENGTH_SHORT);
					toast.show();
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
				Log.v("DEBUG", "finish get route.");
				finishGetRoute(); //TODO probably crash, not in handler
				
			}
		});
		
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
