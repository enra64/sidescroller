package de.oerntec.cheapsidescroller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class DrawingView extends View{
	//private final String TAG="drawingView";
	
	private final int MAX_ENEMIES=10;
	private final int MAX_BULLETS=10;
	private final int BULLET_SIZE=10;
	private final int MY_SHIP_SIZE=50;
	private final int ENEMY_LENGTH=100;
	private final int ENEMY_HEIGHT=50;
	private final int ENEMY_CREATION_DELAY=500;
	
	//check init
	public boolean isInitialized=false;
	
	//has the user lost?
	public boolean hasLost=false;
	
	//dimensions
	int width, height;
	
	//spaceship movement delta
	int spaceshipXDelta=0, spaceshipYDelta=0;
	
	//enemy creation timer
	long lastEnemyCreation=0;
	
	//rectangles
	Rect mySpaceShip=new Rect();
	List<Rect> enemyRectangleList, bulletRectangleList;
	
	//canvas n shit
	private Paint  canvasPaint, mySpaceshipPaint, enemySpaceshipPaint, bulletPaint;
	private Canvas drawCanvas;
	private Bitmap canvasBitmap;

	public DrawingView(Context context, AttributeSet attrs){
		super(context, attrs);
		
		//initalize bullet and enemy lists
		enemyRectangleList=new ArrayList<Rect>();
		bulletRectangleList=new ArrayList<Rect>();
		
		//initialize bulletpaint
		bulletPaint=new Paint();
		bulletPaint.setColor(Color.BLACK);
		bulletPaint.setStyle(Paint.Style.FILL);
		
		//initialize my spaceship
		mySpaceShip.set(50, 50, 50+MY_SHIP_SIZE, 50+MY_SHIP_SIZE);
		mySpaceshipPaint=new Paint();
		mySpaceshipPaint.setColor(Color.GREEN);
		mySpaceshipPaint.setStyle(Paint.Style.STROKE);
		mySpaceshipPaint.setStrokeWidth(3);
		
		//init enemy ships
		enemySpaceshipPaint=new Paint();
		enemySpaceshipPaint.setColor(Color.RED);
		enemySpaceshipPaint.setStyle(Paint.Style.STROKE);
		enemySpaceshipPaint.setStrokeWidth(3);
		canvasPaint = new Paint(Paint.DITHER_FLAG);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		//view given size
		super.onSizeChanged(w, h, oldw, oldh);
		width=w;
		height=h;
		canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		drawCanvas = new Canvas(canvasBitmap);
		redraw();
		isInitialized=true;
	}
	
	private void redraw(){
		//blank screen
		drawCanvas.drawColor(Color.WHITE);
		
		//draw simple rectangle for movement testing
		drawCanvas.drawRect(mySpaceShip, mySpaceshipPaint);
		
		//draw bullets
		for(Rect bullet: bulletRectangleList)
			drawCanvas.drawRect(bullet, bulletPaint);
		
		//draw enemies
		for(Rect enemy: enemyRectangleList)
			drawCanvas.drawRect(enemy, bulletPaint);
		
		//reeeedraw
		invalidate();
	}
	
	/**
	 * Adds 
	 * @param x the amount of x the ship should be moved
	 * @param y the amount of y the ship should be moved
	 */
	public void moveMySpaceShip(int x, int y){
		//move the ship, except if it would go out of view
		if((mySpaceShip.left+x) >=0 && (mySpaceShip.top+y)>=0 && (mySpaceShip.right+x)<width && (mySpaceShip.bottom+y)<height){
			spaceshipXDelta=x;
			spaceshipYDelta=y;
		}
		else{
			spaceshipXDelta=0;
			spaceshipYDelta=0;
		}
		redraw();
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
	public void continueGame(int speedDelta){
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
			if(Rect.intersects(enemy, mySpaceShip))
				hasLost=true;
		
		/*
		 * count score
		 */
		
		
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
		 * todo: 
		 */
		if( (System.currentTimeMillis()-lastEnemyCreation) > ENEMY_CREATION_DELAY)
			addEnemy();
		
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
					bulletRectangleList.remove(bulletIndex);
					enemyRectangleList.remove(enemyIndex);
				}
			}
		}
		redraw();
	}
	
	/**
	 * Add an enemy at a random position in the last third of the game; avoid collisions. Do not execute if the game has not been initialized yet
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
		if(bulletRectangleList.size()>9)
			return;
		
		//create bullets
		Rect newBullet=new Rect();
		int bulletLeft=mySpaceShip.right, bulletRight=bulletLeft+BULLET_SIZE, bulletBottom, bulletTop;
		
		//vertically center bullet
		bulletTop=mySpaceShip.top+( ( (mySpaceShip.bottom-mySpaceShip.top) /2 ) - BULLET_SIZE /2 );
		bulletBottom=bulletTop+BULLET_SIZE;
		
		//add bullet to list
		newBullet.set(bulletLeft, bulletTop, bulletRight, bulletBottom);
		bulletRectangleList.add(newBullet);
	}
		
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
	}
}

