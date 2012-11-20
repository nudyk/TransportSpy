package com.transportspy.db;

import java.io.Serializable;

import android.text.TextUtils;

public class Options implements Serializable {
	private static final long serialVersionUID = -2871106788575097215L;
	private int zoom;
	private String transportList;
	private String[] transportsArray;
	
	public Options(int zoom, String transportList)
	{
		setZoom(zoom);
		setTransportList(transportList);
	}

	private void setZoom(int value)
	{
		if(value<1)
			zoom = 1;
		else if(value > 21)
			zoom = 21;
		else
			zoom = value;
	}
	
	public void setTransportList(String value)
	{
		transportList = value;
		transportsArray = null;
		if(value != null && !TextUtils.isEmpty(value))
		{
			transportsArray = value.split(",");
			for (int n=0; n<transportsArray.length; n++) {
				transportsArray[n] = transportsArray[n].trim();
			}
		}
	}
	
	public int getZoom()
	{
		return zoom;
	}

	public String getTransportList()
	{
		return transportList;
	}
	public String[] getTransportsArray()
	{
		return transportsArray;
	}
}
