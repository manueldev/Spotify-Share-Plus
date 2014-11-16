package com.malejandrodev.addartisttitleforspotifyshare;


import com.google.android.gms.analytics.GoogleAnalytics;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Remove Notificacions
		NotificationManager notifManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		notifManager.cancelAll();
		
        GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker).enableAdvertisingIdCollection(true);
        
        
		String url = getLinkFromIntent();
		if(url == null){
			url = "nourl";
		}		
		new PublishItem(getApplicationContext()).execute(url);
		this.finish();
	}
	
	private String getLinkFromIntent() {
		Intent receivedIntent = getIntent();
		String receivedAction = receivedIntent.getAction();
		

		
		
		if(receivedAction.equals(Intent.ACTION_SEND)){
			String rv = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
			if (rv.startsWith("http://open.spotify.com/")) {
				return rv;
			} else {
				return null;
			}
		}else if (BuildConfig.DEBUG){
			return "http://open.spotify.com/user/0qgiFuYhYuwtFXEwYakddE/starred";
		}
		else{
			return null;
		}	
	}		
}