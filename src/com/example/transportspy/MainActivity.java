package com.example.transportspy;

import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.R.bool;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends MapActivity {
	private AQuery aq;
	private int spyMarshrut = 136;
	private boolean isCenter = true;
	
	@Override
	protected boolean isRouteDisplayed() {
	    return false;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aq = new AQuery(this);
        
        setContentView(R.layout.activity_main);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
        MapController mapController = mapView.getController();
        mapController.setZoom(10);
        ReuestToServer();
	}
	
	public void onClickRefresh(View view)  
	{ 
		EditText et =  (EditText)findViewById(R.id.editTextInfo);
		int oldMarshrute = spyMarshrut;
		try
		{
			int number = Integer.parseInt(et.getText().toString());
			spyMarshrut = number;
		}catch(NumberFormatException ex){}
		if(oldMarshrute != spyMarshrut)
			isCenter = true;
		
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
			et.setText(GetCurrentTime()+". "+String.valueOf(spyMarshrut)+"("+array.length()+")");
			for(int n=0; n<array.length(); n++)
			{
				try 
				{
					JSONObject jo = array.getJSONObject(n);
					String info  = jo.getString("info");
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

						Drawable drawable = new TextDrawable(String.valueOf(spyMarshrut));
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
			if(isCenter && minLatitude != Integer.MAX_VALUE)
			{
				MapController mapController = mapView.getController();
		        GeoPoint point = new GeoPoint((minLatitude + maxLatitude)/2, (minLongitude+maxLongitude)/2);
		        mapController.animateTo(point);

			}
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

		String url = "http://transit.in.ua/importTransport.php?dataRequest%5B%5D=dnepropetrovsk-taxi-" + spyMarshrut;             
        aq.ajax(url, String.class, this, "GetNewDataCallback");
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
}
