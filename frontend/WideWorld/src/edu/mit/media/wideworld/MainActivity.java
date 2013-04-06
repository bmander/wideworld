package edu.mit.media.wideworld;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import edu.mit.media.wideworld.R;

import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.PathOverlay;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends Activity{
	
	IGeoPoint orig;
	IGeoPoint dest;
	MapView mv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/* TODO: REMOVE AS SOON AS POSSIBLE */
		ThreadPolicy tp = ThreadPolicy.LAX;
		StrictMode.setThreadPolicy(tp);
		
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.root);
		
		// set up map
		mv = new MapView(this, 256);
		mv.setMultiTouchControls(true);
		mv.setBuiltInZoomControls(true);
		rl.addView(mv, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
		
		Overlay overlay = new GestureOverlay(this);
		mv.getOverlays().add(overlay);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void findAndDisplayRoute() {
		if( orig==null || dest==null ){
			return;
		}
		
		// get the orig and dest points
		double lat1 = orig.getLatitudeE6()/1E6;
		double lng1 = orig.getLongitudeE6()/1E6;
		double lat2 = dest.getLatitudeE6()/1E6;
		double lng2 = dest.getLongitudeE6()/1E6;
		
		Log.v("DEBUG", "orig:"+orig+" dest:"+dest);
		
		// there's a lot that can go wrong here...
		try {

			// create http client
			HttpClient hc = new DefaultHttpClient();
			HttpResponse response;
			
			// create uri for terminus points
			URI uri = new URI("http://wideworld.media.mit.edu/path?lat1="+lat1+"&lon1="+lng1+"&lat2="+lat2+"&lon2="+lng2);
			Log.v("DEBUG",uri.toString());
			
			// grab response
			response = hc.execute( new HttpGet( uri ));
			StatusLine statusLine = response.getStatusLine();
			
			// if the response is OK
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
		    	// piece together the response string
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        Log.v("DEBUG", Integer.toString(responseString.length()));
		        
		        // convert the response string into a map overlay
		        try {
		        	// the entire message
					JSONObject object = (JSONObject) new JSONTokener(responseString).nextValue();
					String id1 = object.getString("id1");
					String id2 = object.getString("id2");
					
					// get list of linestrings
					JSONArray geoms = object.getJSONArray("geom");
					
					// for each linestring
					for(int i=0; i<geoms.length(); i++){
						// create path overlay
						PathOverlay pathoverlay = new PathOverlay(Color.RED,this);
						
						// populate path overlay
						JSONArray geom = geoms.getJSONArray(i);
						for(int j=0; j<geom.length(); j++){
							JSONArray pt = geom.getJSONArray(j);
							double lng = pt.getDouble(0);
							double lat = pt.getDouble(1);
							pathoverlay.addPoint(new GeoPoint(lat,lng));
						}
						
						// add path overlay to map
						mv.getOverlays().add(pathoverlay);
					}
					
					Log.v("DEBUG", "id1:"+id1+" id2:"+id2);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    } else{
		        //Closes the connection.
		        response.getEntity().getContent().close();
		        throw new IOException(statusLine.getReasonPhrase());
		    }
			//Log.v("DEBUG", "resp:"+response);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

 catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
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
        	final IGeoPoint pt = mapView.getProjection().fromPixels(e.getX(), e.getY());
        	
        	// build alert dialog
        	AlertDialog.Builder builder = new AlertDialog.Builder(context);
        	builder.setPositiveButton("Destination", new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int id){
        			dest = pt;
        			findAndDisplayRoute();
        		}
        	});
        	builder.setNegativeButton("Origin", new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int id){
        			orig = pt;
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
