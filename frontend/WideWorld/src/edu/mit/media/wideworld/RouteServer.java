package edu.mit.media.wideworld;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.osmdroid.util.GeoPoint;

import android.os.AsyncTask;
import android.util.Log;

public class RouteServer {
	String host;
	String instance;
	
	public class Request{
		public double lat1;
		public double lng1;
		public double lat2;
		public double lng2;
		public boolean useTransit;
		public double bikeSpeed;
		
		Request(double lat12, double lng12, double lat22, double lng22, boolean useTransit, double bikeSpeed){
			this.lat1=lat12;
			this.lng1=lng12;
			this.lat2=lat22;
			this.lng2=lng22;
			this.useTransit = useTransit;
			this.bikeSpeed = bikeSpeed;
		}
		
		Request(double lat12, double lng12, double lat22, double lng22){
			this(lat12,lng12,lat22,lng22,true,2.0);
		}
	}
	
	public class Response{
		class Leg {
			static final int TYPE_NONE = 0;
			static final int TYPE_WALK = 1;
			static final int TYPE_TRANSIT = 2;
			static final int MODE_NONE = 0;
			static final int MODE_WALK = 1;
			static final int MODE_BIKESHARE = 2;
			
			int type=TYPE_NONE;
			int mode=MODE_NONE;
			List<Location> locs;

			public Leg(JSONObject jsonLeg) throws JSONException {
				locs = new ArrayList<Location>();
				
				String jsonType = jsonLeg.getString("type");
				if( jsonType.equals("walk") ){
					type = TYPE_WALK;
				} else if( jsonType.equals("transit") ){
					type = TYPE_TRANSIT;
				}
				
				if(type==TYPE_WALK){
					String jsonMode = jsonLeg.getString("mode");
					if( jsonMode.equals("walk") ){
						mode = MODE_WALK;
					} else if( jsonMode.equals("bikeshare") ){
						mode = MODE_BIKESHARE;
					}
				}
				
				JSONArray leg_locs = jsonLeg.getJSONArray("locs");
				for(int i=0; i<leg_locs.length(); i++){
					JSONObject jsonLoc = leg_locs.getJSONObject(i);
					Location loc = new Location(jsonLoc);
					locs.add( loc );
				}	
			}

			public int getLocationCount() {
				return this.locs.size();
			}

			public Location getLocation(int i) {
				return this.locs.get(i);
			}
			
		}
		
		class Location{
			double lat;
			double lon;
			long time;
			
			public Location(JSONObject jsonLoc) throws JSONException {
				lon = jsonLoc.getDouble("lon");
				lat = jsonLoc.getDouble("lat");
				time = jsonLoc.getInt("time");
			}
			
		}
		
		List<Leg> legs;
		
		public Response(JSONObject object) {
			legs = new ArrayList<Leg>();
			
			try {
				JSONArray plan = object.getJSONArray("plan");
				for(int i=0; i<plan.length(); i++){
					JSONObject jsonLeg = plan.getJSONObject(i);
					legs.add( new Leg(jsonLeg) );
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		public long duration() {
			return this.lastLoc().time - this.firstLoc().time;
		}

		private Location firstLoc() {
			if( this.getLegCount()<1 ){
				return null;
			}
			
			Leg firstLeg = this.legs.get(0);
			return firstLeg.getLocation(0);
		}

		private Location lastLoc() {
			if( this.getLegCount()<1 ){
				return null;
			}
			
			Leg lastLeg = this.legs.get(this.legs.size()-1);
			return lastLeg.getLocation(lastLeg.locs.size()-1);
		}

		public int getLegCount() {
			return this.legs.size();
		}

		public Leg getLeg(int i) {
			return this.legs.get(i);
		}
	}
	
	public interface FetchRouteCallback {
		void onResponse(Response resp);
	}
	
	RouteServer( String host, String instance ){
		this.host = host;
		this.instance = instance;
	}
	
	void getRoute( Request request, final FetchRouteCallback callback ){
		class FetchRouteTask extends AsyncTask<String, Void, Response> {
			
			@Override
			protected Response doInBackground(String... url) {
				
				// there's a lot that can go wrong here...
				try {

					// create http client
					HttpClient hc = new DefaultHttpClient();
					HttpResponse response;
					
					// create uri for terminus points
					URI uri = new URI(url[0]);
					
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
				        
				        JSONObject object = (JSONObject) new JSONTokener(responseString).nextValue();
				        Response resp = new Response(object);
				        
				        return resp;
				        
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
			protected void onPostExecute(Response resp){
				callback.onResponse( resp );
			}
			
		}
		String url = "http://"+this.host+"/"+this.instance+"/plan?lat1="+request.lat1+"&lon1="+request.lng1+"&lat2="+request.lat2+"&lon2="+request.lng2+"&bspeed="+request.bikeSpeed+"&transit="+(request.useTransit?"t":"f");
		Log.v("DEBUG", url);
		FetchRouteTask rt = new FetchRouteTask();
		rt.execute(url);
	}
}
