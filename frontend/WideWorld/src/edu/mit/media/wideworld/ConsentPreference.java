package edu.mit.media.wideworld;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ConsentPreference extends DialogPreference{
	
	boolean value;
	private CheckBox consentcheckbox;

	public ConsentPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        setDialogLayoutResource(R.layout.consent_form);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
    }
	
	@Override
	protected void onBindDialogView(View view) {

		view.findViewById(R.id.yesbutton).setVisibility(View.GONE); //the done button is for showing this on setup
		
		this.consentcheckbox = (CheckBox) view.findViewById(R.id.consentcheckbox);
		this.consentcheckbox.setChecked(value);
		this.consentcheckbox.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				ConsentPreference.this.value = arg1;
			}
			
		});

	    super.onBindDialogView(view);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {

	    if(!positiveResult){
	        return;
	    }

	    this.persistBoolean( this.consentcheckbox.isChecked() );
	    setSummaryToConsentValue();

	    super.onDialogClosed(positiveResult);
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	    if (restorePersistedValue) {
	    	this.value = this.getPersistedBoolean(false);
	    } else {
	        this.value = false;
	    }
	    
	    setSummaryToConsentValue();
	    
	}
	
	private void setSummaryToConsentValue() {
		if(value){
			setSummary("Consent given");
		} else {
			setSummary("Consent not given");
		}
	}

}
