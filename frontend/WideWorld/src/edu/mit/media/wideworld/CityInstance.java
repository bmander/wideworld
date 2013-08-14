package edu.mit.media.wideworld;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class CityInstance implements Parcelable{
	double[] bbox = new double[4];
	double[] center = new double[2];
	int default_zoom;
	
	String bikeshare_url;
	String bikeshare_name;
	
	String transit_url;
	String transit_name;
	
	String city_name;
	String prefix;
	
	String tile_server;
	
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
    }
	public CityInstance() {
	}
	public static CityInstance fromJSON(JSONObject jsonObj) throws JSONException {
		CityInstance ret = new CityInstance();
		
		JSONArray jsonBbox = jsonObj.getJSONArray("BBOX");
		ret.bbox[0] = jsonBbox.getDouble(0);
		ret.bbox[1] = jsonBbox.getDouble(1);
		ret.bbox[2] = jsonBbox.getDouble(2);
		ret.bbox[3] = jsonBbox.getDouble(3);
		
		JSONArray jsonCenter = jsonObj.getJSONArray("DEFAULT_POINT");
		ret.center[0] = jsonCenter.getDouble(0);
		ret.center[1] = jsonCenter.getDouble(1);
		
		ret.default_zoom = jsonObj.getInt("DEFAULT_ZOOM");
		ret.bikeshare_url = jsonObj.getString("BIKESHARE_URL");
		ret.bikeshare_name = jsonObj.getString("BIKESHARE_NAME");
		ret.transit_url = jsonObj.getString("TRANSIT_URL");
		ret.transit_name = jsonObj.getString("TRANSIT_NAME");
		ret.city_name = jsonObj.getString("CITY_NAME");
		ret.prefix = jsonObj.getString("PREFIX");
		ret.tile_server = jsonObj.getString("TILE_SERVER");
		
		
		return ret;
		
	}
	
	public String toString(){
		return "<CityInstance "+prefix+" name="+city_name+">";
		
	}
}
