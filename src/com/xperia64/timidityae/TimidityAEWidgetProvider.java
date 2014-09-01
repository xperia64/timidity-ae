/*******************************************************************************
 * Copyright (C) 2014 xperia64 <xperiancedapps@gmail.com>
 * 
 * Copyright (C) 1999-2008 Masanao Izumo <iz@onicos.co.jp>
 *     
 * Copyright (C) 1995 Tuukka Toivonen <tt@cgs.fi>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TimidityAEWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
// TODO make this real
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            //Intent intent = new Intent(context, TimidityActivity.class);
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            //views.setOnClickPendingIntent(R.id.notPause_widget, pendingIntent);
            Intent new_intent = new Intent();
  	  	  new_intent.setAction(context.getResources().getString(R.string.msrv_rec));
  	  	  new_intent.putExtra(context.getResources().getString(R.string.msrv_cmd), 4);
  	  	  PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(context, 0, new_intent, 0);
  	  	  views.setOnClickPendingIntent(R.id.notPrev_widget, pendingNotificationIntent);
  	  	  // Play/Pause
  	  	  new_intent = new Intent();
  	  	  new_intent.setAction(context.getResources().getString(R.string.msrv_rec));
  	  	  new_intent.putExtra(context.getResources().getString(R.string.msrv_cmd), 2);
  	  	  pendingNotificationIntent = PendingIntent.getBroadcast(context, 1, new_intent, 0);
  	  	  views.setOnClickPendingIntent(R.id.notPause_widget, pendingNotificationIntent);
  	  	  // Next
  	  	  new_intent = new Intent();
  	  	  new_intent.setAction(context.getResources().getString(R.string.msrv_rec));
  	  	  new_intent.putExtra(context.getResources().getString(R.string.msrv_cmd), 3);
  	  	  pendingNotificationIntent = PendingIntent.getBroadcast(context, 2, new_intent, 0);
  	  	  views.setOnClickPendingIntent(R.id.notNext_widget, pendingNotificationIntent);
  	  	  // Stop
  	  	  new_intent = new Intent();
  	  	  new_intent.setAction(context.getResources().getString(R.string.msrv_rec));
  	  	  new_intent.putExtra(context.getResources().getString(R.string.msrv_cmd), 5);
  	  	  pendingNotificationIntent = PendingIntent.getBroadcast(context, 3, new_intent, 0);
  	  	  views.setOnClickPendingIntent(R.id.notStop_widget, pendingNotificationIntent);
            // Tell the AppWidgetManager to perform an upd_widgetate on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
