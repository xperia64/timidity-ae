package com.xperia64.timidityae;

import android.app.Activity;
import android.os.Bundle;

public class DummyActivity extends Activity {
	@Override
	public void onCreate( Bundle potato ) {
		// Because JB's/KK's task handling is dumb.
		super.onCreate( potato );
		finish();
	}
}