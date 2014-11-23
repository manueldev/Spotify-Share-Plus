package com.malejandrodev.addartisttitleforspotifyshare;


import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
	
	GoogleAnalytics analytics = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Remove Notificacions
		NotificationManager notifManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		notifManager.cancel(0303456);
		
		
		analytics = GoogleAnalytics.getInstance(this);
		if (BuildConfig.DEBUG){
			analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
		}
		analytics.enableAutoActivityReports(getApplication());
		Tracker mGATracker = analytics.newTracker(R.xml.global_tracker);
		mGATracker.enableAutoActivityTracking(true);
		 
        
		String url = getLinkFromIntent();
		if(url == null){
			url = "nourl";
		}		
		new PublishItem(getApplicationContext()).execute(url);
		
		//Updatecheck
        new UpdateCheck(getApplicationContext()).execute();
        
		this.finish();
	}

	@Override
	protected void onStart() {
		analytics.reportActivityStart(this);
	}
	
	@Override
	protected void onStop() {
		analytics.reportActivityStop(this);
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