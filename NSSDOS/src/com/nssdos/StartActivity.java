package com.nssdos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

public class StartActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		
		
		setContentView(R.layout.start_layout);

		
		Thread splash = new Thread() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				try {
					sleep(1000);
					Intent i = new Intent(StartActivity.this, MapActivity.class);

					startActivity(i);
					finish();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

		};
		splash.start();

	
	}

}
