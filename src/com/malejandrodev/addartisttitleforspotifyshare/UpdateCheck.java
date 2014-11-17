package com.malejandrodev.addartisttitleforspotifyshare;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.json.JSONArray;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.app.NotificationManager;
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
	int size, mId = 5555;
	
	@SuppressWarnings("unused")
	@Override
	protected void onPreExecute() {

		now = new Date();
		//realizar consultas cada 24 horas.
		lastCheck = new Date(getLastCheck());
		long td = now.getTime() - lastCheck.getTime();
		float diffHours = (td) / (60 * 60 * 1000);
		//si la diferencia entre ahora y el ultimo check es menor a 12 horas, se cancela la tarea
		if (diffHours < 12 && !(BuildConfig.DEBUG)) {
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
		    String filename = context.getString(R.string.app_name) + lastVersion + ".apk";
		    try {
		        URL url = new URL(newApkUrl);
		        URLConnection connection = url.openConnection();
		        connection.connect();

		        int fileLength = size;
		        // download the file
		        InputStream input = new BufferedInputStream(url.openStream());
		        OutputStream output = context.openFileOutput(filename, Context.MODE_PRIVATE);

		        byte data[] = new byte[1024];
		        long total = 0;
		        int count;
		        while ((count = input.read(data)) != -1) {
		            total += count;
		            publishProgress((int) (total * 100 / fileLength));
		            output.write(data, 0, count);
		        }

		        output.flush();
		        output.close();
		        input.close();
		    } catch (Exception e) {
		        Log.e("YourApp", "Well that didn't work out so well...");
		        Log.e("YourApp", e.getMessage());
		    }
		    return context.getFilesDir().getAbsolutePath() + "/" + filename;
		}
		return null;
	}
	private void publishProgress(int i) {
		// TODO Auto-generated method stub
		showNotification(false, true, i, "Descargando nueva version");
	}
	
	private void showNotification(boolean isError, boolean isProgressbar, int progress, String... info){
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
		if(isProgressbar){
			mBuilder.setOngoing(true); 
			mBuilder.setProgress(size, progress, false);
		}
//		if (isError){
//			Intent intent = new Intent(context, MainActivity.class);
//			intent.putExtra(Intent.EXTRA_TEXT, link);
//			//ARRG
//			intent.putExtra("retryMode", true);
//			intent.setAction(Intent.ACTION_SEND);
//			intent.setType("text/plain");
//			PendingIntent resendIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
//			mBuilder.addAction(R.drawable.ic_action_refresh, context.getString(R.string.retry), resendIntent);
//		}
		
		mBuilder.setTicker(tickerText);
		NotificationManagerCompat mNotificationManager =
				NotificationManagerCompat.from(context);
		mNotificationManager.notify(null, mId, mBuilder.build());
	}

	// begin the installation by opening the resulting file
	@Override
	protected void onPostExecute(String path) {
		//Remove Notificacions
  		NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  		notifManager.cancel(mId);
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
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
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