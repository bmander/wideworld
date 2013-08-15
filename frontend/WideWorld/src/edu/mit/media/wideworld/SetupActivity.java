package edu.mit.media.wideworld;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SetupActivity extends Activity {
	
	View pitch;
	View consentForm;
	View cityPicker;

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
			}	
		});
		
		setContentView( pitch );

		setFinishOnTouchOutside(false);
    }
    
}