package com.example.roboskel;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class NeckControl extends FragmentActivity{
//TODO implement appropriate classes create sliders, send message to Sek

	private int rotate = 168;
	private int updown = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try{
			Intent intent = getIntent();
			String caller = intent.getStringExtra("caller");
			Window window = this.getWindow();
			window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
			if(caller.equals("menu")){
				this.setTheme(android.R.style.Theme_Light);
			}
		}
		catch(Exception e){
			Log.w("Warning", "Calling activity did not send any extras!");
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_neck_control);
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
}
