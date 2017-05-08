package br.usp.ime.compmus.pushloop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setupButtons();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.menu_item_settings:
			Intent intentSettings = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(intentSettings);
			break;

		default:
			break;
		}
		return true;
	}
	
	private void setupButtons() {
		
		Button pushButton = (Button) findViewById(R.id.buttonPush);
		pushButton.setOnClickListener(onClickListenerButtonPush());
		
		Button loopButton = (Button) findViewById(R.id.buttonLoop);
		loopButton.setOnClickListener(onClickListenerButtonLoop());
	}
	
	private View.OnClickListener onClickListenerButtonPush() {
		
		return new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intentPush = new Intent(MainActivity.this, PushActivity.class);
				startActivity(intentPush);
			}
		};
	}
	
	private View.OnClickListener onClickListenerButtonLoop() {
		
		return new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intentLoop = new Intent(MainActivity.this, LoopActivity.class);
				startActivity(intentLoop);
			}
		};
	}
}
