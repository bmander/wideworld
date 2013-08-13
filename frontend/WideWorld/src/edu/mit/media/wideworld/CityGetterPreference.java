package edu.mit.media.wideworld;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
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
	
	class GetCitiesTask extends AsyncTask<Void,Void,Void> {
		
		private static final int GETCITY_SUCCESS = 0;
		private static final int GETCITY_FAIL = 1;
		private Handler handler;
		
		GetCitiesTask(Handler handler){
			this.handler = handler;
		}
		
		List<CityInstance> getInstances( ) throws IOException {
			List<CityInstance> ret = new ArrayList<CityInstance>();
			
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
			        
			        Object nextValue = new JSONTokener(responseString).nextValue();
			        
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
			        }
			        
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
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return null;
			
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				List<CityInstance> resp = this.getInstances();
	
				if( resp == null ){
					Message msg = new Message();
					msg.arg1 = GETCITY_FAIL;
					handler.sendMessage(msg);
				} else {
					Message msg = new Message();
					msg.arg1 = GETCITY_SUCCESS;
					msg.obj = resp;
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
		
		private ProgressBar pb;
		private RadioGroup citylist;
		private Context context;
		
		GetCitiesHandler(ProgressBar pb, RadioGroup citylist, Context context){
			super();

			this.pb = pb;
			this.citylist = citylist;
			this.context = context;
			
		}
		
		@Override
	    public void handleMessage(Message msg) {	    	
	    	
	    	if( msg.arg1 == GetCitiesTask.GETCITY_FAIL ){

	    	} else {
	    		citylist.removeAllViews();
	    		
	    		@SuppressWarnings("unchecked")
				List<CityInstance> cities = (List<CityInstance>) msg.obj;
	    		for(int i=0; i<cities.size(); i++){
	    			Log.v("DEBUG", "city: "+cities.get(i));
	    			
	    			RadioButton cityView = new RadioButton(context);
	    			cityView.setLayoutParams( new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT) );
	    			cityView.setText(cities.get(i).city_name);
	    			cityView.setTag(cities.get(i).prefix);
	    			cityView.setPadding(5, 10, 10, 5);
	    			citylist.addView(cityView);
	    		}
	    	}
		    	
	    	pb.setVisibility(View.INVISIBLE);
			
	    }
	}
	
    private Button button;
	private ProgressBar progressbar;
	private RadioGroup citylist;

	public CityGetterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setDialogLayoutResource(R.layout.citygetter_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
        
    }
	
	@Override
	protected void onBindDialogView(View view) {

		this.button = (Button) view.findViewById(R.id.updatebutton);
		this.progressbar = (ProgressBar) view.findViewById(R.id.progress);
		this.citylist = (RadioGroup) view.findViewById(R.id.citylist);
		
		this.progressbar.setVisibility(View.INVISIBLE);
        
		this.button.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.v("DEBUG", "get more stuffage");
				progressbar.setVisibility(View.VISIBLE);
				new GetCitiesTask(new GetCitiesHandler(progressbar,citylist,getContext())).execute();
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
