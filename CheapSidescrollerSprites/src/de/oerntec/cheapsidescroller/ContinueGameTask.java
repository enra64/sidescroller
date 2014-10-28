package de.oerntec.cheapsidescroller;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.widget.TextView;

//@SuppressWarnings("unused")
public class ContinueGameTask extends TimerTask {
	DrawingSurfaceView drawView;
	int speedDelta;
	Activity mainActivityReference;
	Timer iAmThisTimer;

	public ContinueGameTask(DrawingSurfaceView dv, int sd, Activity act, Timer t) {
		drawView = dv;
		speedDelta = sd;
		mainActivityReference=act;
		iAmThisTimer=t;
	}

	@Override
	public void run() {
		mainActivityReference.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView highscoreView = (TextView)mainActivityReference.findViewById(R.id.highscoreTextView);
				int score=(int) drawView.score;
				highscoreView.setText(String.valueOf(score));
				drawView.update(speedDelta);
			}
		});
		if(drawView.hasLost){
			iAmThisTimer.cancel();
			iAmThisTimer.purge();
		}
			
	}
}
