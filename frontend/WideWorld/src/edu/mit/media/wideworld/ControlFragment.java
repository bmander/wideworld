package edu.mit.media.wideworld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.util.GeoPoint;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ControlFragment extends Fragment {
	
	static int GEOCODE_FAIL = 1;
	static int GEOCODE_SUCCESS = 2;
	
	class GetAddressTask extends AsyncTask<String,Void,Void> {
		
		private Handler handler;
		private Geocoder geocoder;
		
		GetAddressTask(Handler handler, Geocoder geocoder){
			this.handler = handler;
			this.geocoder = geocoder;
		}
		
		@Override
		protected Void doInBackground(String... arg0) {
			try {
				// stall for half a second before launching geocode, to wait for the user to pause typing
				Thread.sleep(500);
				if( isCancelled() ){
					return null;
				}
				
				String locString = arg0[0];
				Log.v("DEBUG", "geocode this: "+locString);
				List<Address> addresses = geocoder.getFromLocationName(locString, 5, 42.184267, -71.249771, 42.449301, 70.888595);
			
				List<String> resp = new ArrayList<String>();
				for(int i=0; i<addresses.size(); i++){
					resp.add( addresses.get(i).getAddressLine(0) );
					Log.v("DEBUG", addresses.get(i).toString() );
				}
				
				if( isCancelled() ){
					return null;
				}
				Message msg = new Message();
				msg.arg1 = GEOCODE_SUCCESS;
				msg.obj = addresses;
				handler.sendMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.v("DEBUG", e.getMessage() );
				
				if( isCancelled() ){
					return null;
				}
				Message msg = new Message();
				msg.arg1 = GEOCODE_FAIL;
				handler.sendMessage(msg);
			} catch (InterruptedException e) {
				return null;
			}
			
			return null;
		}
		
	}
	
	static class GeocodeResponseHandler extends Handler {
		
		private LinearLayout dropdown;
		private ProgressBar working;
		
		GeocodeResponseHandler(LinearLayout dropdown, ProgressBar working){
			super();
			
			this.dropdown = dropdown;
			this.working = working;
		}
		
		@Override
	    public void handleMessage(Message msg) {
	    	Log.v("DEBUG", "message caught");
	    	
	    	working.setVisibility(View.GONE);
	    	
	    	dropdown.removeAllViews();
	    	
	    	if( msg.arg1 == GEOCODE_FAIL ){
				TextView foo = new TextView(dropdown.getContext());
				foo.setText( "geocode fail" );
				dropdown.addView( foo );
	    	} else {
		    	List<Address> geocodeResults = (List<Address>)msg.obj;
		    	for(int i=0; i<geocodeResults.size(); i++){
					TextView foo = new TextView(dropdown.getContext());
					foo.setFocusable(true);
					Address address = geocodeResults.get(i);
					foo.setText( address.getAddressLine(0) + " " + address.getAddressLine(1) );
					dropdown.addView( foo );
		    	}
	    	}
		    	
			dropdown.setVisibility(View.VISIBLE);
			
	    }
	}
	
	Geocoder geocoder;
	static GeocodeResponseHandler geocodeResponseHandler;
	GetAddressTask getAddressTask = null;
	
	ProgressBar orig_working;
	LinearLayout orig_dropdown;
	EditText orig_text;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		
		final View fragView = inflater.inflate(R.layout.control_fragment, container, false);
		
		orig_working = (ProgressBar)fragView.findViewById(R.id.orig_working);
		
		orig_dropdown = (LinearLayout)fragView.findViewById( R.id.orig_dropdown );
		//orig_dropdown.setBackgroundColor( Color.WHITE );
		geocodeResponseHandler = new GeocodeResponseHandler(orig_dropdown, orig_working);
		//orig_dropdown.removeAllViews();
		
//		TextView foo = new TextView(fragView.getContext());
//		foo.setText("foobarbar");
//		orig_dropdown.addView( foo );
		
		View orig_text_field = fragView.findViewById( R.id.orig_text );
		EditText orig_text = (EditText)fragView.findViewById(R.id.orig_text);

		
		geocoder = new Geocoder(fragView.getContext());
//		
//		final EditText orig_text_field = (EditText)fragView.findViewById(R.id.orig_text);
//		
//        CheckBox use_transit = (CheckBox)fragView.findViewById(R.id.checkbox_usetransit);
//        use_transit.setChecked(true);
//        
//        Spinner spinner = (Spinner) fragView.findViewById(R.id.speed_spinner);
//        // Create an ArrayAdapter using the string array and a default spinner layout
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(fragView.getContext(),
//             R.array.speeds_array, android.R.layout.simple_spinner_item);
//        // Specify the layout to use when the list of choices appears
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        // Apply the adapter to the spinner
//        spinner.setAdapter(adapter);
//        
//        // respond to Go Button click
//        final Button button = (Button) fragView.findViewById(R.id.go_button);
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//            	MainActivity top = (MainActivity)getActivity();
//            	            	
//            	/* if there's an address in the origin box, use it as start point */
//            	EditText orig_text_field = (EditText)fragView.findViewById(R.id.orig_text);
//            	String orig_text = orig_text_field.getText().toString();
//            	if( orig_text.length() > 0 ){
//	            	try {
//	            		//TODO use non-hardcoded bounding box
//						List<Address> addresses = geocoder.getFromLocationName(orig_text, 1, 42.184267, -71.249771, 42.449301, 70.888595);
//												
//						if( addresses.size() > 0 ){
//							Address addy = addresses.get(0);
//							top.orig = new GeoPoint(addy.getLatitude(), addy.getLongitude());							
//						}
//						
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//            	}
//            	
//            	/* if there's an address in the dest box, use it as end point */
//            	EditText dest_text_field = (EditText)fragView.findViewById(R.id.dest_text);
//            	String dest_text = dest_text_field.getText().toString();
//            	if( dest_text.length() > 0 ){
//	            	try {
//	            		//TODO use non-hardcoded bounding box
//						List<Address> addresses = geocoder.getFromLocationName(dest_text, 1, 42.184267, -71.249771, 42.449301, 70.888595);
//												
//						if( addresses.size() > 0 ){
//							Address addy = addresses.get(0);
//							top.dest = new GeoPoint(addy.getLatitude(), addy.getLongitude());							
//						}
//						
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//            	}
//            	
//            	//Log.v("DEBUG", "orig text: "+orig_text.getText() );
//            	
//                Log.v("DEBUG", "CLICK YO");
//            }
//        });
//        
//        final AutoCompleteTextView fromEdit = (AutoCompleteTextView)fragView.findViewById(R.id.orig_text);
//        final ArrayAdapter<String> fa = new ArrayAdapter<String>(fragView.getContext(),
//                android.R.layout.simple_dropdown_item_1line);
//        
//		mHandler = new Handler() {		
//	        @SuppressWarnings("unchecked")
//			@Override
//	        public void handleMessage(Message msg) {
//	        	Log.v("DEBUG", "message caught");
//	        	fa.clear();
//	        	
//	        	List<String> geocodeResults = (List<String>)msg.obj;
//	        	
//	        	for(int i=0; i<geocodeResults.size(); i++){
//	        		fa.add( geocodeResults.get(i) );
//	        	}
//	        	fromEdit.showDropDown();
//	        }
//		};
//		class GetAddressTask extends AsyncTask<String,Void,Void> {
//
//			@Override
//			protected Void doInBackground(String... arg0) {
//				try {
//					String locString = arg0[0];
//					Log.v("DEBUG", "geocode this: "+locString);
//					List<Address> addresses = geocoder.getFromLocationName(locString, 1, 42.184267, -71.249771, 42.449301, 70.888595);
//				
//					List<String> resp = new ArrayList<String>();
//					for(int i=0; i<addresses.size(); i++){
//						resp.add( addresses.get(i).getAddressLine(0) );
//						Log.v("DEBUG", addresses.get(i).toString() );
//					}
//					
//					resp.add( locString+"-blah" );
//					
//					Message msg = new Message();
//					msg.obj = resp;
//					mHandler.sendMessage(msg);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//				return null;
//			}
//			
//		}
//
        orig_text.addTextChangedListener( new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				
				Log.v("DEBUG", arg0.toString() );
				
				if( arg0.length()<2 ){
					orig_working.setVisibility(View.GONE);
					orig_dropdown.setVisibility(View.GONE);
					if( getAddressTask != null ) {
						getAddressTask.cancel(true);
					}
					return;
				}
				
				if( getAddressTask != null ){
					getAddressTask.cancel(true);
				}
				
				orig_working.setVisibility(View.VISIBLE);
				getAddressTask = new GetAddressTask(geocodeResponseHandler, geocoder);
				getAddressTask.execute( arg0.toString() );
				
				
//				try {
//					List<Address> addresses = geocoder.getFromLocationName(arg0.toString(), 1, 42.184267, -71.249771, 42.449301, 70.888595);
//					Log.v("DEBUG", addresses.toString());
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				//new GetAddressTask().execute(arg0.toString());
				
//			    PopupMenu popup = new PopupMenu(fragView.getContext(), orig_text_field);
//			    MenuInflater inflater = popup.getMenuInflater();
//			    inflater.inflate(R.menu.blank, popup.getMenu());
//			    Menu blank = popup.getMenu();
//			    blank.add("foobar");
//			    blank.add("bizbaz");
//			    popup.show();

			}
        	
        });
//        fromEdit.setAdapter(fa);
		
		return fragView;
	}
	


}