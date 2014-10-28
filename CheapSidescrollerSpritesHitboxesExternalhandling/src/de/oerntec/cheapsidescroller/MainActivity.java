package de.oerntec.cheapsidescroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements Handler.Callback {

	DrawingSurfaceView spaceView;
	JoystickView joystick;
	RelativeLayout gameLayout;
	Handler mHandler;
	SharedPreferences scorePreferences;
	private int scoreCount;
	
	private static final String TAG="sidescr main";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// force landscape view
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		// get a handler to be able to communicate with the main thread
		mHandler = new Handler(this);
		
		//scorekeeping
		scorePreferences=getSharedPreferences("score", MODE_MULTI_PROCESS);
		scoreCount=scorePreferences.getInt("scoreCount", 0);
		
		//layout containing the gameview
		gameLayout = (RelativeLayout) findViewById(R.id.gameLayout);
		
		createGame();
	}

	@Override
	public void onPause() {
		spaceView.killGame();
		super.onPause();
	}

	@Override
	public void onResume() {
		spaceView.resumeWithSprites();
		super.onPause();
	}
	
	/**
	 * kills a possible old game, and adds a new one
	 */
	private void createGame(){
		//remove the current game if existing
		if(gameLayout.getChildCount()==1)
			gameLayout.removeViewAt(0);
		
		// instantiate the gameview, and add it to the layout holding it
		spaceView = new DrawingSurfaceView(mHandler, this);
		gameLayout.addView(spaceView);
		setJoystickListener();
	}
	
	/**
	 * finds the joystick and connects the listener to the spaceview
	 */
	private void setJoystickListener() {
		joystick = (JoystickView) findViewById(R.id.joystick);
		joystick.setOnJoystickMovedListener(new JoystickMovedListener() {
			@Override
			public void OnMoved(int pan, int tilt) {
				spaceView.moveMySpaceShip(pan, tilt);
			}

			@Override
			public void OnReleased() {
				spaceView.joystickReleased();
			}
		});
	}

	/**
	 * Handle game message by showing either menu or a "you lost" dialog
	 */
	public boolean handleMessage(Message m) {
		spaceView.play(false);
		//the user lost
		if (m.what == DrawingSurfaceView.GAME_LOST) {
			//show the dialog
			handleGameEnd(m);
		}
		//the game should be paused, because the user clicked the button
		else if (m.what == DrawingSurfaceView.GAME_PAUSED) {
			showPauseMenu();
		}
		return true;
	}

	private void showPauseMenu() {
		Log.i(TAG, "trying to show menu");
		String[] choices = { "Zurück", "Neustart", "Hauptmenü" };
		//define dialog which-cases for easier reading
		final int RESUME=0, RESTART=1, MAIN_MENU=2;
		
		//create dialog
		AlertDialog mDialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Menü")
		.setItems(choices,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case RESUME:
								dialog.dismiss();
								Log.i(TAG, "trying to unpause the game");
								spaceView.play(true);
							break;
							case RESTART:
							case MAIN_MENU:
								//code for restarting
								dialog.dismiss();
								Log.i(TAG, "trying to restart the game");
								handleGameEnd(null);
							break;
						}
					}
				});
		mDialog=builder.create();
		mDialog.show();
	}
	
	/**
	 * Adds myName with "score" points; cuts off any scores over the count of 10
	 * @param score Points name made
	 * @param name The name to be entered into the highscores
	 */
	private void doScoreKeeping(int score, String name){
		//get score save editor
		Editor e=scorePreferences.edit();
		//get score list
		List <Integer> scores=new ArrayList<Integer>();
		for(int i=0;i<scoreCount;i++)
			scores.add(scorePreferences.getInt(""+i, 0));
		scores.add(score);
		//TODO: have to connect name and score for moving in highscore
		//this _should_ order the list by size
		Collections.sort(scores, new SortBySizeComparable());
		for(int i=0;i<scores.size();i++){
			e.putInt(String.valueOf(i)+"score", score);
		}
		e.putInt("scoreCount", scores.size());
		e.commit();
	}
	
	/**
	 * Checks where, and if to place the new score in the highscores
	 * @param newScore
	 * @return
	 */
	private int checkScoreForHighscore(int newScore){
		int scoreCount=scorePreferences.getInt("scoreCount", 0);
		if(scoreCount<11)
			return scoreCount;
		else{
			return -1;
		}
	}
	
	/**
	 * Shows the user a "you lost" dialog, and will eventually write to sprefs for scorekeeping
	 * @param score	the score the user made before dying
	 */
	private void showUserLostDialog(int score){
		Log.i(TAG, "user lost dialog");
		
		//TODO: Ask for name if score is higher than prev lowest
		
		
		//save score
		doScoreKeeping(score, "penis");
		
		//build dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Du hast mit "+score+" Punkten verloren!")
		.setPositiveButton("Neues Spiel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						//neues spiel
						createGame();
						spaceView.resumeWithSprites();
						dialog.dismiss();
					}
				})
		.setNegativeButton("Hauptmenü",
	            new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
						//neues spiel, weil ich noch kein hauptmenü habe
	                	createGame();
						spaceView.resumeWithSprites();
	                    dialog.dismiss();
	                }
	            }
	    )
	    .setCancelable(false);
		AlertDialog dialog=builder.create();
		dialog.show();
	}

	private void handleGameEnd(Message m) {
		//if the user has lost, we have message data
		if(m!=null){
			showUserLostDialog(m.arg1);
		}
		//just restart the game b/c restart command via menu
		else{
        	createGame();
			spaceView.resumeWithSprites();
		}
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
