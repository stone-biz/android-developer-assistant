package com.stone.developer.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class UninstallBroadcastReceiver extends BroadcastReceiver {
	public void onReceive(Context ctx, Intent intent) {
		if(intent != null) {
			String action = intent.getAction();
			 if(!TextUtils.isEmpty(action) ) {
				 Intent it = new Intent(MyActivity.ACTION_REFRESH);
				 if(Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
					 it.putExtra("data", MyActivity.EXTRA_DATA_UNINSTALLED);
				 } else if(Intent.ACTION_PACKAGE_ADDED.equals(action)) {
					 it.putExtra("data", MyActivity.EXTRA_DATA_INSTALLED);
				 }
				 ctx.sendBroadcast(it);
			 }
		}
	}
}
