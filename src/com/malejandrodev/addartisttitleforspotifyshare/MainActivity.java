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
		 
        
		String[] url = getLinkAndDescriptionFromIntent();		
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

	private String[] getLinkAndDescriptionFromIntent() {
		Intent receivedIntent = getIntent();
		String receivedAction = receivedIntent.getAction();
		String rv[] = {"no_url","no_subject"};
		if(receivedAction.equals(Intent.ACTION_SEND)){
			rv[0] = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
			if (rv[0].startsWith("http://open.spotify.com/")) {
				rv[1] = (receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT));
			}
		}else if (BuildConfig.DEBUG){
			rv[0] = "http://open.spotify.com/user/0qgiFuYhYuwtFXEwYakddE/starred";
			rv[1] = receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT);
		}
		return rv;
	}		
}