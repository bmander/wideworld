package edu.mit.media.wideworld;

import java.util.List;

import org.osmdroid.util.GeoPoint;

import edu.mit.media.wideworld.RouteServer.Response;
import edu.mit.media.wideworld.constants.OpenStreetMapConstants;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class MainActivity extends FragmentActivity
{
	class CityPreferenceChangeListener implements OnSharedPreferenceChangeListener {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String tag) {
			
			if( tag.equals("citygetter") ){
				String newPrefix = prefs.getString("citygetter", null);
				
				cities = CitiesFile.getInstances(MainActivity.this);
				if( newPrefix!=null && cities!=null ){
					for(int i=0; i<cities.size(); i++){
						CityInstance city = cities.get(i);
						if( city.prefix.equals( newPrefix ) ){
							MainActivity.this.city = city;
							break;
						}
					}
				} else {
					return;
				}
				
				Log.v("DEBUG", "new city is "+MainActivity.this.city);
				
				//change routeServer
				if( MainActivity.this.routeServer!=null ){
					MainActivity.this.routeServer.url = MainActivity.this.city.route_server;
				}
				
				//change map
				MapFragment mapFragment = (MapFragment) MainActivity.this.getFragmentManager().findFragmentByTag("map");
				
				SharedPreferences mapPrefs = MainActivity.this.getSharedPreferences(OpenStreetMapConstants.PREFS_NAME, Context.MODE_PRIVATE);
				final SharedPreferences.Editor edit = mapPrefs.edit();
				edit.putInt(OpenStreetMapConstants.PREFS_CENTER_LAT, (int) (city.center[0]*1E6));
				edit.putInt(OpenStreetMapConstants.PREFS_CENTER_LON, (int) (city.center[1]*1E6));
				edit.putInt(OpenStreetMapConstants.PREFS_ZOOM_LEVEL, city.default_zoom);
				edit.commit();
				
				if( mapFragment != null ){
					Log.v("DEBUG", "map fragment does exist");
					// if it does, modify its state
					
					// set tile source
					mapFragment.setTileSource( city.tile_server );
					// set center
					mapFragment.setCenter( city.center[0], city.center[1] );
					// set bounds
					// set zoomlevel
					mapFragment.setZoom( city.default_zoom );
					
			        Log.v("DEBUG", "set map on pref change:"+city.default_zoom+" lat:"+city.center[0]+" lon:"+city.center[1]);

				}
				

				

			}
			Log.v("DEBUG", tag+" changed");
			
		}
		
	}
	
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
	private static final int SETUP_ACTIVITY = 0;
	
	List<CityInstance> cities = null;
	CityInstance city = null;
	
    RouteServer routeServer;
	
	boolean useTransit;
	boolean useBikeshare;
	TerminusManager orig;
	TerminusManager dest;
	
	LocationManager locationManager;
	CityPreferenceChangeListener cityChangeListener;

	private double bike_speed;
	
	protected Response routeResponse;
	
    // ===========================================================
    // Constructors
    // ===========================================================
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        /* start setup activity, if wideworld has not yet been set up*/
        Intent intent = new Intent(this, SetupActivity.class);
        startActivityForResult(intent,SETUP_ACTIVITY);
        
        cityChangeListener = new CityPreferenceChangeListener();
    	PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(cityChangeListener);

        /* get the current city */
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String cityPrefix = sharedPref.getString("citygetter", null);
        cities = CitiesFile.getInstances(this);
        if(cityPrefix!=null && cities!=null){
        	for(int i=0; i<cities.size(); i++){
        		CityInstance city = cities.get(i);
        		if(city.prefix.equals(cityPrefix)){
        			this.city = city;
        			break;
        		}
        	}
        }
        
        Log.v("DEBUG", "current city:"+city);
        
        /* if we were able to get the current city, set the state of the map */
        if(city!=null){
			SharedPreferences mapPrefs = MainActivity.this.getSharedPreferences(OpenStreetMapConstants.PREFS_NAME, Context.MODE_PRIVATE);
			
			
			if(!mapPrefs.contains(OpenStreetMapConstants.PREFS_CENTER_LAT) ||
			   !mapPrefs.contains(OpenStreetMapConstants.PREFS_CENTER_LON) ||
			   !mapPrefs.contains(OpenStreetMapConstants.PREFS_ZOOM_LEVEL)) {
				final SharedPreferences.Editor edit = mapPrefs.edit();
				edit.putInt(OpenStreetMapConstants.PREFS_CENTER_LAT, (int) (city.center[0]*1E6));
				edit.putInt(OpenStreetMapConstants.PREFS_CENTER_LON, (int) (city.center[1]*1E6));
				edit.putInt(OpenStreetMapConstants.PREFS_ZOOM_LEVEL, city.default_zoom);
				edit.commit();
			}
        }
        
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
        	
        	useTransit = savedInstanceState.getBoolean("useTransit");
        	useBikeshare = savedInstanceState.getBoolean("useBikeshare");
        } else {
        	useTransit = true;
        	useBikeshare = true;
        }
        
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        if( city!=null ){
        	routeServer = new RouteServer(city.route_server);
        } else {
        	routeServer = null;
        }
                
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
    protected void onResume() {
    	
    	
    	super.onResume();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void openSettings() {
		Log.v("DEBUG", "open settings now");
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
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
    	
    	// but transit preference into state
    	outState.putBoolean("useTransit", useTransit);
    	outState.putBoolean("useBikeshare", useBikeshare);
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
    	PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(cityChangeListener);

    	Log.v("DEBUG", "destroying activity");
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETUP_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Log.v("DEBUG", "SETUP SUCCESSFUL");
                Log.v("DEBUG", "prefix is "+data.getStringExtra("prefix"));
                Log.v("DEBUG", "consent is "+data.getBooleanExtra("consent",false));
            } else {
            	Log.v("DEBUG", "BACKED OUT OF SETUP");
            }
        }
    }
    
	public void onCheckboxClicked(View view) {
	    // Is the view now checked?
	    this.useTransit = ((CheckBox) view).isChecked();
	}
	
	public void onBikeshareCheckboxClicked(View view){
		this.useBikeshare = ((CheckBox) view).isChecked();
	}


	protected void findAndDisplayRoute() {		
		
		if( this.getOrigin()==null || this.getDestination()==null ){
			return;
		}
		
		// the routeserver can be null if the server was not configured or is misconfigured
		if( this.routeServer==null ){
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
		this.routeServer.getRoute(this.routeServer.new Request(lat1, lng1, lat2, lng2, useTransit, useBikeshare, bike_speed), new RouteServer.FetchRouteCallback(){
			public void onResponse(RouteServer.Response resp){
				routeResponse = resp;
				finishGetRoute();
				Log.v("DEBUG", "finish get route.");
				
				//Toast toast = Toast.makeText(superthis, "total time: "+(routeResponse.lastLocTime-firstLocTime)/60+"m", Toast.LENGTH_SHORT);
				Toast toast = Toast.makeText(superthis, "total time: "+routeResponse.duration()/60+"m", Toast.LENGTH_SHORT);
				toast.show();
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
			if(location==null){ continue; }
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

		MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentByTag("map");
		if(mapFrag!=null){
			mapFrag.drawRoute();
		}
	}

	public double getBikeSpeed() {
		return this.bike_speed;
	}

	public void setBikeSpeed(double bikeSpeed) {
		this.bike_speed = bikeSpeed;
	}



    
}
