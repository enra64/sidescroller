package de.oerntec.cheapsidescroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class DrawingSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
	private static final String TAG = "DrawingViewTag";
	private final int MAX_ENEMIES=20;
	private final int MAX_BULLETS=5;
	private final int MISSILE_HEIGHT=15;
	private final int MISSILE_LENGTH=55;
	private final int MY_SHIP_HEIGHT=80;
	private final int MY_SHIP_LENGTH=185;
	private final int ENEMY_LENGTH=150;
	private final int ENEMY_HEIGHT=75;
	private final int ENEMY_CREATION_DELAY=500;
	private final int BUTTON_SIZE=100;
	
	public final static int GAME_PAUSED	=0xA1;
	public final static int GAME_LOST	=0xA2;
	
	//main context
	Context mainContext;
	
	//game loop thread
	GameLoop gameThread;
	
	//check init
	public boolean isInitialized=false, isRfunning=false;
	
	//scorekeeping system
	public int score=0, killScore=0, ticks=0;
	
	//dimensions
	private int width, height;
	
	//spaceship movement delta
	private int spaceshipXDelta=0, spaceshipYDelta=0;
	
	//enemy creation timer
	private long lastEnemyCreation=0;
	
	//rectangles
	private Rect mySpaceShip=new Rect(), backgroundRect=new Rect(), pauseRect=new Rect(), restartRect=new Rect();
	private List<Rect> enemyRectangleList, bulletRectangleList;
	
	//bitmaps
	private Bitmap mySpaceShipSprite, enemySprite, missileSprite, backgroundSprite, playSprite, pauseSprite, restartSprite;//, canvasBitmap;
	
	//paints
	private Paint scorePaint;
	
	//comm handler
	Handler mainHandler;
	
	public DrawingSurfaceView(Handler h, Context c){
		super(c);
		
		mainHandler=h;
		
		// adding the callback (this) to the surface holder to intercept events
		getHolder().addCallback(this);
		
		// create the game loop thread
		gameThread = new GameLoop(this, 5);
		
		//create controls
		pauseRect.set(0, 0, BUTTON_SIZE, BUTTON_SIZE);
		restartRect.set(BUTTON_SIZE, 0, BUTTON_SIZE*2, BUTTON_SIZE);
		
		//initalize bullet and enemy lists
		enemyRectangleList=new ArrayList<Rect>();
		bulletRectangleList=new ArrayList<Rect>();
		
		//initialize my spaceship
		mySpaceShip.set(50, 50, 50+MY_SHIP_LENGTH, 50+MY_SHIP_HEIGHT);
		
		//init enemy ships
		scorePaint=new Paint();
		scorePaint.setColor(Color.BLACK);
		scorePaint.setStyle(Paint.Style.FILL);
		scorePaint.setTextSize(40);
		scorePaint.setStrokeWidth(3);
		//canvasPaint = new Paint(Paint.DITHER_FLAG);
	}
	

	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.i(TAG, "surface changed");
		width=w; height=h;
		getSprites();
		backgroundRect.set(0, 0, w, h);
		isInitialized=true;
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// tell the thread to shut down and wait for it to finish
		// this is a clean shutdown
		stopLoop();
	}
	

	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// at this point the surface is created and
		// we can safely start the game loop
		startLoop();
	}
	
	/**
	 * tell the thread to shut down and wait for it to finish. this is a clean shutdown.
	 */
	private void stopLoop(){
		Log.d(TAG, "stopLoop: killing gamethread");
		gameThread.setKilled(true);
		boolean retry = true;
		while (retry) {
			try {
				gameThread.join();
				retry = false;
			} catch (InterruptedException e) {
				// try again shutting down the thread
				Log.w(TAG, "could not kill gamethread cleanly");
			}
		}
		Log.d(TAG, "Thread was shut down cleanly");
	}
	
	/**
	 * sets the "running" variables, and actually starts the gamethread if "isInitialized" is true
	 */
	private void startLoop(){
		if(isInitialized){
			Log.i(TAG, "surface created");
			gameThread.setRunning(true);
			gameThread.start();
		}
		else
			Log.w(TAG, "surface created, but not initialized. did not start gameloop");
	}
	
	/**
	 * reloads the sprites, because they get lost;
	 * calls startloop
	 */
	public void resumeWithSprites(){
		getSprites();
		startLoop();
		isInitialized=true;
		gameThread.setRunning(true);
	}
	
	public void play(boolean run){
		isInitialized=true;
		gameThread.setRunning(run);
		if(run){
			gameThread.run();
			//setFocusable(true);
			requestFocus();
		}
	}
	
	public void killGame(){
		//stop gamethread execution
		gameThread.setKilled(true);
		gameThread.setRunning(false);
		//try killing the gamethread now
		stopLoop();
	}
	
	/**
	 * Move everything according to the passed time
	 * @param speedDelta A constant factor determining the gamespeed.
	 */
	public void update(int speedDelta){
		//abort if not initialized
		if(!isInitialized)
			return;
		
		/*
		 * move the spaceship while the joystick is active
		 */
		moveMySpaceShip(spaceshipXDelta, spaceshipYDelta);
		mySpaceShip.offset(spaceshipXDelta, spaceshipYDelta);
		
		/*
		 * ******************************* USER LOST **************************
		 * if the spaceship has collided with an enemy, the user has lost.
		 */
		for(Rect enemy : enemyRectangleList)
			if(Rect.intersects(enemy, mySpaceShip)){
				gameThread.setRunning(false);
				//send message to main 		*handler*   *what*		*arg1* *2* *random object i need*
				Message msg= Message.obtain(mainHandler, GAME_LOST, score, 0, null);
				mainHandler.sendMessage(msg);
				//must break out of this update
				return;
			}
		
		/*
		 * count score
		 */
		ticks++;
		score=ticks/35+killScore;
		
		/*
		 * move all bullets
		 */
		for (int index=0; index<bulletRectangleList.size(); index++){
			Rect currentBullet = bulletRectangleList.get(index);
			currentBullet.offset(speedDelta*2, 0);
			bulletRectangleList.set(index, currentBullet);
		}
		
		/*
		 * if we have less than 10 enemies, add one after ENEMY_CREATION_DELAY
		 */
		if( (System.currentTimeMillis()-lastEnemyCreation) > ENEMY_CREATION_DELAY){
			lastEnemyCreation=System.currentTimeMillis();
			addEnemy();
		}
		
		/*
		 * move all enemies
		 */
		for (int index=0; index<enemyRectangleList.size(); index++){
			Rect currentEnemy = enemyRectangleList.get(index);
			currentEnemy.offset(-speedDelta, 0);
			enemyRectangleList.set(index, currentEnemy);
		}
		
		/*
		 * delete bullets if out of display
		 */
		for (int index=0; index<bulletRectangleList.size(); index++){
			Rect currentBullet = bulletRectangleList.get(index);
			if(currentBullet.left>width)
				bulletRectangleList.remove(index);
		}
		
		/*
		 * delete enemies if out of view
		 */
		for (int index=0; index<enemyRectangleList.size(); index++){
			Rect currentEnemy = enemyRectangleList.get(index);
			if(currentEnemy.right<0)
				enemyRectangleList.remove(index);
		}
				
		/*
		 * collision checking:
		 * if currentEnemy contains currentBullet: delete both
		 */
		for (int bulletIndex=0; bulletIndex<bulletRectangleList.size(); bulletIndex++){
			Rect currentBullet = bulletRectangleList.get(bulletIndex);
			for (int enemyIndex=0; enemyIndex<enemyRectangleList.size(); enemyIndex++){
				Rect currentEnemy = enemyRectangleList.get(enemyIndex);
				if(Rect.intersects(currentBullet, currentEnemy)){
					//a bullet can hit multiple targets
					try{
						bulletRectangleList.remove(bulletIndex);
					}
					catch(IndexOutOfBoundsException e){
						Log.w(TAG, "Two ships hit");
					}
					enemyRectangleList.remove(enemyIndex);
					killScore+=5;
				}
			}
		}
	}
	
	/**
	 * Draw all bitmaps according to the values set in update
	 * @param drawCanvas The Canvas to draw unto
	 */
	public void render(Canvas drawCanvas){
		if(!isInitialized){
			Log.w(TAG, "unitialized attempt to render");
			return;
		}
		//blank screen ... with sprite
		drawCanvas.drawBitmap(backgroundSprite, null, backgroundRect, null);

		//draw myship sprite
		drawCanvas.drawBitmap(mySpaceShipSprite, null, mySpaceShip, null);
		
		//draw controls
		if(gameThread.isRunning())
			drawCanvas.drawBitmap(pauseSprite, null, pauseRect, null);
		else
			drawCanvas.drawBitmap(playSprite, null, pauseRect, null);
		
		//dont draw restart icon, for it is in the menu
		//drawCanvas.drawBitmap(restartSprite, null, restartRect, null);
		
		//draw score
		drawCanvas.drawText(String.valueOf(score), width-200, 50, scorePaint);
		
		//draw bullets
		for(Rect missile: bulletRectangleList)
			drawCanvas.drawBitmap(missileSprite, null, missile, null);
		
		//draw enemies
		for(Rect enemy: enemyRectangleList)
			drawCanvas.drawBitmap(enemySprite, null, enemy, null);
	}
	
	/**
	 * Adds 
	 * @param x the amount of x the ship should be moved
	 * @param y the amount of y the ship should be moved
	 */
	public void moveMySpaceShip(int x, int y){
		//move the ship, except if it would go out of view
		if((mySpaceShip.left+x) >=0 && (mySpaceShip.right+x)<width)
			spaceshipXDelta=x;
		else
			spaceshipXDelta=0;
		
		if((mySpaceShip.top+y) >=0 && (mySpaceShip.bottom+y)<height)
			spaceshipYDelta=y;
		else
			spaceshipYDelta=0;
	}
	
	/**
	 * Resets the x and y deltas of the spaceship to zero, so it stops moving
	 */
	public void joystickReleased(){
		spaceshipXDelta=spaceshipYDelta=0;
	}
	
	/**
	 * Add an enemy at a random position in the last third of the game; avoid collisions. 
	 * Do not execute if the game has not been initialized yet
	 */
	private void addEnemy(){
		if(enemyRectangleList.size()<MAX_ENEMIES){
			Random r=new Random();
			Rect newEnemy=new Rect();
			int enemyTop, enemyBottom, enemyLeft, enemyRight;
			boolean positionOk=true;
			
			//do this as long as we have a collision
			do{
			//random left position in the last third of the game minus the enemys length
			enemyLeft=r.nextInt( (width/3)-ENEMY_LENGTH ) + ( (2*width) /3);
			enemyRight=enemyLeft+ENEMY_LENGTH;
			
			//random top position
			enemyBottom=r.nextInt(height);
			//avoid ships above the field
			if(enemyBottom<=ENEMY_HEIGHT)
				enemyBottom+=ENEMY_HEIGHT;
			enemyTop=enemyBottom-ENEMY_HEIGHT;
			
			//set position of rect
			newEnemy.set(enemyLeft, enemyTop, enemyRight, enemyBottom);
			positionOk=true;
			
			//check enemy collision
			for(Rect checkRect : enemyRectangleList)
				if(Rect.intersects(checkRect, newEnemy))
					positionOk=false;
			
			//check bullet collision
			for(Rect checkRect : bulletRectangleList)
				if(Rect.intersects(checkRect, newEnemy))
					positionOk=false;
			
			//check myShip collision
			if(mySpaceShip.contains(newEnemy))
				positionOk=false;
			}
			while (!positionOk);
			
			//finally, add the new enemy
			enemyRectangleList.add(newEnemy);
		}
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event){
		int action = event.getAction();
		// decide how to handle click
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			handleTouch(event);
			break;
		default:
			return false;
		}
		return true;
	}
	
	private void handleTouch(MotionEvent e){
		int x=(int)e.getX();
		int y = (int)e.getY();
		
		//check whether the pause button has been clicked
		if(pauseRect.contains(x, y)){
			Message msg= Message.obtain(mainHandler, GAME_PAUSED);
			mainHandler.sendMessage(msg);
		}
		
		//fires bullet if screen has been touched
		else if(gameThread.isRunning()){
			Log.i(TAG, "firing bullet");
			fireBullet();
		}
	}
	
	/**
	 * Fire a bullet at the front center of the Spaceship (maximum amount: MAX_BULLETS)
	 */
	private void fireBullet(){
		//abort if maximum bullet count has been reached or the game is paused
		if(bulletRectangleList.size()>MAX_BULLETS)
			return;
		
		//create bullets
		Rect newBullet=new Rect();
		int bulletLeft=mySpaceShip.right, bulletRight=bulletLeft+MISSILE_LENGTH, bulletBottom, bulletTop;
		
		//vertically center bullet
		bulletTop=mySpaceShip.top+( ( (mySpaceShip.bottom-mySpaceShip.top) /2 ) - MISSILE_HEIGHT /2 );
		bulletBottom=bulletTop+MISSILE_HEIGHT;
		
		//add bullet to list
		newBullet.set(bulletLeft, bulletTop, bulletRight, bulletBottom);
		bulletRectangleList.add(newBullet);
	}
	
	/**
	 * decode sprites from resources, scale them
	 */
	private void getSprites(){
		//decoding
		if(true){
			//initialize sprites
			mySpaceShipSprite=BitmapFactory.decodeResource(getResources(), R.drawable.my_spaceship_with_propulsion);
			enemySprite=BitmapFactory.decodeResource(getResources(), R.drawable.enemy_with_propulsion);
			missileSprite=BitmapFactory.decodeResource(getResources(), R.drawable.missile_with_propulsion);
			
			//background-"sprite"
			backgroundSprite=BitmapFactory.decodeResource(getResources(), R.drawable.background);
			
			//control "sprite"s
			playSprite=BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_play);
			pauseSprite=BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_pause);
			restartSprite=BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_replay);
		}
		//scaling intensifies
		if(true){
			mySpaceShipSprite=Bitmap.createScaledBitmap(mySpaceShipSprite, MY_SHIP_LENGTH, MY_SHIP_HEIGHT, false);
			enemySprite=Bitmap.createScaledBitmap(enemySprite, ENEMY_LENGTH, ENEMY_HEIGHT, false);
			missileSprite=Bitmap.createScaledBitmap(missileSprite, MISSILE_LENGTH, MISSILE_HEIGHT, false);
			if(isInitialized)
				backgroundSprite=Bitmap.createScaledBitmap(backgroundSprite, width, height, false);
			playSprite=Bitmap.createScaledBitmap(playSprite, BUTTON_SIZE, BUTTON_SIZE, false);
			pauseSprite=Bitmap.createScaledBitmap(pauseSprite, BUTTON_SIZE, BUTTON_SIZE, false);
			restartSprite=Bitmap.createScaledBitmap(restartSprite, BUTTON_SIZE, BUTTON_SIZE, false);
		}
	}
}

