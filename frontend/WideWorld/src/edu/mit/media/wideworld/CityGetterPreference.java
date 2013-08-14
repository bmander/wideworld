package edu.mit.media.wideworld;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class CityGetterPreference extends DialogPreference{
	
	class GetCitiesTask extends AsyncTask<Void,Void,Void> {
		
		private static final int GETCITY_SUCCESS = 0;
		private static final int GETCITY_FAIL = 1;
		private Handler handler;
		
		GetCitiesTask(Handler handler){
			this.handler = handler;
		}
		

		
		String getInstancesJSON( ) throws IOException {
			
			
			// there's a lot that can go wrong here...
			try {

				// create http client
				HttpClient hc = new DefaultHttpClient();
				HttpResponse response;
				
				// create uri for terminus points
				URI uri = new URI( "http://wideworld.media.mit.edu/instances" );
				
				// grab response
				response = hc.execute( new HttpGet( uri ));
				StatusLine statusLine = response.getStatusLine();
				
				// if the response is OK
			    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
			    	// piece together the response string
			        ByteArrayOutputStream out = new ByteArrayOutputStream();
			        response.getEntity().writeTo(out);
			        out.close();
			        String responseString = out.toString();
			        return responseString;
			        
			    } else{
			        //Closes the connection.
			        response.getEntity().getContent().close();
			        throw new IOException(statusLine.getReasonPhrase());
			    }
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} 
			
			return null;
			
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				String instancesJSON = this.getInstancesJSON();
	
				if( instancesJSON == null ){
					Message msg = new Message();
					msg.arg1 = GETCITY_FAIL;
					handler.sendMessage(msg);
				} else {
					Message msg = new Message();
					msg.arg1 = GETCITY_SUCCESS;
					msg.obj = instancesJSON;
					handler.sendMessage(msg);
				}
			} catch (IOException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.arg1 = GETCITY_FAIL;
				handler.sendMessage(msg);
			}
			
			return null;
		}
		
	}


	
	static class GetCitiesHandler extends Handler {
		
		private CityGetterPreference cgprefs;
		
		GetCitiesHandler(CityGetterPreference cgprefs){
			super();

			this.cgprefs = cgprefs;
			
		}
		
		@Override
	    public void handleMessage(Message msg) {	    	
	    	
	    	if( msg.arg1 == GetCitiesTask.GETCITY_FAIL ){

	    	} else {	    		

	    		String citiesJSON = (String)msg.obj;
	    		List<CityInstance> cities = CitiesFile.getInstances(citiesJSON);
	    		
	    		if( cities!=null ){
	    			cgprefs.showCitiesList(cities);
	    		}
	    			
	    		CitiesFile.saveInstancesJSON(cgprefs.getContext(), citiesJSON);
				
	    	}
		    	
	    	cgprefs.progressbar.setVisibility(View.INVISIBLE);
			
	    }


	}
	
    private Button button;
	private ProgressBar progressbar;
	private RadioGroup citylist;
	public List<CityInstance> cities;
	String clickedPrefix = null;

	public CityGetterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setDialogLayoutResource(R.layout.citygetter_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
        
		String citiesJSON = CitiesFile.getInstancesJSON(getContext());
		if( citiesJSON!=null ){
			cities = CitiesFile.getInstances(citiesJSON);
		}
    }
	
	public void showCitiesList(List<CityInstance> cities){
		citylist.removeAllViews();
		
		for(int i=0; i<cities.size(); i++){
			RadioButton cityView = new RadioButton(getContext());
			cityView.setLayoutParams( new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT) );
			cityView.setText(cities.get(i).city_name);
			cityView.setTag(cities.get(i).prefix);
			cityView.setPadding(5, 10, 10, 5);
			cityView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					String prefix = (String)v.getTag();
					clickedPrefix = prefix;
				}
				
			});
			citylist.addView(cityView);
			
			if(cities.get(i).prefix.equals(clickedPrefix)){
				citylist.check(cityView.getId());
			}
		}
		
		this.cities = cities;
	}
	
	@Override
	protected void onBindDialogView(View view) {

		this.button = (Button) view.findViewById(R.id.updatebutton);
		this.progressbar = (ProgressBar) view.findViewById(R.id.progress);
		this.citylist = (RadioGroup) view.findViewById(R.id.citylist);
		
		this.progressbar.setVisibility(View.INVISIBLE);
        
		final CityGetterPreference cgprefs = this;
		this.button.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				progressbar.setVisibility(View.VISIBLE);
				new GetCitiesTask(new GetCitiesHandler(cgprefs)).execute();
			}

		});
		
		if( cities!=null ){
			this.showCitiesList(cities);
		}

	    super.onBindDialogView(view);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {

	    if(!positiveResult){
	    	// If 'bos' is the city preference, someone clicks on the city-picker, selects 'nyc',
	    	// and then hits cancel, if they immediately re-open the city-picker it should show 'bos',
	    	// not 'nyc'.
	    	clickedPrefix = this.getPersistedString(null);
	        return;
	    }

	    this.persistString(clickedPrefix);
	    
	    setSummaryToCityName();

	    super.onDialogClosed(positiveResult);
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	    if (restorePersistedValue) {
	        clickedPrefix = this.getPersistedString(null);
	        
	        setSummaryToCityName();
	    } else {
	        clickedPrefix = null;
	    }
	    
	    
	}

	private void setSummaryToCityName() {
		// sift through json-stored cities for city name
		if( clickedPrefix!=null & cities!=null ){
			for(int i=0; i<cities.size(); i++){
				CityInstance city = cities.get(i);
				if(city.prefix.equals(clickedPrefix)){
					this.setSummary(city.city_name);
					break;
				}
			}
		}
	}

}
