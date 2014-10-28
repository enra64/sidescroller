package de.oerntec.cheapsidescroller;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity{

	DrawingSurfaceView spaceView;
	JoystickView joystick;
	
	//game clock (?)
	GameLoop gameThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//force landscape view
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		//get the games drawing area
		spaceView=(DrawingSurfaceView) findViewById(R.id.gameview);
		
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
		
		/* gameloop baad
		//start the game continuing loop
		if(myTimer != null)
			myTimer.cancel();
		myTimer=new Timer();
		updateTask = new ContinueGameTask(spaceView, 5, this, myTimer);
		myTimer.schedule(updateTask, 0, 30);
		*/
		//
		/*
		//add drawing surface view
		RelativeLayout mainLayout=(RelativeLayout) findViewById(R.id.mainLayout);
		mainLayout.addView(new DrawingSurfaceView(this));
		//bring the highscore and the joystick view to the back
		mainLayout.bringChildToFront(mainLayout.getChildAt(0));
		mainLayout.bringChildToFront(mainLayout.getChildAt(0));*/
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
