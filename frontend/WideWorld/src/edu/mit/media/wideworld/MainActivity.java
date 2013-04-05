package edu.mit.media.wideworld;

import edu.mit.media.wideworld.R;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import org.osmdroid.views.MapView;

public class MainActivity extends Activity {

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
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
