package edu.mit.media.wideworld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.util.GeoPoint;

import android.annotation.TargetApi;
import android.app.Fragment;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;


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
	    	locPicker.working.setVisibility(View.GONE);
	    	
	    	if( msg.arg1 == GEOCODE_FAIL ){
				locPicker.text.setBackgroundColor( Color.RED );
				locPicker.dropdown_header.setText("something went wrong");
	    	} else {
		    	List<Address> geocodeResults = (List<Address>)msg.obj;
		    	
		    	if( geocodeResults.size()<1 ){
		    		locPicker.dropdown_header.setText("no results found");
		    		locPicker.text.setBackgroundColor( Color.RED );
		    	} else {
		    		locPicker.dropdown.removeHeaderView( locPicker.dropdown_header );
			    	HeaderViewListAdapter dropdownContents = (HeaderViewListAdapter) locPicker.dropdown.getAdapter();
			    	((AddressAdapter)dropdownContents.getWrappedAdapter()).setAddresses( geocodeResults );
		    	}
	    	}
		    	
	    	locPicker.showDropdown();
			
			
	    }
	}
	
	class LocationPicker {
		protected static final int MIN_GEOCODE_STRING_SIZE = 2;
		MainActivity.TerminusManager terminus;
		ProgressBar working;
		ListView dropdown;
		EditText text;
		Button button;
		TextView dropdown_header;
		
		LocationPicker( final MainActivity.TerminusManager terminus, final ProgressBar working, final ListView dropdown, EditText text, Button button ){
			this.terminus = terminus;
			this.working = working;
			this.dropdown = dropdown;
			
			this.text = text;
			this.button = button;
			
			this.dropdown_header = (TextView) getActivity().getLayoutInflater().inflate( R.layout.locpicker_item, this.dropdown, false );
			this.dropdown.addHeaderView( this.dropdown_header, null, false );
			this.dropdown.setAdapter( new AddressAdapter() );
			
			//this.dropdown_header = final View fragView = inflater.inflate(R.layout.control_fragment, container, false);

			
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
					if(terminus.type == MainActivity.TerminusManager.BLANK){
						terminus.setFromMyLocation();
					} else {
						terminus.clear();
					} 
				}

			});

			final LocationPicker superthis = this;
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

					// Check we want to look up address
					if( terminus.type != MainActivity.TerminusManager.BLANK ){
						return;
					}

					// If the string is too short, cancel geocoding
					if( s.length()<MIN_GEOCODE_STRING_SIZE ){
						cancelGeocoding();
						return;
					}

					// If a geocode task is underway, cancel it
					if( getAddressTask != null ){
						getAddressTask.cancel(true);
					}

					// Set the working spinner, and start a geocode task
					working.setVisibility(View.VISIBLE);
					showDropdownHeader();
					dropdown_header.setText("Locating address...");
					clearDropdown();
					showDropdown();
					
					GeocodeResponseHandler hh = new GeocodeResponseHandler(superthis);
					getAddressTask = new GetAddressTask(hh, geocoder);
					getAddressTask.execute( s.toString() );

				}

			});
		}
		
		protected void showDropdownHeader() {
			if(dropdown.getHeaderViewsCount()==0){
				dropdown.addHeaderView( dropdown_header, null, false );
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
			button.setText("GPS");
			text.setEnabled(true);
			text.setText("");
		}
		
		void setFromMyLocation() {
			button.setText("X");
			text.setEnabled(false);
			text.setText("My Location");
		}
		
		void setFromMap(GeoPoint pt) {
			working.setVisibility(View.GONE);
			hideDropdown();
			button.setText("X");
			text.setText("FROM MAP");
			text.setEnabled(false);
		}

		void setFromAddress( Address address ) {
			text.setEnabled(false);
			text.setText( address.getAddressLine(0) );
			hideDropdown();
			button.setText("X");
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
		Button o_button = (Button)fragView.findViewById(R.id.orig_button);
		ListView o_dropdown = (ListView)fragView.findViewById( R.id.orig_dropdown );
		orig = new LocationPicker( activity.orig, o_working, o_dropdown, o_text, o_button );
		
		// Build destination tri-picker
		ProgressBar d_working = (ProgressBar)fragView.findViewById(R.id.dest_working);
		EditText d_text = (EditText)fragView.findViewById(R.id.dest_text);
		Button d_button = (Button)fragView.findViewById(R.id.dest_button);
		ListView d_dropdown = (ListView)fragView.findViewById( R.id.dest_dropdown );
		dest = new LocationPicker( activity.dest, d_working, d_dropdown, d_text, d_button );
				
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