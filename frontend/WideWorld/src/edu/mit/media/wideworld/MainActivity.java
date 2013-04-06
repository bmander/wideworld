package edu.mit.media.wideworld;

import java.io.IOException;
import java.util.List;

import edu.mit.media.wideworld.R;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

public class MainActivity extends Activity{
	
	IGeoPoint orig;
	IGeoPoint dest;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.root);
		
		// set up map
		MapView mv = new MapView(this, 256);
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
	
	private void findAndDisplayRoute(){
		Log.v("DEBUG", "orig:"+orig+" dest:"+dest);
	}
	
	public class GestureOverlay extends Overlay {
		
		Context context;

		public GestureOverlay(Context ctx) {
			super(ctx);
			
			context = ctx;
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void draw(Canvas arg0, MapView arg1, boolean arg2) {
			// TODO Auto-generated method stub
			
		}
		
        public boolean onLongPress(final MotionEvent e, final MapView mapView) {
        	Log.v("DEBUG", e.toString());
        	final IGeoPoint pt = mapView.getProjection().fromPixels(e.getX(), e.getY());
        	Log.v("DEBUG", pt.getLatitudeE6()+","+pt.getLongitudeE6());
        	
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
        	ad.show();
        	
            return false;
        }
        
        
	}
	

}
