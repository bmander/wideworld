package edu.mit.media.wideworld;

import java.util.List;

import android.location.Address;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int pos, View convert, ViewGroup parent) {
		TextView foo = new TextView(parent.getContext());
		Address address = addresses.get(pos);
		foo.setText( address.getAddressLine(0) + ", " + address.getAddressLine(1) );
		return foo;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}
	
}