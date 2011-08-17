package edu.rit.cs.perception;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PreviewActivity extends Activity {

	/**
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preview_layout);

		Bundle extras = getIntent().getExtras();
		String name = extras.getString("name");
		String address = extras.getString("address");
		int port = extras.getInt("port");
	}

}
