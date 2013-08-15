package edu.mit.media.wideworld;

import java.util.List;

import edu.mit.media.wideworld.CityGetterPreference.GetCitiesHandler;

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
	    		// in the case of failure, enable a 'retry' button
	    	} else {	    		

	    		String citiesJSON = (String)msg.obj;
	    		List<CityInstance> cities = CitiesFile.getInstances(citiesJSON);
	    		
	    		if( cities!=null ){
	    			ctx.showCitiesList(cities);
	    		}
	    			
	    		CitiesFile.saveInstancesJSON(ctx, citiesJSON);
				
	    	}
		    	
	    	ctx.progress.setVisibility(View.INVISIBLE);
			
	    }


	}
	
	View pitch;
	View consentForm;
	View cityPicker;
	
	LinearLayout cityList;
	ProgressBar progress;
	

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
				
				// todo move this to start when setup starts
				new GetCitiesTask(new GetCitiesHandler(SetupActivity.this)).execute();
			}	
		});
		
		cityList = (LinearLayout) cityPicker.findViewById(R.id.citylist);
		progress = (ProgressBar) cityPicker.findViewById(R.id.progressbar);
		
		setContentView( pitch );
		
		

		setFinishOnTouchOutside(false);
		
    }


	public void showCitiesList(List<CityInstance> cities) {
		Log.v("DEBUG", "write list of "+cities.size()+" cities");
		
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
					String prefix = (String)v.getTag();
					
					// set preference
					Log.v("DEBUG", "preferred city is "+prefix);
				}
				
			});
			cityList.addView(cityView);
			
//			if(cities.get(i).prefix.equals(clickedPrefix)){
//				citylist.check(cityView.getId());
//			}
		}
		
//		this.cities = cities;
	}
    
}