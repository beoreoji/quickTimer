package com.manickids.quicktimer;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Main extends Activity implements OnTouchListener {
	
	public static Activity homeActivity; // 엑티비티 외부에서 제어 당하기 위한 static
	AlarmManager am;
	NotificationManager nm;
	PowerManager pm;
	PowerManager.WakeLock WL;
	SharedPreferences pref;
	Vibrator vibe;
	MediaPlayer mMediaPlayer;
	
	ImageView spring; 
	Button reset, setting, apply;
	TextView remindTime;

	PendingIntent sender;
	TimerTask timerTask;
	Timer timer;
	Uri ringtone;
	Intent it;
	Bundle savedInstanceState;
	AnimationDrawable frame;
	String time; // 표시시간
	int second = 0; // 남은 초
	long waketime; // 작동예정 시간 (밀리초)
	float offset; // 터치위치
	int sense, volSense; // 감도
	boolean freeze = false;
	AdManager adm;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.main);
        homeActivity = Main.this;
        
        //화면꺼짐 인텐트 필터를 브로드캐스트리시버에 등록
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        LinearLayout adFrame = (LinearLayout)findViewById(R.id.adWrapper);
        String locale = this.getResources().getConfiguration().locale.getLanguage();
        adm = new AdManager(Main.this, this, adFrame, locale);
        
        vibe = (Vibrator)this.getSystemService(Service.VIBRATOR_SERVICE);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	waketime = System.currentTimeMillis() + 3000;
		sender = PendingIntent.getBroadcast(this, 0, new Intent(this, TimerReceiver.class), PendingIntent.FLAG_ONE_SHOT);      
		am.set(AlarmManager.RTC_WAKEUP, waketime, sender);
		am.cancel(sender);
        nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        remindTime = (TextView)findViewById(R.id.remindTime);
        reset = (Button)findViewById(R.id.reset);
        setting = (Button)findViewById(R.id.setting);
        apply = (Button)findViewById(R.id.apply);
        spring = (ImageView)findViewById(R.id.spring);
        spring.setOnTouchListener(this);

        sense = Integer.parseInt( pref.getString("sense", "15") );
        volSense = Integer.parseInt( pref.getString("volSense", "10") );

        it = getIntent();
        if(it.getBooleanExtra("ACTION",false)) {
        	it.putExtra("ACTION",false);
        	nm.cancel(0);
        	pref.edit().putBoolean("isAlarm", false).commit();
        	action();
        } else if(isAlarm()) {
        	nm.cancel(0);
        	waketime = getWaketime();
        	second = (int)((waketime - System.currentTimeMillis()) / 1000);
        	printTime();
        	reset.setVisibility(View.INVISIBLE);
        	setting.setVisibility(View.INVISIBLE);
        	apply.setBackgroundResource(R.drawable.btn_cancel);
        	timerTask = new TimerTask(){
        		public void run(){
       				second--;
       				TIMEChanged();
       			}
        	};
        	timer = new Timer();
        	timer.schedule(timerTask,500,1000);
        }
    }
	
    @Override
    protected void onStart(){
    	super.onStart();
    	FlurryAgent.onStartSession(this, "CTR9X4DM8JF8MJT9H6JQ");
    }
    
    @Override
    public void onRestart(){
    	super.onRestart();
    	onCreate(savedInstanceState);
    }

    private void action(){
    	mMediaPlayer = new MediaPlayer();
    	second = 0; // 마이너스로 내려가는 경우에 대비해, 남은초를 무조건 0초로 바꿈
    	printTime();
    	ringtone = Uri.parse( pref.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString() ));

    	// 화면 깨움
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    	pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        WL = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "Look at me");
        WL.acquire();

	    final AlertDialog.Builder alarm = new AlertDialog.Builder(this);
		alarm.setTitle(getString(R.string.clear)).setIcon(android.R.drawable.ic_dialog_alert);
		alarm.setPositiveButton(getString(R.string.unset), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which){
				try {
					if(pref.getBoolean("vibe", false)){
						vibe.cancel();
						vibe.vibrate(0);
						
					}
				} catch(Exception e){
					e.printStackTrace();
				}
				try {
					mMediaPlayer.stop();
					mMediaPlayer.release();
				} catch(Exception e){
					e.printStackTrace();
				}
				ringtone = null;
				mMediaPlayer = null;
				WL.release();
			}
		});
		alarm.show();
	    try {
			if(pref.getBoolean("vibe", false)){
				long[] pattern = {500, 1000};
				vibe.vibrate(pattern, 0);
			}
			if(ringtone != null && !pref.getBoolean("silence", false)){
	    		mMediaPlayer = new MediaPlayer();
		        mMediaPlayer.setDataSource(this, ringtone);
		        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
		        mMediaPlayer.setLooping(true);
		        mMediaPlayer.prepare();
		        mMediaPlayer.start();
	    	}
	    } catch (Exception e) {
	        e.printStackTrace();
	    }	    
    	reset.setVisibility(View.VISIBLE);
    	setting.setVisibility(View.VISIBLE);
    }
    
    private boolean isAlarm(){
    	return pref.getBoolean("isAlarm", false);
    }

    private long getWaketime(){
    	return pref.getLong("WAKETIME", 0);
    }
    
    public void TIMEChanged(){
    	runOnUiThread(new Runnable() {
    		public void run() {
    			printTime();
    			if(second < 1) {
    				timer.cancel();
    		        pref.edit().putBoolean("isAlarm", false).commit();
    		        waketime = 0;
    		    	second = 0;
    		    	apply.setBackgroundResource(R.drawable.btn_start);
    		    	action();
    			}
    		}
    	});    	
    }

    public void plustime(View v){
    	switch(v.getId()){
    	case R.id.p_hour:
    		second += 3600; break;
    	case R.id.p_30m:
    		second += 1800; break;
    	case R.id.p_10m:
			second += 600; break;
		case R.id.p_1m:
			second += 60; break;
    	}
    	if(second > 359999) alert99();
		printTime();
    }
    
    public void reset(View v){
    	second = 0;
    	printTime();
    }

    private void alert99(){
		freeze = true;
	    final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.nomore)).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getString(R.string.recommend_quickmemo));
		alert.setPositiveButton(getString(R.string.understand), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which){
				second = 359999;
				printTime();
				freeze = false;
			}
		});
		alert.show();					
    }
    
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(!freeze){
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				vibe.vibrate(20);
				offset = event.getX();
			}
			if (event.getAction() == MotionEvent.ACTION_MOVE){
				if(event.getX() < offset){
			        spring.setBackgroundResource(R.anim.slide_left);
			        frame = (AnimationDrawable)spring.getBackground();
					frame.start();
					if(second >= sense) {
						second -= sense;
						printTime();
					}
				}
				else if(event.getX() > offset){
			        spring.setBackgroundResource(R.anim.slide_right);
			        frame = (AnimationDrawable)spring.getBackground();
					frame.start();
					if(!freeze)	second += sense;
					if(second > 359999) alert99();
					printTime();
				}
				offset = event.getX();
			}
			if(event.getAction() == MotionEvent.ACTION_UP){
				frame.stop();
			}
		}
		return true;
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	super.onKeyDown(keyCode, event);    	
    	
    	if(event.getKeyCode() == KeyEvent.KEYCODE_MENU) return false;
    	
    	if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
    		onBackPressed();
    		return false;
    	}

    	if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP){
    		if(!freeze)	second += volSense;
			if(second > 359999) alert99();
			printTime();
			return true;
    	}

    	if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN){
			if(second >= volSense) {
				second -= volSense;
				printTime();
			}
			return true;
    	}
    	
    	return false;
    }
  
    private void printTime(){
    	long hour = second/3600;
    	long min = (second%3600)/60;
    	long sec = (second%3600)%60;
    	if(hour > 0) {
    		if(hour > 99) time = "99:59:59";
    		else time = String.format("%d:%02d:%02d", hour,min,sec);
    	}
    	else time = String.format("%02d:%02d", min,sec);
    	remindTime.setText(time);
    }

    private String printWaketime(long waketime){
    	Date time = new Date(waketime);
    	Calendar calendar = GregorianCalendar.getInstance();
    	calendar.setTime(time);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int min = calendar.get(Calendar.MINUTE);
		int sec = calendar.get(Calendar.SECOND);
		return String.format("%d"+getString(R.string.hour)+ "%d"+getString(R.string.minute)+ "%d"+getString(R.string.second), hour, min, sec);
    }
    
    public void apply(View v){
    	if(!isAlarm() && second > 0) {
	    	waketime = System.currentTimeMillis() + (long)(second * 1000);
	    	pref.edit().putBoolean("isAlarm", true).putLong("WAKETIME", waketime).commit();
	    	timerTask = new TimerTask(){
	    		public void run(){
	   				second--;
	   				TIMEChanged();
	   			}
	    	};
	    	timer = new Timer();
	    	timer.schedule(timerTask,500,1000);
	    	reset.setVisibility(View.INVISIBLE);
	    	setting.setVisibility(View.INVISIBLE);
	    	apply.setBackgroundResource(R.drawable.btn_cancel);
    	}
    	else if(isAlarm()) {
    	    final AlertDialog.Builder confirm = new AlertDialog.Builder(this);
    		confirm.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener(){
    			public void onClick(DialogInterface dialog, int which){
    				am.cancel(sender);
    				timer.cancel();
    				pref.edit().putBoolean("isAlarm", false).commit();
    		    	waketime = 0;
    				second = 0;
    				reset.setVisibility(View.VISIBLE);
    				setting.setVisibility(View.VISIBLE);
    				apply.setBackgroundResource(R.drawable.btn_start);
    				printTime();
    			}
    		}).setNegativeButton(getString(R.string.dont), new DialogInterface.OnClickListener(){
    			public void onClick(DialogInterface dialog, int which) {}
    		}).setMessage(getString(R.string.unsetnow)).setTitle(getString(R.string.caution)).setIcon(android.R.drawable.ic_dialog_alert);    
    		confirm.show();
    	}
    }
    
    public void startSetting(View v){
    	it = new Intent(this, Preferences.class);
    	startActivity(it);
    }
    
    private void setExit(){
		if(isAlarm()){
	    	waketime = System.currentTimeMillis() + (long)(second * 1000);
			pref.edit().putLong("WAKETIME", waketime).commit();
			timer.cancel();
			sender = PendingIntent.getBroadcast(this, 0, new Intent(this, TimerReceiver.class), PendingIntent.FLAG_ONE_SHOT);      
			am.set(AlarmManager.RTC_WAKEUP, waketime, sender);
    		Intent i = new Intent(this, Main.class);
	    	i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pending = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
			Notification notification = new Notification(R.drawable.icon, getString(R.string.running_now), System.currentTimeMillis());
			notification.setLatestEventInfo(this, getString(R.string.app_name), printWaketime(waketime), pending);
			nm.notify(0,notification);
		}
		else {
	    	waketime = System.currentTimeMillis() + 3000;
			sender = PendingIntent.getBroadcast(this, 0, new Intent(this, TimerReceiver.class), PendingIntent.FLAG_ONE_SHOT);      
			am.set(AlarmManager.RTC_WAKEUP, waketime, sender);
			am.cancel(sender);
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch(item.getItemId()){
    		case R.id.preference:
    			Intent intent = new Intent(this, Preferences.class);
	           	startActivity(intent);
    			break;
    		case R.id.about:
    			final Dialog about = new Dialog(this);
    			LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
    			View wrapper = inflater.inflate(R.layout.about, (ViewGroup)findViewById(R.id.wrapper));
    			wrapper.findViewById(R.id.btn_close).setOnClickListener(new OnClickListener(){
    				public void onClick(View v){
    					about.dismiss();
    				}
    			});
    			about.setContentView(wrapper);
    			about.show();
    			break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    	setExit();
    	FlurryAgent.onEndSession(this);
    }
        
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	adm.destroy();
    	ActivityManager actm = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
    	actm.restartPackage(getPackageName());
    }
}