package edu.mit.media.wideworld;

import java.util.List;

import android.content.Context;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

class AddressAdapter extends BaseAdapter {
	
	private List<Address> addresses;
	
	AddressAdapter( List<Address> addresses ){
		this.addresses = addresses;
	}

	public AddressAdapter() {
		this.addresses = null;
	}

	@Override
	public int getCount() {
		if( this.addresses == null ){
			return 0;
		}
		return addresses.size();
	}

	@Override
	public Address getItem(int i) {
		return addresses.get(i);
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int pos, View convert, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout item = (LinearLayout) inflater.inflate( R.layout.locpicker_item, parent, false );
		TextView line1 = (TextView) item.findViewById(R.id.line1);
		TextView line2 = (TextView) item.findViewById(R.id.line2);
		Address address = addresses.get(pos);
		line1.setText( address.getAddressLine(0) );
		line2.setText( getSecondLine(address) );
		return item;
	}

	private String getSecondLine(Address address) {
		String secondLine = "";
		if(address.getLocality() != null){
			secondLine += address.getLocality();
		}
		if(address.getSubAdminArea() != null){
			if(secondLine.length()>0){ secondLine += ", "; }
			secondLine += address.getSubAdminArea();
		}
		if(address.getAdminArea() != null){
			if(secondLine.length()>0){ secondLine += ", "; }
			secondLine += address.getAdminArea();
		}
		return secondLine;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public void clear() {
		this.addresses = null;
	}
	
}