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
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
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
		
		private LocationPicker locPicker;
		
		GeocodeResponseHandler(LocationPicker locPicker){
			super();
			
			this.locPicker = locPicker;
		}
		
		@Override
	    public void handleMessage(Message msg) {	    	
	    	locPicker.working.setVisibility(View.GONE);
	    	
	    	if( msg.arg1 == GEOCODE_FAIL ){
				locPicker.dropdown.setVisibility(View.GONE);
	    	} else {
		    	List<Address> geocodeResults = (List<Address>)msg.obj;
		    	
		    	AddressAdapter adapt = new AddressAdapter( geocodeResults );
		    	locPicker.dropdown.setAdapter( adapt );
	    	}
		    	
			locPicker.dropdown.setVisibility(View.VISIBLE);
			
	    }
	}
	
	class LocationPicker {
		MainActivity root;
		ProgressBar working;
		ListView dropdown;
		EditText text;
		Button button;
		
		LocationPicker( final MainActivity root, final ProgressBar working, final ListView dropdown, EditText text, Button button ){
			this.root = root;
			this.working = working;
			this.dropdown = dropdown;
			this.text = text;
			this.button = button;
			
			if( root.orig_state == MainActivity.TERMINUS_BLANK){
				clear();
			} else if( root.orig_state == MainActivity.TERMINUS_MAP ){
				setFromMap( root.getOrigin() );
			}
			
			this.dropdown.setOnItemClickListener( new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position,
						long id) {
					root.orig_state = MainActivity.TERMINUS_ADDRESS;
					AddressAdapter data = (AddressAdapter)parent.getAdapter();
					Address selected = data.getItem(position);
					
					root.setOriginFromAddress( selected );
				}
				
			});
			
			this.button.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if(root.orig_state == MainActivity.TERMINUS_BLANK){
						((MainActivity)getActivity()).setOriginFromMyLocation();
					} else {
						((MainActivity)getActivity()).clearOrigin();
					} 
				}

			});

			this.text.addTextChangedListener( new TextWatcher() {

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

					if( root.orig_state != MainActivity.TERMINUS_BLANK ){
						return;
					}

					if( arg0.length()<2 ){
						working.setVisibility(View.GONE);
						dropdown.setVisibility(View.GONE);
						if( getAddressTask != null ) {
							getAddressTask.cancel(true);
						}
						return;
					}

					if( getAddressTask != null ){
						getAddressTask.cancel(true);
					}

					working.setVisibility(View.VISIBLE);
					getAddressTask = new GetAddressTask(geocodeResponseHandler, geocoder);
					getAddressTask.execute( arg0.toString() );

				}

			});
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
			dropdown.setVisibility(View.GONE);
			button.setText("X");
			text.setText("FROM MAP");
			text.setEnabled(false);
		}
		
		void setFromAddress( Address address ) {
			text.setEnabled(false);
			text.setText( address.getAddressLine(0) );
			dropdown.setVisibility(View.GONE);
			button.setText("X");
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
		Log.v("DEBUG", "create control" );
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Log.v("DEBUG", "create control view");

		final MainActivity activity = (MainActivity)this.getActivity();

		final View fragView = inflater.inflate(R.layout.control_fragment, container, false);
		
		ProgressBar o_working = (ProgressBar)fragView.findViewById(R.id.orig_working);
		EditText o_text = (EditText)fragView.findViewById(R.id.orig_text);
		Button o_button = (Button)fragView.findViewById(R.id.orig_button);
		ListView o_dropdown = (ListView)fragView.findViewById( R.id.orig_dropdown );
		orig = new LocationPicker( activity, o_working, o_dropdown, o_text, o_button );
		
		ProgressBar d_working = (ProgressBar)fragView.findViewById(R.id.dest_working);
		EditText d_text = (EditText)fragView.findViewById(R.id.dest_text);
		Button d_button = (Button)fragView.findViewById(R.id.dest_button);
		ListView d_dropdown = (ListView)fragView.findViewById( R.id.dest_dropdown );
		dest = new LocationPicker( activity, d_working, d_dropdown, d_text, d_button );
		
		geocodeResponseHandler = new GeocodeResponseHandler(orig);
		
		geocoder = new Geocoder(fragView.getContext());

        CheckBox use_transit = (CheckBox)fragView.findViewById(R.id.checkbox_usetransit);
        use_transit.setChecked( activity.useTransit );
        
        // set up speed spinner
        Spinner spinner = (Spinner) fragView.findViewById(R.id.speed_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(fragView.getContext(),
             R.array.speeds_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
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