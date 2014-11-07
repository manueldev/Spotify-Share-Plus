package com.malejandrodev.addartisttitleforspotifyshare;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.*;

public class MainActivity extends Activity {

	String trackID, artista, titulo, link;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		trackID = getTrackIDFromIntent();
		
		if(trackID != null){
			getSongInfo(trackID);
		}else{
			Toast.makeText(getApplicationContext(), getString(R.string.error_notrackid), Toast.LENGTH_SHORT).show();
		}
		this.finish();
	}
	
	private void getSongInfo(String trackID) {
		AsyncHttpClient client = new AsyncHttpClient();
		client.get("https://api.spotify.com/v1/tracks/" + trackID, new JsonHttpResponseHandler(){

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONObject response) {
				try {
					titulo = response.getString("name");
					JSONArray artistasJSONArray = response.getJSONArray("artists");
					JSONObject artistasJSONObject = artistasJSONArray.getJSONObject(0);
					artista = artistasJSONObject.getString("name");
					
					if (BuildConfig.DEBUG) {
						Log.i(getString(R.string.app_name), artista + " " + titulo);
					}
					enviarCancion("♫ " + titulo + " - " + artista + " " + link + " #NowPlaying");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					Throwable throwable, JSONObject errorResponse) {
				Toast.makeText(getApplicationContext(), getString(R.string.error_nosonginfo), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private String getTrackIDFromIntent() {
		Intent receivedIntent = getIntent();
		String receivedAction = receivedIntent.getAction();
		if(receivedAction.equals(Intent.ACTION_SEND)){
			String receivedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
			if (receivedText != null){
				link  = receivedText;
				String[] receivedText2 = receivedText.split("/");
				return receivedText2[receivedText2.length-1];
			}			
		}
		return null;
	}
	
	public void enviarCancion(String txPublicar){
		//txPublicar debe incluir "titulo – artista link #NowPlaying"
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, txPublicar);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}
}
