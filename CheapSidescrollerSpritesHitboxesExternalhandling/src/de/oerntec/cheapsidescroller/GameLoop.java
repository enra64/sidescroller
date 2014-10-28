/**
 * 
 */
package de.oerntec.cheapsidescroller;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;


/**
 * @author impaler
 *
 * The Main thread which contains the game loop. The thread must have access to 
 * the surface view and holder to trigger events every game tick.
 */
public class GameLoop extends Thread {
	
	private static final String TAG = GameLoop.class.getSimpleName();
	
	Context mainContext;
	
	// desired fps
	private final static int 	MAX_FPS = 45;	
	// maximum number of frames to be skipped
	private final static int	MAX_FRAME_SKIPS = 5;	
	// the frame period
	private final static int	FRAME_PERIOD = 1000 / MAX_FPS;	

	
	// Surface holder that can access the physical surface
	private SurfaceHolder surfaceHolder;
	// The actual view that handles inputs
	// and draws to the surface
	private DrawingSurfaceView drawView;
	int speedDelta;

	// flags to hold game state 
	public boolean mRunning;
	private boolean killed=false;
	
	public void setRunning(boolean running) {
		this.mRunning = running;
	}
	
	public boolean isRunning(){
		return this.mRunning;
	}
	
	public void setKilled(boolean killState) {
		this.killed = killState;
	}
	
	public GameLoop(DrawingSurfaceView dv, int sd) {
		this.drawView = dv;
		this.speedDelta = sd;
		this.surfaceHolder = dv.getHolder();
		Log.i(TAG, "GameLoop constructor called");
	}

	@Override
	public void run() {
		Canvas canvas;
		Log.d(TAG, "Starting game loop");

		long beginTime;		// the time when the cycle began
		long timeDiff;		// the time it took for the cycle to execute
		int sleepTime;		// ms to sleep (<0 if we're behind)
		int framesSkipped;	// number of frames being skipped 
		
		@SuppressWarnings("unused")
		boolean log=false;
		
		sleepTime = 0;
		
		if(mRunning){
			log=false;
			Log.i(TAG, "running...");
		}
		else{
			Log.w(TAG, "GameLoop not running!");
			log=true;
		}
		
		while(!killed){
			while (mRunning) {
				if(mRunning){
					canvas = null;
					// try locking the canvas for exclusive pixel editing
					// in the surface
					try {
						canvas = this.surfaceHolder.lockCanvas();
						synchronized (surfaceHolder) {
							beginTime = System.currentTimeMillis();
							framesSkipped = 0;	// resetting the frames skipped
							// update game state 
							this.drawView.update(speedDelta);
							// render state to the screen
							// draws the canvas on the panel
							this.drawView.render(canvas);				
							// calculate how long did the cycle take
							timeDiff = System.currentTimeMillis() - beginTime;
							// calculate sleep time
							sleepTime = (int)(FRAME_PERIOD - timeDiff);
							
							if (sleepTime > 0) {
								// if sleepTime > 0 we're OK
								try {
									// send the thread to sleep for a short period
									// very useful for battery saving
									Thread.sleep(sleepTime);	
								} catch (InterruptedException e) {}
							}
							
							while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
								Log.w(TAG, "had to skip frames!");
								// we need to catch up
								this.drawView.update(speedDelta); // update without rendering
								sleepTime += FRAME_PERIOD;	// add frame period to check if in next frame
								framesSkipped++;
							}
						}
					} 
					finally {
						// in case of an exception the surface is not left in 
						// an inconsistent state
						if (canvas != null) {
							surfaceHolder.unlockCanvasAndPost(canvas);
						}
					}	// end finally
				}
			}
		}
	}
	
}
