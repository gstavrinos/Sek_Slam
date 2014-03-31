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
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SimpleSensor extends FragmentActivity implements SensorEventListener,OnClickListener,
		ActionBar.OnNavigationListener 
{
	
	private SensorManager mSensorManager;
	/*Matrixes used to store values to calculate angle at each axis*/
	private float[] gravity,magnetic,azimuth,refAzimuth,currAzimuth;
	/*Matrix that holds canonical values in range [-1,1] of current position using as reference 
	point the one set by the user*/
	private float[] canonicalAz;
	/*Threshold Variable that controls the sensitivity of movement*/ 
	private final float E=4.0f;
	/*Maximum angle(the angle that canonicalizes the value sent to robot)*/
	private final float maxAngle=30.0f;
	/*Difference between last and current value*/
	private float DeltaX;
	/*Variable used to prevent subsequent break on/off uses*/
	private boolean useBreak;
	/*Sensors*/
	private Sensor accSensor,magnetSensor;
	private boolean initialized,referenceP;
	TextView z;
	/*variables used to verify whether user shakes device or not*/
	private float mAccel; /*acceleration apart from gravity*/
	private float mAccelCurrent; /* current acceleration including gravity */
	private float mAccelLast; /*last acceleration including gravity*/
    private boolean cartesian_on = false;
    private boolean polar_on = false;
    private boolean standard_on = false;
    private MjpegView mv = null;
	private int width = 320;
    private int height = 240;
    private String URL;
	private static final boolean DEBUG=true;
    private static final String TAG = "MJPEG";
	/*
	 * 
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_sensor);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		/* if sec is not streaming initiate streaming */
		if(!ActiveConnection.getConn().isStreaming())
			ActiveConnection.getConn().stream(false);
		/* get view for streaming */
		mv = (MjpegView) findViewById(R.id.mv);
		if(mv != null){
        	mv.setResolution(width, height);
        }

		/*Getting sensors*/
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		/*Initializing variables*/
		refAzimuth=new float[3];
		gravity=new float[3];
		magnetic=new float [3];
		initialized=false;
		referenceP=false;
		azimuth=new float[3];
		canonicalAz=new float[3];
		ActiveConnection.getConn().setPower(false);
		ActiveConnection.getConn().setOnPause(false);
		useBreak=true;
		
		/*Parameter to keep screen from locking*/
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		/*Send operation code 2 for sensor control*/
		try{
			ActiveConnection.getConn().stateAndSensitivity(2, 0.2f, 0.0f);
		}
		catch(Exception e){}
		
		mv.setVisibility(View.INVISIBLE);

		/*Initialize variables used to determine shake(emergency brake of robot)*/
		mAccel = 0.00f;
	    mAccelCurrent = SensorManager.GRAVITY_EARTH;
	    mAccelLast = SensorManager.GRAVITY_EARTH;
	    
	    Button ref=(Button)findViewById(R.id.referenceP);
	    ref.setOnClickListener(SimpleSensor.this);
	    z=(TextView)findViewById(R.id.textView1);
	    SeekBar s=(SeekBar)findViewById(R.id.Speed);
	    s.incrementProgressBy(10);
	    s.setMax(100);
	    s.setMinimumHeight(300);
	    s.setMinimumWidth(100);
	    s.setProgress(50);
	    s.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
	    	
	        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	    	{
	        	progress = progress / 10;
	            progress = (progress * 10-50)*2;
	            canonicalAz[2]=(float)progress/100.0f;
	            
	            Log.d("Progress",Integer.toString(progress));
	    	    ActiveConnection.getConn().send(canonicalAz);
	        }
	        
	        public void onStartTrackingTouch(SeekBar seekBar) {}

	        public void onStopTrackingTouch(SeekBar seekBar) 	
	        {Toast.makeText(getBaseContext(),"Speed : "+canonicalAz[2], Toast.LENGTH_SHORT).show();}
	    });
	}

	@Override
	public void onClick(View v) 
	{
		switch (v.getId()) 
		{
			/*User defines point 0,0,0*/
	        case R.id.referenceP:
	        {
	        	
				referenceP=true;
				System.arraycopy(azimuth, 0,refAzimuth, 0, 3);
				ActiveConnection.getConn().setPower(true);
				canonicalAz[0]=0.0f;
				canonicalAz[1]=0.0f;
				canonicalAz[2]=0.0f;
				ActiveConnection.getConn().send(canonicalAz);
				break;
	        }
		}
	}
	
	@Override
	public void onSensorChanged(SensorEvent e)
	{
		switch(e.sensor.getType())
		{
			/*get gravity value arrays from Magnet
			 * in order to get orientation(e,w,s,n)*/
			case(Sensor.TYPE_MAGNETIC_FIELD):
			{
				System.arraycopy(e.values, 0, magnetic, 0, 3);
				break;
			}
			/*get gravity value arrays from Accelerometer
			 * define which way is up/down,identify shake event*/
			case(Sensor.TYPE_ACCELEROMETER):
			{
				System.arraycopy(e.values, 0, gravity, 0, 3);
				mAccelLast = mAccelCurrent;
				/*Using only Z axis*/
				mAccelCurrent = (float) Math.sqrt((double) (e.values[2]*e.values[2]+e.values[1]*e.values[1]));
			    float delta = mAccelCurrent - mAccelLast;
			    mAccel = mAccel * 0.8f + delta; // perform low-cut filter
				if(mAccel>7.0 && useBreak)
				{
					useBreak=false;
					Thread t=new Thread(){public void run(){try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {} useBreak=true;}};
					t.start();
					
					if(ActiveConnection.getConn().isPower())
					{
						Toast.makeText(getBaseContext(),"Emergency brake used at : "+mAccel, Toast.LENGTH_SHORT).show();
						ActiveConnection.getConn().setPower(false);
						ActiveConnection.getConn().stop();
					}
					else
					{
						Toast.makeText(getBaseContext(),"Resuming function", Toast.LENGTH_SHORT).show();
						ActiveConnection.getConn().setPower(true);
					}
				}
				break;
			}
		}
		
		if (gravity != null && magnetic != null) 
		{
			/*Rotation matrix and Inclination matrix*/
			float R[] = new float[9];
			float I[] = new float[9];
			/* Compute the inclination matrix I as well as the rotation matrix R 
			transforming a vector from the device 
			coordinate system to the world's coordinate system 
			R and I [Length 9]
			gravity vector expressed in the device's coordinate [Length 3]
			geoMagnetic vector expressed in the device's coordinate[Length 3]
			*/
			if(SensorManager.getRotationMatrix(R, I,gravity, magnetic))
			{
				currAzimuth = new float[3];
				SensorManager.getOrientation(R, currAzimuth);
				if(!initialized)
				{
					DeltaX=0.0f;
					System.arraycopy(currAzimuth, 0,azimuth, 0, 3);
					initialized=true;
				}
				else
				{
					/*Convert to degrees positive in the counter-clockwise direction
					  azimuth, rotation around the Z axis and calculate Delta*/
					DeltaX=(float)(Math.abs(Math.toDegrees(currAzimuth[1]-azimuth[1])));
					
				}
				/*If the variation of the angle exceeds E(minimum variation)
				in at least one axis*/
				if((DeltaX>E) )//|| (DeltaY>E))// || (DeltaZ>E))
				{	
					System.arraycopy(currAzimuth, 0,azimuth, 0, 3);
					if(!referenceP)
					{/*
						z.setText(Float.toString((float)(Math.toDegrees(azimuth[0]))));
						pitch, rotation around the X axis
						x.setText(Float.toString((float)(Math.toDegrees(azimuth[1]))));
						roll, rotation around the Y axis
						y.setText(Float.toString((float)(Math.toDegrees(azimuth[2]))));*/
					}
					else
					{
						canonicalAz[1]=-(Math.round(((float)cannAz(refAzimuth[1],azimuth[1])/maxAngle)*100.0f))/100.0f;
						
						ActiveConnection.getConn().send(canonicalAz);
						z.setText(Float.toString(canonicalAz[1]));
					}
				}
			}
		}
	}
	
	private int cannAz(float r,float az)
	{
		/*if(r*az)>0 or |r|+|az|<=180 return az-r*/
		return (int)Math.round(Math.toDegrees(((r*az>0.0f) || (Math.abs(r)+Math.abs(az)<=(float)Math.PI) ? az-r:( (r>0.0f)&&(az<0.0f) ? ((float)2.0*Math.PI-r+az):(az-r-(float)2.0*Math.PI)))));
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();  
		/* Register the sensor listeners */
		mSensorManager.registerListener(this, accSensor,SensorManager.SENSOR_DELAY_NORMAL);
	    mSensorManager.registerListener(this, magnetSensor,SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		mSensorManager.unregisterListener(this);
		
		//ActiveConnection.getConn().setPower(false);
		ActiveConnection.getConn().pause();
		
		//finish();
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
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
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_sensor, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		
		return true;
	}
		
		@Override
		 public boolean onOptionsItemSelected(MenuItem item) 
		{
			 /*super.onOptionsItemSelected(item);*/
			    	
			 switch(item.getItemId())
			 {
				case R.id.camera:
					// if the view is not visible
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
				case R.id.sliders:
					Intent a=new Intent(getApplicationContext(),NeckControl.class);
					a.putExtra("caller", "simple_sensor");
					startActivityForResult(a,1);
						break;
			 }
			 
			 return true;
		}
		 
		 //do something when the other intent, comes back here
		 @Override
		 public void onActivityResult(int requestCode, int resultCode, Intent data){
			 try{
				ActiveConnection.getConn().stateAndSensitivity(2, 0.2f, 0.0f);	
			 }
			 catch(Exception e){}
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

	
}
