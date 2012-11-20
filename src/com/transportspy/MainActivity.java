package com.transportspy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.transportspy.db.DBConnector;
import com.transportspy.db.Options;

public class MainActivity extends MapActivity {
	private AQuery aq;
	private boolean isCenter = false;
	private Options options;
	private Pattern transportListPattern;
	private Pattern transportNumberPatern;
	//final private Options defailtOptions = new Options(10, "136, 32");
	
	@Override
	protected boolean isRouteDisplayed() {
	    return false;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aq = new AQuery(this);
        transportListPattern = Pattern.compile("(\\s*\\w+\\d*,*\\s*)+");
        transportNumberPatern = Pattern.compile("№\\s*(\\d+\\w*)");
        
        setContentView(R.layout.activity_main);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
        MapController mapController = mapView.getController();
        
        DBConnector connector = new DBConnector(this);
        options = connector.getOptions();
        if(options == null)
        {
        	options = new Options(10, "136, 32");//defailtOptions;
        }
        mapController.setZoom(options.getZoom());
        ReuestToServer();
	}
	
	@Override
    public void onPause()
	{
		super.onPause();
		if(options != null)
		{
			DBConnector connector = new DBConnector(this);
			connector.setOptions(options);
		}
	}
	public void editTextInfoOnClick(View view)
	{
		String transportList = options.getTransportList();
		if(transportList != null && !TextUtils.isEmpty(transportList))
		{	
			EditText et =  (EditText)findViewById(R.id.editTextInfo);
			Editable data = et.getText();
			Matcher m = transportListPattern.matcher(data);
			if(!m.matches())
			{
				et.setText(transportList);
			}
		}
	}
	
	public void onClickRefresh(View view)  
	{ 
		EditText et =  (EditText)findViewById(R.id.editTextInfo);
		Editable data = et.getText();
		
		InputMethodManager imm = (InputMethodManager)
		        getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
		
		Matcher m = transportListPattern.matcher(data);
		if(m.matches())
		{
			options.setTransportList(data.toString());
		}
		ReuestToServer();
	}  
	
	private String GetCurrentTime()
	{
		Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return String.format("%02d:%02d:%02d", hour, minute, second); // ЧЧ:ММ:СС - формат времени
    }
	
	private void DrawTransports(String jsonString, Boolean isCenter)
	{
		List<Overlay> mapOverlays = null;
		MapView mapView = (MapView) findViewById(R.id.mapview);
		JSONArray array = String2JsonArray(jsonString);
		if(array != null)
		{
			int minLatitude = Integer.MAX_VALUE, minLongitude = Integer.MAX_VALUE;
			int maxLatitude = Integer.MIN_VALUE, maxLongitude = Integer.MIN_VALUE;
			EditText et =  (EditText)findViewById(R.id.editTextInfo);
			//TODO: Add show marshruts
			//et.setText(GetCurrentTime()+". "+String.valueOf(spyMarshrut)+"("+array.length()+")");
			Map<String, Integer> infoMap = new HashMap<String, Integer>();
			for(int n=0; n<array.length(); n++)
			{
				try 
				{
					JSONObject jo = array.getJSONObject(n);
					String info  = jo.getString("info");
					String marshrutNumber = getTransportNumber(info);
					if(infoMap.containsKey(marshrutNumber))
					{
						int oldValue = infoMap.get(marshrutNumber);
						oldValue++;
						infoMap.put(marshrutNumber, oldValue);
					}
					else
					{
						infoMap.put(marshrutNumber, 1);
					}
					
					JSONArray coordinatesJsonArray = jo.getJSONArray("cordinate");
					int iLatitude, iLongitude;
					if(coordinatesJsonArray.length()>1)
					{
						iLatitude = (int)(coordinatesJsonArray.getDouble(0) * 1000000);
						minLatitude = Math.min(minLatitude, iLatitude);
						maxLatitude = Math.max(minLatitude, iLatitude);
						iLongitude = (int)(coordinatesJsonArray.getDouble(1) *1000000);
						minLongitude = Math.min(minLongitude, iLongitude);
						maxLongitude = Math.max(minLongitude, iLongitude);
						String transportNumber = getTransportNumber(info);
						Drawable drawable;
						drawable = new TextDrawable(transportNumber);
						DrawableItemizedOverlay itemizedoverlay = new DrawableItemizedOverlay(drawable, this);
						GeoPoint point = new GeoPoint(iLatitude, iLongitude);
						OverlayItem overlayitem = new OverlayItem(point, "Info", info);
						itemizedoverlay.addOverlay(overlayitem);
						
						if(mapOverlays == null)
						{
							mapOverlays = mapView.getOverlays();
							mapOverlays.clear();
						}
						mapOverlays.add(itemizedoverlay);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			String s = infoMap.toString();
			et.setText(GetCurrentTime()+". " + s);
			if(isCenter && minLatitude != Integer.MAX_VALUE)
			{
				MapController mapController = mapView.getController();
		        GeoPoint point = new GeoPoint((minLatitude + maxLatitude)/2, (minLongitude+maxLongitude)/2);
		        mapController.animateTo(point);

			}
			mapView.invalidate();
		}
	}
	private String getTransportNumber(String info)
	{
		Matcher m = transportNumberPatern.matcher(info);
		if(m.find())
		{
			return m.group(1);
		}
		else
		{
			return "???";
		}
	}
	private JSONArray String2JsonArray(String string)
	{
		if(string == null || string.isEmpty())
			return null;
		int firstSymbol = string.codePointAt(0);
		if(firstSymbol == 0x61)
		{
			string = string.substring(1, string.length()-1);
		}
		JSONArray ret = null;
		try {
			ret = new JSONArray(string);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return ret;		
	}
	
	private void ReuestToServer()
	{
		Button button = (Button)findViewById(R.id.buttonRefresh);
		button.setEnabled(false);

		//http://transit.in.ua/import.php?dataRequest[]=dnepropetrovsk-taxi-1
		//http://transit.in.ua/importTransport.php?dataRequest%5B%5D=dnepropetrovsk-taxi-1&dataRequest%5B%5D=dnepropetrovsk-taxi-2
		String[] transportsArray = options.getTransportsArray(); 
		if( transportsArray != null)
		{
			List<String> parameters = new ArrayList<String>(); 
			for (String string : transportsArray) {
				parameters.add("dataRequest%5B%5D=dnepropetrovsk-taxi-"+string);
			}
			String parametersString = TextUtils.join("&", parameters);
			//String url = "http://transit.in.ua/importTransport.php?dataRequest%5B%5D=dnepropetrovsk-taxi-" + spyMarshrut;
			String url = "http://transit.in.ua/importTransport.php?"+parametersString;
        	aq.ajax(url, String.class, this, "GetNewDataCallback");
		}
	}
	
	public void GetNewDataCallback(String url, String jsonString, AjaxStatus status)
	{
		Button button = (Button)findViewById(R.id.buttonRefresh);
		button.setEnabled(true);
		DrawTransports(jsonString, isCenter);
		isCenter = false;
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void locateToCurrentPosition()
    {
    	MapView mapView = (MapView) findViewById(R.id.mapview);        
        MapController mapController = mapView.getController();
        
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    	final LocationListener locationListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				
			}

			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				
			}
    	};
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    	Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	if(location == null)
    		location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    	if(location == null)
    		location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    	if(location != null)
    	{
    		mapController.animateTo(new GeoPoint(
    				(int) (location.getLatitude() * 1000000), 
    				(int) (location.getLongitude() * 1000000)));
    	}
    }
}
