package de.oerntec.cheapsidescroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
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
	
	//main context
	Context mainContext;
	
	//game loop thread
	GameLoop gameThread;
	
	//check init
	public boolean isInitialized=false;
	
	//current score value
	public float score=0;
	
	//has the user lost?
	public boolean hasLost=false;
	
	//dimensions
	private int width, height;
	
	//spaceship movement delta
	private int spaceshipXDelta=0, spaceshipYDelta=0;
	
	//enemy creation timer
	private long lastEnemyCreation=0;
	
	//rectangles
	private Rect mySpaceShip=new Rect(), backgroundRect=new Rect();;
	private List<Rect> enemyRectangleList, bulletRectangleList;
	
	//bitmaps
	private Bitmap mySpaceShipSprite, enemySprite, missileSprite, backgroundSprite;//, canvasBitmap;
	
	//canvas n shit
	private Paint  mySpaceshipPaint, scorePaint, bulletPaint;
	//we now get the canvas from the loop
	//private Canvas drawCanvas;

	public DrawingSurfaceView(Context context, AttributeSet attrs){
		super(context, attrs);
		
		// adding the callback (this) to the surface holder to intercept events
		getHolder().addCallback(this);
		
		//get sprites from disk
		getSprites();
		
		// create the game loop thread
		gameThread = new GameLoop(this, 5);
		
		//initalize bullet and enemy lists
		enemyRectangleList=new ArrayList<Rect>();
		bulletRectangleList=new ArrayList<Rect>();
		
		//initialize bulletpaint
		bulletPaint=new Paint();
		bulletPaint.setColor(Color.BLACK);
		bulletPaint.setStyle(Paint.Style.FILL);
		
		//initialize my spaceship
		mySpaceShip.set(50, 50, 50+MY_SHIP_LENGTH, 50+MY_SHIP_HEIGHT);
		mySpaceshipPaint=new Paint();
		mySpaceshipPaint.setColor(Color.RED);
		mySpaceshipPaint.setStyle(Paint.Style.STROKE);
		mySpaceshipPaint.setStrokeWidth(3);
		
		//init enemy ships
		scorePaint=new Paint();
		scorePaint.setColor(Color.BLACK);
		scorePaint.setStyle(Paint.Style.FILL);
		scorePaint.setTextSize(40);
		scorePaint.setStrokeWidth(3);
		//canvasPaint = new Paint(Paint.DITHER_FLAG);
	}
	
	private void getSprites(){
		//initialize sprites
		mySpaceShipSprite=BitmapFactory.decodeResource(getResources(), R.drawable.my_spaceship_with_propulsion);
		enemySprite=BitmapFactory.decodeResource(getResources(), R.drawable.enemy_with_propulsion);
		missileSprite=BitmapFactory.decodeResource(getResources(), R.drawable.missile_with_propulsion);
		
		//background-"sprite"
		backgroundSprite=BitmapFactory.decodeResource(getResources(), R.drawable.background);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.i(TAG, "surface changed");
		width=w;
		height=h;
		backgroundRect.set(0, 0, width, height);
		//canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		//drawCanvas = new Canvas(canvasBitmap);
		//render();
		isInitialized=true;
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// tell the thread to shut down and wait for it to finish
		// this is a clean shutdown
		stopLoop();
	}
	
	/**
	 * tell the thread to shut down and wait for it to finish. this is a clean shutdown.
	 */
	private void stopLoop(){
		Log.d(TAG, "Surface is being destroyed");
		boolean retry = true;
		while (retry) {
			try {
				gameThread.join();
				retry = false;
			} catch (InterruptedException e) {
				// try again shutting down the thread
			}
		}
		Log.d(TAG, "Thread was shut down cleanly");
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// at this point the surface is created and
		// we can safely start the game loop
		startLoop();
	}
	
	private void startLoop(){
		if(!isInitialized){
			Log.i(TAG, "surface created");
			gameThread.setRunning(true);
			gameThread.start();
		}
		else
			Log.w(TAG, "surface created, but not initialized. did not start gameloop");
	}
	
	public void render(Canvas drawCanvas){
		if(!isInitialized){
			Log.w(TAG, "unitialized attempt to render");
			return;
		}
		//blank screen ... with sprite
		drawCanvas.drawBitmap(backgroundSprite, null, backgroundRect, null);
		
		//draw myship sprite
		drawCanvas.drawBitmap(mySpaceShipSprite, null, mySpaceShip, null);
	
		
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
	
	public void giveContext(Context c){
		mainContext=c;
	}
	
	public Context getMainContext(){
		return mainContext;
	}
	
	/**
	 * Resets the x and y deltas of the spaceship to zero, so it stops moving
	 */
	public void joystickReleased(){
		spaceshipXDelta=spaceshipYDelta=0;
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
		mySpaceShip.offset(spaceshipXDelta, spaceshipYDelta);
		
		/*
		 * if the spaceship has collided with an enemy, the user has lost.
		 */
		for(Rect enemy : enemyRectangleList)
			if(Rect.intersects(enemy, mySpaceShip)){
				resetGame();
				//must break out of this update
				return;
			}
		
		/*
		 * count score
		 */
		score+=0.04;
		
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
					score+=5;
				}
			}
		}
	}

	/**
	 * Restart the game cleanly after the user has lost
	 */
	private void resetGame(){
		//avoid any updating during reset
		gameThread.setRunning(false);
		isInitialized=false;
		
		//reget sprites
		getSprites();
		
		//reset myShip
		mySpaceShip.set(50, 50, 50+MY_SHIP_LENGTH, 50+MY_SHIP_HEIGHT);
		
		//clean lists
		bulletRectangleList.clear();
		enemyRectangleList.clear();
		
		//reset ints
		score=spaceshipXDelta=spaceshipYDelta=0;
		
		//reenable updating
		isInitialized=true;
		gameThread.setRunning(true);
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
			fireBullet();
			break;
		default:
			return false;
		}
		return true;
	}
	
	/**
	 * Fire a bullet at the front center of the Spaceship (maximum amount: MAX_BULLETS)
	 */
	private void fireBullet(){
		//abort if maximum bullet count has been reached
		if(bulletRectangleList.size()>MAX_BULLETS)
			return;
		
		//log because i fucked up
		//Log.i(TAG, "firing bullet");
		
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
		
	/* not needed in surfaceview
	@Override
	protected void onDraw(Canvas canvas) {
		try{
			canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
		}
		catch(Exception e){
			Log.w(TAG, "could not draw background");
		}
	}
	*/
}

