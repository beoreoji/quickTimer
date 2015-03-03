package com.manickids.quicktimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimerReceiver extends BroadcastReceiver {
		
	@Override
	public void onReceive(Context context, Intent intent){
		Intent it = new Intent(context, Main.class);
		it.putExtra("ACTION",true);
		it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(it);
	}
}

