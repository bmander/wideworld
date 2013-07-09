package edu.mit.media.wideworld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.util.GeoPoint;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ControlFragment extends Fragment {
	
	public static final int MAX_RETRY = 4;
	public static final int MAX_GEOCODED_LOCATIONS = 8;
	static int GEOCODE_FAIL = 1;
	static int GEOCODE_SUCCESS = 2;

	
	class GetAddressTask extends AsyncTask<String,Void,Void> {
		
		private Handler handler;
		private Geocoder geocoder;
		
		GetAddressTask(Handler handler, Geocoder geocoder){
			this.handler = handler;
			this.geocoder = geocoder;
		}
		
		List<Address> geocodeWithinArea(String loc, int maxResults, double bottom, double left, double top, double right ) throws IOException {
			List<Address> prefilter = geocoder.getFromLocationName(loc, maxResults, bottom, left, top, right);

			List<Address> postfilter = new ArrayList<Address>();
			for( int i=0; i<prefilter.size(); i++){
				Address aa = prefilter.get(i);
				double lat = aa.getLatitude();
				double lon = aa.getLongitude();
				if(lat>bottom && lat<top && lon>left && lon<right){
					postfilter.add( aa );
				}
			}
			return postfilter;
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
				List<Address> addresses = geocodeWithinArea(locString, MAX_GEOCODED_LOCATIONS, 42.184267, -71.249771, 42.449301, 70.888595);
			
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
		
		private LocationPicker locPicker;
		
		GeocodeResponseHandler(LocationPicker locPicker){
			super();
			
			this.locPicker = locPicker;
		}
		
		@SuppressWarnings("unchecked")
		@Override
	    public void handleMessage(Message msg) {	    	
	    	
	    	if( msg.arg1 == GEOCODE_FAIL ){
	    		locPicker.setErrorState();
				if( locPicker.retryCount < MAX_RETRY ){
					locPicker.retryCount += 1;
					locPicker.dropdown_header_text.setText("Server error. Retrying "+locPicker.retryCount+"/"+MAX_RETRY+"...");
					locPicker.geocode();
				} else {
					locPicker.working.setVisibility(View.GONE);
					locPicker.dropdown_header_working.setVisibility(View.GONE);
					locPicker.dropdown_header_text.setText("Server error. Couldn't cope.");
				}
	    	} else {
	    		locPicker.working.setVisibility(View.GONE);
		    	List<Address> geocodeResults = (List<Address>)msg.obj;
		    	locPicker.clearErrorState();
		    	
		    	if( geocodeResults.size()<1 ){
		    		locPicker.dropdown_header_text.setText("no results found");
		    		locPicker.dropdown_header_working.setVisibility(View.GONE);
		    		locPicker.setErrorState();
		    	} else {
		    		locPicker.dropdown.removeHeaderView( locPicker.dropdown_header_view );
			    	HeaderViewListAdapter dropdownContents = (HeaderViewListAdapter) locPicker.dropdown.getAdapter();
			    	((AddressAdapter)dropdownContents.getWrappedAdapter()).setAddresses( geocodeResults );
		    	}
	    	}
		    	
	    	locPicker.showDropdown();
			
			
	    }
	}
	
	class LocationPicker {
		public int retryCount;
		protected static final int MIN_GEOCODE_STRING_SIZE = 2;
		MainActivity.TerminusManager terminus;
		ProgressBar working;
		ListView dropdown;
		EditText text;
		ImageButton button;
		RelativeLayout lozenge;
		TextView lozenge_text;
		ImageButton lozenge_button;
		LinearLayout dropdown_header_view;
		TextView dropdown_header_text;
		ProgressBar dropdown_header_working;
		GeocodeResponseHandler hh;
		
		Drawable nonerror_text_background;
		
		LocationPicker( final MainActivity.TerminusManager terminus, final ProgressBar working, final ListView dropdown, EditText text, ImageButton button, RelativeLayout lozenge ){
			this.retryCount = 0;
			
			this.terminus = terminus;
			this.working = working;
			this.dropdown = dropdown;
			this.lozenge = lozenge;
			this.lozenge_text = (TextView)lozenge.findViewById(R.id.lozenge_text);
			this.lozenge_button = (ImageButton)lozenge.findViewById(R.id.lozenge_button);
			
			this.text = text;
			this.button = button;
			
			this.dropdown_header_view = (LinearLayout) getActivity().getLayoutInflater().inflate( R.layout.locpicker_header, this.dropdown, false );
			this.dropdown_header_text = (TextView) dropdown_header_view.findViewById(R.id.header_text);
			this.dropdown_header_working = (ProgressBar) dropdown_header_view.findViewById(R.id.header_working);
			this.dropdown.addHeaderView( this.dropdown_header_view, null, false );
			this.dropdown.setAdapter( new AddressAdapter() );
			
			this.hh = new GeocodeResponseHandler(this);
			
			nonerror_text_background = this.text.getBackground();
			
			if( terminus.type == MainActivity.TerminusManager.BLANK){
				clear();
			} else if( terminus.type == MainActivity.TerminusManager.MAP ){
				setFromMap( terminus.pt );
			}
			
			this.dropdown.setOnItemClickListener( new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position,
						long id) {
					HeaderViewListAdapter headerlist = (HeaderViewListAdapter)parent.getAdapter();
					AddressAdapter data = (AddressAdapter) headerlist.getWrappedAdapter();
					
					// The index of the item in the wrapped list is the index of the item in the
					// ListView minus the number of headers
					Address selected = data.getItem(position-headerlist.getHeadersCount());
					
					terminus.setFromAddress( selected );
				}
				
			});
			
			this.button.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					terminus.setFromMyLocation();
				}

			});
			
			this.lozenge_button.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if(terminus.type != MainActivity.TerminusManager.BLANK){
						terminus.clear();
					} 
				}

			});

			this.text.addTextChangedListener( new TextWatcher() {

				@Override
				public void afterTextChanged(Editable arg0) {

				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1,
						int arg2, int arg3) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before,
						int count) {
					
					clearErrorState();

					// only look up address when the field is in a blank state
					if( terminus.type != MainActivity.TerminusManager.BLANK ){
						return;
					}

					// If the string is too short, cancel geocoding
					if( s.length()<MIN_GEOCODE_STRING_SIZE ){
						cancelGeocoding();
						return;
					}

					// Set the working spinner, and start a geocode task
					dropdown_header_working.setVisibility(View.VISIBLE);
					showDropdownHeader();
					dropdown_header_text.setText("Locating address...");
					clearDropdown();
					showDropdown();
					
					retryCount=0;
					geocode();

				}

			});
			
			this.text.setOnFocusChangeListener(new OnFocusChangeListener(){

				@Override
				public void onFocusChange(View arg0, boolean hasFocus) {
					if( !hasFocus ){
						hideDropdown();
					}
				}
				
			});
		}
		
		public void setErrorState() {
			text.setBackgroundColor( Color.rgb(255,200,200) );
		}
		
		@SuppressLint("NewApi")
		public void clearErrorState() {
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
				text.setBackground(nonerror_text_background);
			} else {
				text.setBackgroundColor( Color.WHITE );
			}
		}

		public void geocode() {
			// If a geocode task is underway, cancel it
			if( getAddressTask != null ){
				getAddressTask.cancel(true);
			}
			getAddressTask = new GetAddressTask(hh, geocoder);
			getAddressTask.execute( text.getText().toString() );
		}

		protected void showDropdownHeader() {
			if(dropdown.getHeaderViewsCount()==0){
				dropdown.addHeaderView( dropdown_header_view, null, false );
			}
		}

		public void showDropdown() {
			dropdown.setVisibility(View.VISIBLE);
			dropdown.bringToFront();
		}
		
		private void hideDropdown() {
			dropdown.setVisibility(View.GONE);
		}
		
		private void clearDropdown() {
			((AddressAdapter)((HeaderViewListAdapter)dropdown.getAdapter()).getWrappedAdapter()).clear();
		}

		void clear(){
			text.setText("");
			text.setEnabled(true);
			lozenge_text.setText("");
			lozenge.setVisibility(View.GONE);
			text.requestFocus();
		}
		
		void setFromMyLocation() {
			working.setVisibility(View.GONE);
			hideDropdown();
			text.setEnabled(false);
			lozenge_text.setText("My location");
			lozenge.setVisibility(View.VISIBLE);
		}
		
		void setFromMap(GeoPoint pt) {
			working.setVisibility(View.GONE);
			hideDropdown();
			text.setEnabled(false);
			lozenge_text.setText("Selected from map");
			lozenge.setVisibility(View.VISIBLE);
		}

		void setFromAddress( Address address ) {
			working.setVisibility(View.GONE);
			hideDropdown();
			text.setEnabled(false);
			lozenge_text.setText( address.getAddressLine(0) );
			lozenge.setVisibility(View.VISIBLE);
		}

		private void cancelGeocoding() {
			working.setVisibility(View.GONE);
			hideDropdown();
			if( getAddressTask != null ) {
				getAddressTask.cancel(true);
			}
		}
	}

	Geocoder geocoder;
	static GeocodeResponseHandler geocodeResponseHandler;
	GetAddressTask getAddressTask = null;

	LocationPicker orig;
	LocationPicker dest;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate( savedInstanceState );
		
		geocoder = new Geocoder(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		final MainActivity activity = (MainActivity)this.getActivity();

		final View fragView = inflater.inflate(R.layout.control_fragment, container, false);
		
		// Build origin tri-picker
		ProgressBar o_working = (ProgressBar)fragView.findViewById(R.id.orig_working);
		EditText o_text = (EditText)fragView.findViewById(R.id.orig_text);
		ImageButton o_button = (ImageButton)fragView.findViewById(R.id.orig_button);
		ListView o_dropdown = (ListView)fragView.findViewById( R.id.orig_dropdown );
		RelativeLayout o_lozenge = (RelativeLayout)fragView.findViewById( R.id.orig_lozenge );
		orig = new LocationPicker( activity.orig, o_working, o_dropdown, o_text, o_button, o_lozenge );
		
		// Build destination tri-picker
		ProgressBar d_working = (ProgressBar)fragView.findViewById(R.id.dest_working);
		EditText d_text = (EditText)fragView.findViewById(R.id.dest_text);
		ImageButton d_button = (ImageButton)fragView.findViewById(R.id.dest_button);
		ListView d_dropdown = (ListView)fragView.findViewById( R.id.dest_dropdown );
		RelativeLayout d_lozenge = (RelativeLayout)fragView.findViewById( R.id.dest_lozenge );
		dest = new LocationPicker( activity.dest, d_working, d_dropdown, d_text, d_button, d_lozenge );
				
		// "use transit" checkbox
        CheckBox use_transit = (CheckBox)fragView.findViewById(R.id.checkbox_usetransit);
        use_transit.setChecked( activity.useTransit );
        
        // set up speed spinner
        Spinner spinner = (Spinner) fragView.findViewById(R.id.speed_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(fragView.getContext(),
             R.array.speeds_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        // Go! button
        Button go_button = (Button)fragView.findViewById(R.id.go_button);
        go_button.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				activity.findAndDisplayRoute();
			}
        	
        });
		
		return fragView;
	}

	public void startGetRoute() {
		((Button)getView().findViewById(R.id.go_button)).setText("...");
	}

	public void finishGetRoute() {
		((Button)getView().findViewById(R.id.go_button)).setText("go!");
	}
	






}