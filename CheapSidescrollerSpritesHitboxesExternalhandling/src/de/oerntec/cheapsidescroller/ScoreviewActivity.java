package de.oerntec.cheapsidescroller;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ScoreviewActivity extends Activity {
	private ListView scoreListView;
	SharedPreferences scorePreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//the usual stuff
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scoreview);
		//scorekeeping shared preferences
		scorePreferences=getSharedPreferences("score", MODE_MULTI_PROCESS);
		//check for having to display a dialog+
		Bundle b = getIntent().getExtras();
		boolean displayDialog=b.getBoolean("displayDialog");
		if(displayDialog){
			
		}
		//get score strings
		int scoreCount=scorePreferences.getInt("scoreCount", 0);
		List<String> scoreList=new ArrayList<String>();
		for(int i=0;i<scoreCount;i++){
			int score=scorePreferences.getInt(String.valueOf(i), 0);
			scoreList.add(i+": "+score);
		}
		//initialize adapter, set it
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this, 
                android.R.layout.simple_list_item_1,
                scoreList );
		scoreListView=(ListView) findViewById(R.id.scoreList);
		scoreListView.setAdapter(arrayAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.scoreview, menu);
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
