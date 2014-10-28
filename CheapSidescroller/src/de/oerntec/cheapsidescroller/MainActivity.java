package de.oerntec.cheapsidescroller;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity{

	DrawingView spaceView;
	JoystickView joystick;
	
	//game clock (?)
	TimerTask updateTask;
	Timer myTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//force landscape view
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		//get the games drawing area
		spaceView=(DrawingView) findViewById(R.id.spaceView);
		
		//find joystick and implement its listener
		joystick=(JoystickView) findViewById(R.id.joystick);
		joystick.setOnJoystickMovedListener(new JoystickMovedListener(){

			@Override
			public void OnMoved(int pan, int tilt) {
				spaceView.moveMySpaceShip(pan, tilt);
			}

			@Override
			public void OnReleased() {
				spaceView.joystickReleased();
			}
			
		});
		
		//start the game continuing loop
		if(myTimer != null)
			myTimer.cancel();
		myTimer=new Timer();
		updateTask = new ContinueGameTask(spaceView, 5, this, myTimer);
		myTimer.schedule(updateTask, 0, 30);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
