package edu.mit.media.wideworld;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;

public class CitiesFile {
	public static String getInstancesJSON(Context context) {
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
			e.printStackTrace();
		}
		return null;
	}
	
	public static void saveInstancesJSON(Context context, String instancesJSON) {
		try {
			FileOutputStream fos;
			fos = context.openFileOutput("instances.json", Context.MODE_PRIVATE);
			fos.write(instancesJSON.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<CityInstance> getInstances(String instanceJSON){
		try {	
	
			List<CityInstance> ret = new ArrayList<CityInstance>();
	        Object nextValue = new JSONTokener(instanceJSON).nextValue();
	        
	        if( nextValue.getClass() == String.class ){
	        	return null;
	        } else if( nextValue.getClass() == JSONArray.class ) {
	        	JSONArray jsonInstances = (JSONArray)nextValue;
	        	for(int i=0; i<jsonInstances.length(); i++){
	        		JSONObject jsonObj;
	
					jsonObj = jsonInstances.getJSONObject(i);
	
	        		CityInstance inst = CityInstance.fromJSON( jsonObj );
	        		ret.add( inst );
	        	}
		        return ret;
	        } else {
	        	return null;
	        }
        
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
}
