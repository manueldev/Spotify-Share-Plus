package com.malejandrodev.addartisttitleforspotifyshare;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class PublishItem extends AsyncTask<String, Void, Void>{
	
	/* 
	 * Item Types
	 * 
	 * Track: ♫ Reina Japonesa - Fernando Milagros open.spotify.com/track/5OEqQkQ #NowPlaying
	 * https://open.spotify.com/track/5OEqQkQKB37nPH30qyYHsA
	 * /tracks/{id}
	 *  
	 * Artist: ♫ Tears For Fears spoti.fi/MVI4N1 #NowPlaying
	 * https://open.spotify.com/artist/4bthk9UfsYUYdcFyqxmSUU?
	 * /artists/{id} 
	 * 
	 * Album: ♫ The Seeds Of Love (Remastered with bonus tracks) – Tears For Fears spoti.fi/Q1ZxLc #NowPlaying
	 * https://open.spotify.com/album/79XgUw5U86BeIiMnZ4Rrht
	 * /albums/{id}
	 * 
	 * Starred list: ♫ Starred, Manuel Alejandro spoti.fi/1uQLjB2 #NowPlaying
	 * http://open.spotify.com/user/1231232/starred
	 * /users/{user_id} 
	 * 
	 * */

	String link, title, artist, jsonr, item, mensaje = "♫ ";
	String[] tipoSh = {
            "tracks",
            "artists",
            "albums",
            "starred"
    	};
	Integer mId = 0303456, tipoSelected;
	Context context;
	
	public PublishItem(Context contextin){
		context = contextin;
	}
	
	protected Void doInBackground(String... arg0) {
		link = arg0[0];
		if(!isNetworkAvailable()){
			showNotification(true, "Error", context.getString(R.string.error_noinet));
		}else if(!parseLink() || link.equals("no_url")){
			showNotification(false, "Error", context.getString(R.string.error_whenShFromWrongPlace));
		}else if(!getitemInfo()){
			showNotification(true, "Error", context.getString(R.string.error_parse));
		}else if(!creatingMessage()){
			showNotification(true, "Error", context.getString(R.string.error_parse));
		}else{
			enviarCancion(mensaje);
			sendTypeAnalytics();
		}
		return null;
	}
	
	private void sendTypeAnalytics() {
		// Get tracker.
		Tracker t = GoogleAnalytics.getInstance(context).newTracker(R.xml.global_tracker);
		t.enableAutoActivityTracking(true);
		// Build and send an Event.
		t.send(new HitBuilders.EventBuilder()
		    .setCategory("Normal")
		    .setAction("Publicando")
		    .setLabel(tipoSh[tipoSelected])
		    .build());
	}

	private boolean creatingMessage() {
		try {
			JSONObject jsonParsed = new JSONObject(jsonr);
			if (tipoSh[tipoSelected].equals("starred")){
				String starredText = context.getResources().getStringArray(R.array.itemTypes)[3];
				String owner = jsonParsed.getString("display_name");
				mensaje += starredText.substring(0, 1).toUpperCase(Locale.getDefault()) + starredText.substring(1) + ", " + owner + " " + link + " #NowPlaying";
			}else if(tipoSh[tipoSelected].equals("tracks") || tipoSh[tipoSelected].equals("albums")){
				title = jsonParsed.getString("name");
				JSONArray artistsJSONArray = jsonParsed.getJSONArray("artists");
				for (int i = 0; i < artistsJSONArray.length(); i++){
					if(i == 0){
						artist = artistsJSONArray.getJSONObject(i).getString("name");
					}else{
						artist += ", " + artistsJSONArray.getJSONObject(i).getString("name");
					}
				}
				mensaje += title + " - " + artist + " " + link + " #NowPlaying";
			}else if(tipoSh[tipoSelected].equals("artists")){
				artist = jsonParsed.getString("name");
				mensaje += artist + " " + link + " #NowPlaying";
			}
			return true;
		} catch (Exception e) {
			Log.e(context.getString(R.string.app_name), e.getMessage());
			return false;
		}
	}	

	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	private void showNotification(boolean isError, String... info){
		//info: 0 title, 1 description,
		String tickerText;
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder
        .setSmallIcon(R.drawable.ic_stat_notify_default)
        .setContentTitle(info[0]);
		tickerText = info[0];
		if (info.length == 2){
			mBuilder
		        .setContentText(info[1])
		        .setStyle(new NotificationCompat.BigTextStyle()
		        .bigText(info[1]));
			tickerText = info[1];
		}
		if (isError){
			Intent intent = new Intent(context, MainActivity.class);
			intent.putExtra(Intent.EXTRA_TEXT, link);
			//ARRG
			intent.putExtra("retryMode", true);
			intent.setAction(Intent.ACTION_SEND);
			intent.setType("text/plain");
			PendingIntent resendIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
			mBuilder.addAction(R.drawable.ic_action_refresh, context.getString(R.string.retry), resendIntent);
		}
		
		mBuilder.setTicker(tickerText);
		NotificationManagerCompat mNotificationManager =
				NotificationManagerCompat.from(context);
		mNotificationManager.notify(null, mId, mBuilder.build());
	}
	
	private boolean parseLink() {
		try {
			String[] linkDivido = link.split("/");
			String tipoDeItem = linkDivido[linkDivido.length-2];
			tipoDeItem += "s";		
			tipoSelected = Arrays.asList(tipoSh).indexOf(tipoDeItem);
			if(linkDivido[linkDivido.length-1].equals("starred")){
				item = linkDivido[linkDivido.length-2];
				tipoSelected = Arrays.asList(tipoSh).indexOf("starred");
			}else{
				item = linkDivido[linkDivido.length-1];
			}
			if (tipoSelected < 0){
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}		
	}

	private void enviarCancion(String txPublicar){
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, txPublicar);
		sendIntent.setType("text/plain");
		context.startActivity(sendIntent);
	}

	private void loadData(String url) throws Exception {
		OkHttpClient client = new OkHttpClient();
		
		client.setConnectTimeout(5, TimeUnit.SECONDS);
	    
		Request request = new Request.Builder()
        .url(url)
        .build();
		

	    Response response = client.newCall(request).execute();
	    if (!response.isSuccessful()){ 
	    	client.cancel(request);
	    	throw new IOException("Unexpected code " + response);
	    }
	    jsonr = response.body().string();
	}
	
	private boolean getitemInfo() {
		
		String apiUrl = "https://api.spotify.com/v1/";
		if (tipoSh[tipoSelected].equals("starred")){
			apiUrl += "users/" + item; 
		}else{
			apiUrl += tipoSh[tipoSelected] + "/" + item; 
		}
		
		try {
			loadData(apiUrl);
			return true;
		} catch (Exception e) {
			Log.w(context.getString(R.string.app_name), e.getMessage());
			return false;
		}

	}	
}