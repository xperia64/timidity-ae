package com.xperia64.timidityae;

import android.app.Activity;
import android.os.Bundle;

public class DummyActivity extends Activity {
	@Override
	public void onCreate( Bundle potato ) {
		// Because JB's/KK's/LP's task handling is dumb.
		// Really Google? This bug is still present in Lollipop.
		// I should not have to create an Activity just to keep my service alive.
		super.onCreate( potato );
		// With and without this something breaks. 
		// With, it closes the task manager. 
		// Without, volume buttons break and lag occurs.
		this.finish(); 
	}
}