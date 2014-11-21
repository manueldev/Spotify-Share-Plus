package com.malejandrodev.addartisttitleforspotifyshare;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class UpdateCheck extends AsyncTask<Void, Void, String>{
	
	Context context;
	Date lastCheck, now;
	String lastVersion, localVersion, newApkUrl;
	int mId = 5555;
	long size;
	
	NotificationCompat.Builder mBuilder;
	NotificationManagerCompat mNotificationManager;
	
	@SuppressWarnings("unused")
	@Override
	protected void onPreExecute() {

		now = new Date();
		//realizar consultas cada 24 horas.
		lastCheck = new Date(getLastCheck());
		long td = now.getTime() - lastCheck.getTime();
		float diffHours = (td) / (60 * 60 * 1000);
		//si la diferencia entre ahora y el ultimo check es menor a x horas, se cancela la tarea
		if (diffHours < 6 && !(BuildConfig.DEBUG)) {
			cancel(true);
		}
	}
	@Override
	protected String doInBackground(Void... arg0) {
		localVersion = getLocalVersion();
		lastVersion = getLastVersion();
		SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(context.getResources().getString(R.string.saved_lastCheck), String.valueOf(now.getTime()));
		editor.commit();
		
		if (!localVersion.equals(lastVersion)) {
			showNotification(false, true, 0, context.getResources().getString(R.string.downloadingUpdate));
			
			String filename = context.getString(R.string.app_name) + lastVersion + ".apk";
			
			try {
				final OkHttpClient client;

			    client = new OkHttpClient();
			    client.setConnectTimeout(10, TimeUnit.SECONDS);
			    client.setWriteTimeout(10, TimeUnit.SECONDS);
			    client.setReadTimeout(15, TimeUnit.SECONDS);
			    Request request = new Request.Builder()
			        .url(newApkUrl)
			        .build();	
			    Response response = client.newCall(request).execute();
			   
			    if (response.isSuccessful()) {
	                InputStream inputStream = null;
	                OutputStream output = null;
	                try {
	                    inputStream = response.body().byteStream();
	                    byte[] buff = new byte[1024 * 4];
	                    long downloaded = 0;
	                    long target = size;
	                    if((target = response.body().contentLength()) != -1);
	                    int readed, latestPercentDone, percentDone = -1;
	                    output = context.openFileOutput(filename, Context.MODE_PRIVATE);
	                    
	                    
	    		        while ((readed = inputStream.read(buff)) != -1) {
	    		        	downloaded += readed;
	    		            latestPercentDone  = (int)((downloaded * 100) / target);
	    		            if (percentDone != latestPercentDone) {
	    		                percentDone = latestPercentDone;
	    		                publishProgress(percentDone);
	    		            }
	    		            output.write(buff, 0, readed);
	    		            
	    		            if (isCancelled()) {
	                            return "Descarga cancelada por cancel();";
	                        }
	    		        }
	    		        //return file address
	    		        return context.getFilesDir().getAbsolutePath() + "/" + filename;
	                } catch (IOException ignore) {
	                	Log.e(context.getString(R.string.app_name), "Error " + ignore.getMessage());
	                    return "Error" + ignore.getMessage();
	                } finally {
	                    if (inputStream != null) {
	                        inputStream.close();
	                    }
	                    if (output != null) {
	                    	output.flush();
	                    	output.close();
						}
	                }
	            } else {
	                //Servidor no responde con HTTP-200.
	            	Log.e(context.getString(R.string.app_name), "Descargando update, HTTP: " + response.code());
	            }
			    
			} catch (Exception e) {
				Log.e(context.getString(R.string.app_name), e.getMessage());
			}
		}else{
			cancel(true);
		}
		return null;
	}
	private void publishProgress(int i) {
		try {
			mBuilder.setProgress(100, i, false);
			mNotificationManager.notify(mId, mBuilder.build());
		} catch (Exception e) {
			Log.e(context.getString(R.string.app_name), e.getMessage());
		}
		
	}
	
	private void showNotification(boolean isError, boolean isProgressbar, int progress, String... info){
		//info: 0 title, 1 description
		String tickerText;
		mBuilder = new NotificationCompat.Builder(context);
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
		if(isProgressbar){
			mBuilder.setOngoing(true); 
		}
		
		mBuilder.setTicker(tickerText);
		mNotificationManager =
				NotificationManagerCompat.from(context);
		mNotificationManager.notify(null, mId, mBuilder.build());
	}

	// begin the installation by opening the resulting file
	@Override
	protected void onPostExecute(String path) {
		//Remove Notificacions
  		mNotificationManager.cancel(mId);
  		Intent i = new Intent();
	    i.setAction(Intent.ACTION_VIEW);
	    File f = new File(path);
	    f.setReadable(true, false);
	    i.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive" );
	    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    Log.d(context.getString(R.string.app_name), "Installing new version");
	    context.startActivity(i);
	}
	
	public UpdateCheck(Context contextin){
		context = contextin;
	}
	
	private String getLocalVersion() {
		try {
			return "v" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "0";
	}
	private String getLastVersion() {
		String url = "https://api.github.com/repos/manueldev/Spotify-Share-Plus/releases";
		try {
			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url(url).build();
			Response response = client.newCall(request).execute();
			
		    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
		    JSONArray jsonParsed = new JSONArray(response.body().string());
		    for (int i = 0; i < jsonParsed.length(); i++) {
		    	boolean preRelease = jsonParsed.getJSONObject(i).getBoolean("prerelease");
		    	if(!preRelease){
		    		newApkUrl = jsonParsed.getJSONObject(i).getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
		    		size = jsonParsed.getJSONObject(i).getJSONArray("assets").getJSONObject(0).getInt("size");
		    		return jsonParsed.getJSONObject(i).getString("tag_name");
		    	}
			}		    
		} catch (Exception e) {
			Log.e(context.getString(R.string.app_name), e.getMessage());
		}
		return "0";
	}
	
	private long getLastCheck() {
		SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		String lastCheckFromSharedPreferences = sharedPref.getString(context.getResources().getString(R.string.saved_lastCheck), String.valueOf(now.getTime()));
		return Long.valueOf(lastCheckFromSharedPreferences).longValue();
	}

	
}