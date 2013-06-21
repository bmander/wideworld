package edu.mit.media.wideworld;

import java.io.IOException;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ControlFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View fragView = inflater.inflate(R.layout.control_fragment, container, false);
		
        CheckBox use_transit = (CheckBox)fragView.findViewById(R.id.checkbox_usetransit);
        use_transit.setChecked(true);
        
        Spinner spinner = (Spinner) fragView.findViewById(R.id.speed_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(fragView.getContext(),
             R.array.speeds_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        
        // respond to Go Button click
        final Button button = (Button) fragView.findViewById(R.id.go_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//MainActivity getActivity();
            	
            	Geocoder geocoder = new Geocoder(v.getContext());
            	
            	/* if there's an address in the origin box, use it as start point */
            	EditText orig_text_field = (EditText)fragView.findViewById(R.id.orig_text);
            	String orig_text = orig_text_field.getText().toString();
            	
            	if( orig_text.length() > 0 ){
	            	try {
	            		//TODO use non-hardcoded bounding box
						List<Address> addresses = geocoder.getFromLocationName(orig_text, 1, 42.184267, -71.249771, 42.449301, 70.888595);
						
//						if( addresses.size() > 0 ){
//							
//						}
						
						for(int i=0; i<addresses.size(); i++){
							Address addy = addresses.get(i);
							Log.v("DEBUG", addy.toString());
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            	
            	Log.v("DEBUG", "orig text: "+orig_text.getText() );
            	
                Log.v("DEBUG", "CLICK YO");
            }
        });
		
		return fragView;
	}
	


}