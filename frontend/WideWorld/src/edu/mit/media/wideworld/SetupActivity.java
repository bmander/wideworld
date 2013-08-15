package edu.mit.media.wideworld;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

public class SetupActivity extends Activity {
	
	static class GetCitiesHandler extends Handler {
		
		private SetupActivity ctx;
		
		GetCitiesHandler(SetupActivity ctx){
			super();

			this.ctx = ctx;
			
		}
		
		@Override
	    public void handleMessage(Message msg) {
				    	
	    	if( msg.arg1 == GetCitiesTask.GETCITY_FAIL ){
	    		// city list fetch failed for some reason. most likely timed out because the phone isn't
	    		// connected to the internet
	    		ctx.showRetry();
	    	} else {	    		

	    		String citiesJSON = (String)msg.obj;
	    		List<CityInstance> cities = CitiesFile.getInstances(citiesJSON);
	    		
	    		if( cities!=null ){
	    			ctx.showCitiesList(cities);
	    			CitiesFile.saveInstancesJSON(ctx, citiesJSON);
	    		} else {
	    			// if the msg came back with a success type but the citiesJSON didn't parse
	    			// into a valid 'cities' list, most likely the server sent back something cruddy
	    			// in this case it doesn't make much sense to ask the user to hit 'retry' but if the
	    			// server isn't giving city instance information they're out of luck in any case
	    			ctx.showRetry();
	    		}
				
	    	}
		    	
	    	ctx.progress.setVisibility(View.INVISIBLE);
			
	    }


	}
	
	View pitch;
	View consentForm;
	View cityPicker;
	
	LinearLayout cityList;
	ProgressBar progress;
	Button doneButton;
	TextView cityPickerErrorMessage;
	
	String prefix=null;
	

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		LayoutInflater inflater = getLayoutInflater();
		pitch = inflater.inflate(R.layout.setup_activity, null);
		consentForm = inflater.inflate(R.layout.consent_form, null);
		cityPicker = inflater.inflate(R.layout.city_picker, null);
		
		Button pitchYes = (Button)pitch.findViewById(R.id.yesbutton);
		pitchYes.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setContentView( consentForm );
			}
		});
		
		Button pitchNo = (Button)pitch.findViewById(R.id.nobutton);
		pitchNo.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setContentView(cityPicker);
			}
		});
		
		Button consentDone = (Button)consentForm.findViewById(R.id.yesbutton);
		consentDone.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setContentView(cityPicker);
			}	
		});
		
		View pickerContent = cityPicker.findViewById(R.id.citypickercontent);
		pickerContent.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				startGetCitiesTask();
			}
			
		});
		
		cityList = (LinearLayout) cityPicker.findViewById(R.id.citylist);
		progress = (ProgressBar) cityPicker.findViewById(R.id.progressbar);
		doneButton = (Button) cityPicker.findViewById(R.id.donebutton);
		cityPickerErrorMessage = (TextView) cityPicker.findViewById(R.id.citypickererrormessage);
		
		setContentView( pitch );
		
		startGetCitiesTask();

		setFinishOnTouchOutside(false);
		
    }


	private void startGetCitiesTask() {
		cityPickerErrorMessage.setVisibility(View.GONE);
		progress.setVisibility(View.VISIBLE);
		new GetCitiesTask(new GetCitiesHandler(SetupActivity.this)).execute();		
	}


	public void showRetry() {
		cityPickerErrorMessage.setVisibility(View.VISIBLE);
	}


	public void showCitiesList(List<CityInstance> cities) {		
		cityList.removeAllViews();
		
		for(int i=0; i<cities.size(); i++){
			RadioButton cityView = new RadioButton(this);
			cityView.setLayoutParams( new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT) );
			cityView.setText(cities.get(i).city_name);
			cityView.setTag(cities.get(i).prefix);
			cityView.setPadding(5, 10, 10, 5);
			cityView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					prefix = (String)v.getTag();
					
					doneButton.setEnabled(true);
				}
				
			});
			cityList.addView(cityView);
			
		}
		
	}
    
}