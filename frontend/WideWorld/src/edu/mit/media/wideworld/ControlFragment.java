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
		
		private ListView dropdown;
		private ProgressBar working;
		
		GeocodeResponseHandler(ListView orig_dropdown, ProgressBar working){
			super();
			
			this.dropdown = orig_dropdown;
			this.working = working;
		}
		
		@Override
	    public void handleMessage(Message msg) {
	    	Log.v("DEBUG", "message caught");
	    	
	    	working.setVisibility(View.GONE);
	    	
	    	if( msg.arg1 == GEOCODE_FAIL ){
				dropdown.setVisibility(View.GONE);
	    	} else {
		    	List<Address> geocodeResults = (List<Address>)msg.obj;
		    	
		    	AddressAdapter adapt = new AddressAdapter( geocodeResults );
		    	dropdown.setAdapter( adapt );
	    	}
		    	
			dropdown.setVisibility(View.VISIBLE);
			
	    }
	}
	
	Geocoder geocoder;
	static GeocodeResponseHandler geocodeResponseHandler;
	GetAddressTask getAddressTask = null;
	
	ProgressBar orig_working;
	ListView orig_dropdown;
	EditText orig_text;
	Button orig_button;
	
	ProgressBar dest_working;
	ListView dest_dropdown;
	EditText dest_text;
	Button dest_button;
	
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
		
		orig_working = (ProgressBar)fragView.findViewById(R.id.orig_working);
		dest_working = (ProgressBar)fragView.findViewById(R.id.dest_working);
		orig_text = (EditText)fragView.findViewById(R.id.orig_text);
		orig_button = (Button)fragView.findViewById(R.id.orig_button);
		
		orig_dropdown = (ListView)fragView.findViewById( R.id.orig_dropdown );
		orig_dropdown.setOnItemClickListener( new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				activity.orig_state = MainActivity.TERMINUS_ADDRESS;
				AddressAdapter data = (AddressAdapter)parent.getAdapter();
				Address selected = data.getItem(position);
				
				activity.setOriginFromAddress( selected );
			}
			
		});
		
		if( activity.orig_state == MainActivity.TERMINUS_BLANK){
			clearOriginUI();
		} else if( activity.orig_state == MainActivity.TERMINUS_MAP ){
			setOriginFromMapUI( activity.getOrigin() );
		}
		
		orig_button.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(activity.orig_state == MainActivity.TERMINUS_BLANK){
					((MainActivity)getActivity()).setOriginFromMyLocation();
				} else {
					((MainActivity)getActivity()).clearOrigin();
				} 
			}
			
		});
		
		geocodeResponseHandler = new GeocodeResponseHandler(orig_dropdown, orig_working);
		
		geocoder = new Geocoder(fragView.getContext());

        CheckBox use_transit = (CheckBox)fragView.findViewById(R.id.checkbox_usetransit);
        use_transit.setChecked( activity.useTransit );
        
        // set up speed spinner
        Spinner spinner = (Spinner) fragView.findViewById(R.id.speed_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(fragView.getContext(),
             R.array.speeds_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

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
				
				if( activity.orig_state != MainActivity.TERMINUS_BLANK ){
					return;
				}
								
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

			}
        	
        });
        
        Button go_button = (Button)fragView.findViewById(R.id.go_button);
        go_button.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				activity.findAndDisplayRoute();
			}
        	
        });
		
		return fragView;
	}
	
	protected void clearOriginUI(){
		orig_button.setText("GPS");
		orig_text.setEnabled(true);
		orig_text.setText("");
	}

	protected void setOriginFromMyLocationUI() {
		orig_button.setText("X");
		orig_text.setEnabled(false);
		orig_text.setText("My Location");
	}

	public void setOriginFromMapUI(GeoPoint pt) {
		Log.v("DEBUG", "updating origin UI");
		orig_working.setVisibility(View.GONE);
		orig_dropdown.setVisibility(View.GONE);
		orig_button.setText("X");
		orig_text.setText("FROM MAP");
		orig_text.setEnabled(false);
	}
	
	public void setOriginFromAddressUI( Address address ) {
		orig_text.setEnabled(false);
		orig_text.setText( address.getAddressLine(0) );
		orig_dropdown.setVisibility(View.GONE);
		orig_button.setText("X");
	}

	public void setDestinationFromMap(GeoPoint pt) {
		Log.v("DEBUG", "updating destination UI");
//		dest_working.setVisibility(View.GONE);
//		dest_dropdown.setVisibility(View.GONE);
//		dest_button.setText("X");
//		dest_text.setText("FROM MAP");
//		dest_text.setEnabled(false);
	}


	


}