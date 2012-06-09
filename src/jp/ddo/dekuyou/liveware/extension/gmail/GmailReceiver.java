package jp.ddo.dekuyou.liveware.extension.gmail;

import jp.ddo.dekuyou.android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GmailReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// 
		Log.initialize(context);

		Log.d("Gmail onReceive : " + intent.getAction());

		Bundle extras = intent.getExtras();
		
		
		// 
		intent = new Intent(context, GmailExtensionService.class);
		intent.putExtras(extras);
		context.startService(intent);
		
		
//		if (extras != null) {
//
//			String extrasStr = "";
//			Set<String> set = extras.keySet();
//			for (String str : set) {
//				Log.d(this.getClass().getPackage().getName(), "extras:" + str
//						+ ":" + extras.get(str).toString());
//				extrasStr = "extras:" + str + ":" + extras.get(str).toString()
//						+ "\n";
//
//			}
//			
//			Log.d(extrasStr);
//
//
//		}

	}
}
