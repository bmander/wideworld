package edu.mit.media.wideworld;

import android.os.Parcel;
import android.os.Parcelable;

public class CityInstance implements Parcelable{
	float[] bbox = new float[4];
	float[] center = new float[2];
	int default_zoom;
	
	String bikeshare_url;
	String bikeshare_name;
	
	String transit_url;
	String transit_name;
	
	String city_name;
	String prefix;
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloatArray(bbox);
		dest.writeFloatArray(center);
		dest.writeInt(default_zoom);
		dest.writeString(bikeshare_url);
		dest.writeString(bikeshare_name);
		dest.writeString(transit_url);
		dest.writeString(transit_name);
		dest.writeString(city_name);
		dest.writeString(prefix);
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
        in.readFloatArray(bbox);
        in.readFloatArray(center);
        default_zoom = in.readInt();
        bikeshare_url = in.readString();
        bikeshare_name = in.readString();
        transit_url = in.readString();
        transit_name = in.readString();
        city_name = in.readString();
        prefix = in.readString();
    }
}
