package edu.mit.media.wideworld;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import edu.mit.media.wideworld.ControlFragment.LocationPicker;
import edu.mit.media.wideworld.RouteServer.Response;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class CityGetterPreference extends DialogPreference{
	
	class HandlerPayload{
		String instancesJSON;
		List<CityInstance> instances;
		
		public HandlerPayload(String instancesJSON,
				List<CityInstance> instances) {
			this.instancesJSON = instancesJSON;
			this.instances = instances;
		}

	}
	
	class GetCitiesTask extends AsyncTask<Void,Void,Void> {
		
		private static final int GETCITY_SUCCESS = 0;
		private static final int GETCITY_FAIL = 1;
		private Handler handler;
		
		GetCitiesTask(Handler handler){
			this.handler = handler;
		}
		
		List<CityInstance> getInstances(String instanceJSON) throws JSONException{
			List<CityInstance> ret = new ArrayList<CityInstance>();
	        Object nextValue = new JSONTokener(instanceJSON).nextValue();
	        
	        Log.v("DEBUG", "next value is "+nextValue.toString() );
	        if( nextValue.getClass() == String.class ){
	        	return null;
	        } else if( nextValue.getClass() == JSONArray.class ) {
	        	JSONArray jsonInstances = (JSONArray)nextValue;
	        	for(int i=0; i<jsonInstances.length(); i++){
	        		JSONObject jsonObj = jsonInstances.getJSONObject(i);
	        		CityInstance inst = CityInstance.fromJSON( jsonObj );
	        		ret.add( inst );
	        	}
		        return ret;
	        } else {
	        	return null;
	        }
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			return null;
			
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				String instancesJSON = this.getInstancesJSON();
				
				List<CityInstance> instances;
				try {
					if(instancesJSON!=null){
						instances = this.getInstances(instancesJSON);
					} else {
						instances = null;
					}
				} catch (JSONException e) {
					instances = null;
				}
	
				if( instances == null ){
					Message msg = new Message();
					msg.arg1 = GETCITY_FAIL;
					handler.sendMessage(msg);
				} else {
					Message msg = new Message();
					msg.arg1 = GETCITY_SUCCESS;
					msg.obj = new HandlerPayload(instancesJSON, instances);
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
	    		HandlerPayload obj = (HandlerPayload)msg.obj;
	    		cgprefs.showCitiesList(obj.instances);
	    		
				cgprefs.saveInstancesJSON(obj.instancesJSON);

	    	}
		    	
	    	cgprefs.progressbar.setVisibility(View.INVISIBLE);
			
	    }


	}
	
    private Button button;
	private ProgressBar progressbar;
	private RadioGroup citylist;
	public List<CityInstance> cities;

	public CityGetterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setDialogLayoutResource(R.layout.citygetter_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
        
        String instancesJSON = getInstancesJSON(context);
        
    }
	
	public void showCitiesList(List<CityInstance> cities){
		citylist.removeAllViews();
		
		for(int i=0; i<cities.size(); i++){
			RadioButton cityView = new RadioButton(getContext());
			cityView.setLayoutParams( new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT) );
			cityView.setText(cities.get(i).city_name);
			cityView.setTag(cities.get(i).prefix);
			cityView.setPadding(5, 10, 10, 5);
			citylist.addView(cityView);
		}
		
		this.cities = cities;
	}

	private String getInstancesJSON(Context context) {
		try {
			FileInputStream instancesFile = context.openFileInput("instances.json");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			while(true){
				int bb = instancesFile.read();
				if(bb==-1){
					break;
				}
				bos.write(bb);
			}
			String instancesJSON = bos.toString();
			return instancesJSON;
		} catch (FileNotFoundException e) {
			// normal occurrence when instances.json has not been written before
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private void saveInstancesJSON(String instancesJSON) {
		try {
			FileOutputStream fos;
			fos = getContext().openFileOutput("instances.json", Context.MODE_PRIVATE);
			fos.write(instancesJSON.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
				Log.v("DEBUG", "get more stuffage");
				progressbar.setVisibility(View.VISIBLE);
				new GetCitiesTask(new GetCitiesHandler(cgprefs)).execute();
			}

		});

	    super.onBindDialogView(view);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {

	    if(!positiveResult)
	        return;

	    // stuff to do when th edialog closes

	    super.onDialogClosed(positiveResult);
	}

}
