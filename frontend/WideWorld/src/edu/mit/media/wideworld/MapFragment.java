// Created by plusminus on 00:23:14 - 03.10.2008
package edu.mit.media.wideworld;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.PathOverlay;

import edu.mit.media.wideworld.constants.OpenStreetMapConstants;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Fragment;
//import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Default map view activity.
 * 
 * @author Marc Kurtz
 * @author Manuel Stahl
 * 
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MapFragment extends Fragment implements OpenStreetMapConstants
{

    // ===========================================================
    // Fields
    // ===========================================================

    private SharedPreferences mPrefs;
    private static MapView mMapView;
    private ResourceProxy mResourceProxy;
    private List<PathOverlay> mPathOverlays;
    
    RouteServer routeServer;
    


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        routeServer = new RouteServer("wideworld.media.mit.edu", "bos");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {    	
    	Log.v("DEBUG", "CREATE VIEW; mPathOverlays already has "+mPathOverlays+" items");
        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy);
        mMapView.setUseSafeCanvas(true);
        setHardwareAccelerationOff();
        
        if(mPathOverlays != null){
        	for(int i=0; i<mPathOverlays.size(); i++){
        		PathOverlay po = mPathOverlays.get(i);
        		mMapView.getOverlays().add(po);
        	}
        }
        
        return mMapView;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff()
    {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
		//final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        // mResourceProxy = new ResourceProxyImpl(getActivity().getApplicationContext());

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        
        Overlay overlay = new GestureOverlay(context);
		mMapView.getOverlays().add(overlay);
		
		MyLocationOverlay locOverlay = new MyLocationOverlay(context, mMapView);	
		mMapView.getOverlays().add(locOverlay);
		locOverlay.enableMyLocation();
		
		Log.v("DEBUG", locOverlay.toString() );

        mMapView.getController().setZoom(mPrefs.getInt(PREFS_ZOOM_LEVEL, 1));
        mMapView.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));

        setHasOptionsMenu(true);
    }

    @Override
    public void onPause()
    {
        final SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(PREFS_TILE_SOURCE, mMapView.getTileProvider().getTileSource().name());
        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        edit.commit();

        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        final String tileSourceName = mPrefs.getString(PREFS_TILE_SOURCE,
                TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        try {
            final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
            mMapView.setTileSource(tileSource);
        } catch (final IllegalArgumentException ignore) {
        }
    }
	
	private void findAndDisplayRoute() {
		final MainActivity context = (MainActivity)this.getActivity();
		
	    boolean useTransit = context.useTransit;
		
		if( context.orig==null || context.dest==null ){
			return;
		}
		
		// get the orig and dest points
		double lat1 = context.orig.getLatitudeE6()/1E6;
		double lng1 = context.orig.getLongitudeE6()/1E6;
		double lat2 = context.dest.getLatitudeE6()/1E6;
		double lng2 = context.dest.getLongitudeE6()/1E6;
				
		routeServer.getRoute(routeServer.new Request(lat1, lng1, lat2, lng2, useTransit, 4.5), new RouteServer.FetchRouteCallback(){
			public void onResponse(RouteServer.Response resp){
				// remove all the old paths from the map
				if(mPathOverlays != null){
					List<Overlay> allOverlays = mMapView.getOverlays();
					for(int i=0; i<mPathOverlays.size(); i++){
						allOverlays.remove(mPathOverlays.get(i));
					}
				}
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
						PathOverlay pathoverlay = new PathOverlay(pathColor,context);
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
						mMapView.getOverlays().add(pathoverlay);
						mPathOverlays.add(pathoverlay);
	
					}
					//mMapView.invalidate();
					Message msg = new Message();
					msg.arg1 = firstLocTime;
					msg.arg2 = lastLocTime;
					msg.obj = context;
					mHandler.sendMessage(msg);
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}
	
	static Handler mHandler = new Handler() {		
        @Override
        public void handleMessage(Message msg) {
        	
			Toast toast = Toast.makeText((Context)msg.obj, "total time: "+(msg.arg2-msg.arg1)/60+"m", Toast.LENGTH_SHORT);
			toast.show();
			
            mMapView.invalidate();
        }

	};

	public class GestureOverlay extends Overlay {
		
		Context context;

		public GestureOverlay(Context ctx) {
			super(ctx);
			context = ctx;
		}

		@Override
		protected void draw(Canvas arg0, MapView arg1, boolean arg2) {
			// TODO Auto-generated method stub
			
		}
		
        public boolean onLongPress(final MotionEvent e, final MapView mapView) {
        	final MainActivity top = (MainActivity)getActivity();
        	
        	final GeoPoint pt = (GeoPoint) mapView.getProjection().fromPixels(e.getX(), e.getY());
        	Log.v("DEBUG", "poke ("+e.getX()+","+e.getY()+"), projected to ("+pt.getLatitudeE6()+","+pt.getLongitudeE6()+")");
        	
        	// build alert dialog
        	AlertDialog.Builder builder = new AlertDialog.Builder(context);
        	builder.setPositiveButton("Destination", new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int id){
        			top.dest = pt;
        			findAndDisplayRoute();
        		}
        	});
        	builder.setNegativeButton("Origin", new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int id){
        			top.orig = pt;
        			findAndDisplayRoute();
        		}
        	});
        	builder.setCancelable(true);
        	AlertDialog ad = builder.create();
        	
        	// show alert dialog
        	ad.show();
        	
            return false;
        }
        
        
	}

}
