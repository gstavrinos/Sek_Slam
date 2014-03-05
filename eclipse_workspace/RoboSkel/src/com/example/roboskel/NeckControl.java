package com.example.roboskel;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.camera.simplemjpeg.MjpegInputStream;
import com.camera.simplemjpeg.MjpegView;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class NeckControl extends FragmentActivity implements ActionBar.OnNavigationListener{
	
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private int rotate = 168;
	private int updown = 0;	
    private boolean cartesian_on = false;
    private boolean polar_on = false;
    private boolean standard_on = false;
    private MjpegView mv = null;
    private String URL;
	private static final boolean DEBUG=true;
    private static final String TAG = "MJPEG";
	private int width = 320;
    private int height = 240;
    private final Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			Intent intent = getIntent();
			String caller = intent.getStringExtra("caller");
			Window window = this.getWindow();
			window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
			if(caller.equals("menu")){
				this.setTheme(android.R.style.Theme_Holo_Light);
				ActionBar actionBar = getActionBar();
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setIcon(R.drawable.camera);
			}
		}
		catch(Exception e){
			Log.w("Warning", "Calling activity did not send any extras!");
		}
		setContentView(R.layout.activity_neck_control);
		if(!ActiveConnection.getConn().isStreaming())
			ActiveConnection.getConn().stream(false);
		/* get view for streaming */
		mv = (MjpegView) findViewById(R.id.mv);
		if(mv != null){
        	mv.setResolution(width, height);
        }
		mv.setVisibility(View.INVISIBLE);
	    ActiveConnection.getConn().setState(14);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    SeekBar rotation=(SeekBar)findViewById(R.id.rotation);
	    rotation.incrementProgressBy(1);
	    rotation.setMax(34);
	    rotation.setMinimumHeight(300);
	    rotation.setMinimumWidth(100);
	    rotation.setProgress(17);
	    rotation.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
	    	
	        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	    	{
	            rotate = 150 + progress;
	            //Toast.makeText(getBaseContext(),"Rotation : "+rotate, Toast.LENGTH_SHORT).show();
	          // ActiveConnection.getConn().setState(14);
	            try{
	            	ActiveConnection.getConn().setRotationAndTilt(rotate, updown, false);
	            }
	            catch(Exception e){
	        		Log.e("Error", "Could not connect to Sek!");
	        	}
	            	//ActiveConnection.getConn().send(665);
	        }
	        
	        public void onStartTrackingTouch(SeekBar seekBar) {}

	        public void onStopTrackingTouch(SeekBar seekBar) {}
	    });
	    SeekBar tilt=(SeekBar)findViewById(R.id.tilt);
	    tilt.incrementProgressBy(1);
	    tilt.setMax(60);
	    tilt.setMinimumHeight(300);
	    tilt.setMinimumWidth(100);
	    tilt.setProgress(30);
	    tilt.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
	    	
	        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	    	{
	        	updown = progress - 30;
	            //Toast.makeText(getBaseContext(),"Tilt : "+updown, Toast.LENGTH_SHORT).show();
	            //ActiveConnection.getConn().setState(15);
	        	try{
	        		ActiveConnection.getConn().setRotationAndTilt(rotate, updown, false);
	        	}
	        	catch(Exception e){
	        		Log.e("Error", "Could not connect to Sek!");
	        	}
	        		//ActiveConnection.getConn().send(665);
	        }
	        
	        public void onStartTrackingTouch(SeekBar seekBar) {}

	        public void onStopTrackingTouch(SeekBar seekBar) {}
	    });
	}
	
	@Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 //super.onOptionsItemSelected(item);
		    	
		 switch(item.getItemId())
		 {
		 case R.id.camera:
				/* if the view is not visible */
				if(!mv.isShown()|| (mv.isShown() && !standard_on))
				{
					if(mv.isShown()){
				    	mv.stopPlayback();
				    	mv.setVisibility(View.INVISIBLE);
					}
					cartesian_on = false;
					polar_on = false;
					standard_on = true;
		        	URL=mv.getUrl(getSharedPreferences("SAVED_VALUES", MODE_PRIVATE),0);
		     		new DoRead().execute(URL);
		     		mv.setVisibility(View.VISIBLE);
		     	}
			    else
			    {
			    	standard_on = false;
			    	mv.stopPlayback();
			    	mv.setVisibility(View.INVISIBLE);
			    }
				break;
				case R.id.cartesian_radar:
					/* if the view is not visible */
					if(!mv.isShown() || (mv.isShown() && !cartesian_on))
					{
						if(mv.isShown()){
					    	mv.stopPlayback();
					    	mv.setVisibility(View.INVISIBLE);
						}
						cartesian_on = true;
						polar_on = false;
						standard_on = false;
			        	URL=mv.getUrl(getSharedPreferences("SAVED_VALUES", MODE_PRIVATE),1);
			     		new DoRead().execute(URL);
			     		mv.setVisibility(View.VISIBLE);
			     	}
				    else
				    {
						cartesian_on = false;
				    	mv.stopPlayback();
				    	mv.setVisibility(View.INVISIBLE);
				    }
					break;
					case R.id.polar_radar:
						/* if the view is not visible */
						if(!mv.isShown() || (mv.isShown() && !polar_on))
						{
							if(mv.isShown()){
						    	mv.stopPlayback();
						    	mv.setVisibility(View.INVISIBLE);
							}
							cartesian_on = false;
							polar_on = true;
							standard_on = false;
				        	URL=mv.getUrl(getSharedPreferences("SAVED_VALUES", MODE_PRIVATE),2);
				     		new DoRead().execute(URL);
				     		mv.setVisibility(View.VISIBLE);
				     	}
					    else
					    {
					    	polar_on = false;
					    	mv.stopPlayback();
					    	mv.setVisibility(View.INVISIBLE);
					    }
						break;
		 }
		 return true;
	 }
	
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();	
		try{
			ActiveConnection.getConn().setRotationAndTilt(rotate, updown, true);
		}
		catch(Exception e){
			Log.e("Error", "Could not connect to Sek!");
		}
			//ActiveConnection.getConn().send(666);
	}
	
	
	
	


	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	    super.onWindowFocusChanged(hasFocus);
	}

	@Override
	   public void onPause() {
	    	if(DEBUG) Log.d(TAG,"onPause()");
	        super.onPause();
	        if(mv!=null){
	        	if(mv.isStreaming()){
			        mv.stopPlayback();
	        	}
	        }
	    }
	
	@Override
	protected void onStop() 
	{
		super.onStop();
	}
	   @Override
	   public void onDestroy() {
	    	if(DEBUG) Log.d(TAG,"onDestroy()");
	    	
	    	if(mv!=null){
	    		mv.freeCameraMemory();
	    	}
	        super.onDestroy();
	        finish();
	    }
	

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		/* Restore the previously serialized current dropdown position.*/
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		/* Serialize the current dropdown position.*/
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/* Inflate the menu; this adds items to the action bar if it is present.*/
		getMenuInflater().inflate(R.menu.manual_control, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		return true;
	}
	
	public void setImageError(){
    	handler.post(new Runnable() {
    		@Override
    		public void run() {
    			Log.e("Image Error","");
    			return;
    		}
    	});
    }
	
    private class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;         
            DefaultHttpClient httpclient = new DefaultHttpClient(); 
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5*1000);
            HttpConnectionParams.setSoTimeout(httpParams, 5*1000);
            if(DEBUG) Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                if(DEBUG) Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());  
            } catch (ClientProtocolException e) {
            	if(DEBUG){
	                e.printStackTrace();
	                Log.d(TAG, "Request failed-ClientProtocolException", e);
            	}
                //Error connecting to camera
            } catch (IOException e) {
            	if(DEBUG){
	                e.printStackTrace();
	                Log.d(TAG, "Request failed-IOException", e);
            	}
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            if(result!=null){
            	result.setSkip(1);
            	setTitle(R.string.app_name);
            }else{
            	Log.e("Disconnected","");
            	//setTitle(R.string.title_disconnected);
            }
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(false);
        }
    }
    
    public class RestartApp extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... v) {
        	NeckControl.this.finish();
            return null;
        }

        protected void onPostExecute(Void v) {
        	startActivity((new Intent(NeckControl.this,NeckControl.class)));
        }
    }
}
