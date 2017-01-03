/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

public class TimidityAEWidgetProvider extends AppWidgetProvider {

	String currTitle = "";
	boolean paused = true;
	boolean art = false;
	boolean setdeath = false;
	static double maxBitmap = -1;

	@Override
	public void onEnabled(Context context) {

		AppWidgetManager mgr = AppWidgetManager.getInstance(context);

		Intent i = new Intent(context, TimidityActivity.class);
		PendingIntent myPI = PendingIntent.getActivity(context, 99, i, 0);

		// Get the layout for the App Widget
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

		//attach the click listener for the service start command intent
		views.setOnClickPendingIntent(R.id.widget_container, myPI);

		//define the componenet for self
		ComponentName comp = new ComponentName(context.getPackageName(), TimidityAEWidgetProvider.class.getName());
		//tell the manager to update all instances of the toggle widget with the click listener
		mgr.updateAppWidget(comp, views);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getExtras() != null) {
			paused = intent.getBooleanExtra("com.xperia64.timidityae.timidityaewidgetprovider.paused", true);
			currTitle = intent.getStringExtra("com.xperia64.timidityae.timidityaewidgetprovider.title");
			art = intent.getBooleanExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", false);
			setdeath = intent.getBooleanExtra("com.xperia64.timidityae.timidityaewidgetprovider.death", false);
			if (currTitle == null)
				currTitle = "";
		}
		super.onReceive(context, intent);
	}

	public static Bitmap scaleDownBitmap(Bitmap photo) {

		int currSize;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
			 currSize = photo.getByteCount();
		}else{
			currSize = photo.getRowBytes() * photo.getHeight();
		}
		if (currSize > maxBitmap) {
			int h = (int) (Math.sqrt(maxBitmap)) / (photo.getRowBytes() / photo.getWidth());
			int w = (int) (h * photo.getWidth() / ((double) photo.getHeight()));
			return Bitmap.createScaledBitmap(photo, w, h, true);
		} else {
			return photo;
		}
	}

	/*public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
	{
		System.out.println("I am resized");
	}*/
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private static Point getDisplaySize(final Display display) {
		final Point point = new Point();
		try {
			display.getSize(point);
		} catch (NoSuchMethodError ignore) { // Older device
			point.x = display.getWidth();
			point.y = display.getHeight();
		}
		return point;
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//System.out.println("Update was called");
		if (maxBitmap == -1) {
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = getDisplaySize(display);
			maxBitmap = size.x * size.y * 1.5;
			if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR1)) // TODO Seems safe
				maxBitmap *= 4;
		}

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int appWidgetId : appWidgetIds) {
			// Create an Intent to launch ExampleActivity

			//Get the layout for the App Widget and attach an on-click listener
			// to the button
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			views.setTextViewText(R.id.titley_widget, currTitle);
			views.setImageViewResource(R.id.notPause_widget, (paused) ? R.drawable.ic_media_play : R.drawable.ic_media_pause);
			if (art)
				if (Globals.currArt != null) {
					views.setImageViewBitmap(R.id.art_widget, scaleDownBitmap(Globals.currArt));
				} else
					views.setImageViewResource(R.id.art_widget, R.drawable.timidity);
			else
				views.setImageViewResource(R.id.art_widget, R.drawable.timidity);

			//retrieve a ref to the manager so we can pass a view update

			Intent configIntent = new Intent(context, TimidityActivity.class);

			PendingIntent configPendingIntent = PendingIntent.getActivity(context, 99, configIntent, 0);

			views.setOnClickPendingIntent(R.id.widget_container, configPendingIntent);


			Intent new_intent = new Intent();
			//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_prev);
			PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(context, 6, new_intent, 0);
			views.setOnClickPendingIntent(R.id.notPrev_widget, pendingNotificationIntent);
			// Play/Pause
			new_intent = new Intent();
			//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_play_or_pause);
			pendingNotificationIntent = PendingIntent.getBroadcast(context, 7, new_intent, 0);
			views.setOnClickPendingIntent(R.id.notPause_widget, pendingNotificationIntent);
			// Next
			new_intent = new Intent();
			//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_next);
			pendingNotificationIntent = PendingIntent.getBroadcast(context, 8, new_intent, 0);
			views.setOnClickPendingIntent(R.id.notNext_widget, pendingNotificationIntent);
			// Stop
			new_intent = new Intent();
			//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_stop);
			pendingNotificationIntent = PendingIntent.getBroadcast(context, 9, new_intent, 0);
			views.setOnClickPendingIntent(R.id.notStop_widget, pendingNotificationIntent);
			// Tell the AppWidgetManager to perform an upd_widgetate on the current app widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
		if (setdeath)
			SettingsStorage.nukedWidgets = true;
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
}
