package edu.mit.media.wideworld;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class CityInstance implements Parcelable{
	double[] bbox = new double[4];
	double[] center = new double[2];
	int default_zoom=0;
	
	String bikeshare_url=null;
	String bikeshare_name=null;
	
	String transit_url=null;
	String transit_name=null;
	
	String city_name=null;
	String prefix=null;
	
	String tile_server=null;
	String route_server=null;
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDoubleArray(bbox);
		dest.writeDoubleArray(center);
		dest.writeInt(default_zoom);
		dest.writeString(bikeshare_url);
		dest.writeString(bikeshare_name);
		dest.writeString(transit_url);
		dest.writeString(transit_name);
		dest.writeString(city_name);
		dest.writeString(prefix);
		dest.writeString(tile_server);
		dest.writeString(route_server);
	}
	
    public static final Parcelable.Creator<CityInstance> CREATOR
    = new Parcelable.Creator<CityInstance>() {
		public CityInstance createFromParcel(Parcel in) {
		    return new CityInstance(in);
		}
		
		public CityInstance[] newArray(int size) {
		    return new CityInstance[size];
		}
	};
	
    private CityInstance(Parcel in) {
        in.readDoubleArray(bbox);
        in.readDoubleArray(center);
        default_zoom = in.readInt();
        bikeshare_url = in.readString();
        bikeshare_name = in.readString();
        transit_url = in.readString();
        transit_name = in.readString();
        city_name = in.readString();
        prefix = in.readString();
        tile_server = in.readString();
        route_server = in.readString();
    }
	public CityInstance() {
	}
	public static CityInstance fromJSON(JSONObject jsonObj) {
		CityInstance ret = new CityInstance();
		
		JSONArray jsonBbox;
		try {
			jsonBbox = jsonObj.getJSONArray("BBOX");
			ret.bbox[0] = jsonBbox.getDouble(0);
			ret.bbox[1] = jsonBbox.getDouble(1);
			ret.bbox[2] = jsonBbox.getDouble(2);
			ret.bbox[3] = jsonBbox.getDouble(3);
		} catch (JSONException e) {
			ret.bbox = null;
		}

		JSONArray jsonCenter;
		try {
			jsonCenter = jsonObj.getJSONArray("DEFAULT_POINT");
			ret.center[0] = jsonCenter.getDouble(0);
			ret.center[1] = jsonCenter.getDouble(1);
		} catch (JSONException e) {
			ret.center = null;
		}
		
		try {
			ret.default_zoom = jsonObj.getInt("DEFAULT_ZOOM");
		} catch (JSONException e) {
		}
		try {
			ret.bikeshare_url = jsonObj.getString("BIKESHARE_URL");
		} catch (JSONException e) {
		}
		try {
			ret.bikeshare_name = jsonObj.getString("BIKESHARE_NAME");
		} catch (JSONException e) {
		}
		try {
			ret.transit_url = jsonObj.getString("TRANSIT_URL");
		} catch (JSONException e) {
		}
		try {
			ret.transit_name = jsonObj.getString("TRANSIT_NAME");
		} catch (JSONException e) {
		}
		try {
			ret.city_name = jsonObj.getString("CITY_NAME");
		} catch (JSONException e) {
		}
		try {
			ret.prefix = jsonObj.getString("PREFIX");
		} catch (JSONException e) {
		}
		try {
			ret.tile_server = jsonObj.getString("TILE_SERVER");
		} catch (JSONException e) {
		}
		try {
			ret.route_server = jsonObj.getString("ROUTE_SERVER");
		} catch (JSONException e) {
		}
		
		
		return ret;
		
	}
	
	public String toString(){
		return "<CityInstance "+prefix+" name="+city_name+">";
		
	}
}
