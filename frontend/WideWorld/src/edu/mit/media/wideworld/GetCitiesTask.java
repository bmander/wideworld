package edu.mit.media.wideworld;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

class GetCitiesTask extends AsyncTask<Void,Void,Void> {
	
	static final int GETCITY_SUCCESS = 0;
	static final int GETCITY_FAIL = 1;
	
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
