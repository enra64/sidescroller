package de.oerntec.cheapsidescroller;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;

public class ContinueGameTask extends TimerTask {
	DrawingView drawView;
	int speedDelta;
	Activity mainActivityReference;
	Timer iAmThisTimer;

	public ContinueGameTask(DrawingView dv, int sd, Activity act, Timer t) {
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
				drawView.continueGame(speedDelta);
			}
		});
		if(drawView.hasLost){
			iAmThisTimer.cancel();
			iAmThisTimer.purge();
		}
			
	}
}
