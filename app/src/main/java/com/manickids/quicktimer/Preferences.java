package com.manickids.quicktimer;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

public class Preferences extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	SharedPreferences pf;
	ListPreference sense, volSense;
	RingtonePreference ring;
	Preference duration;
	String durstr;
	Long durtime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.setting);
		pf = PreferenceManager.getDefaultSharedPreferences(this);
		sense = (ListPreference)findPreference("sense");
		sense.setSummary(sense.getValue()+getString(R.string.second));
		sense.setOnPreferenceChangeListener(this);
		volSense = (ListPreference)findPreference("volSense");
		volSense.setSummary(volSense.getEntry());
		volSense.setOnPreferenceChangeListener(this);
		ring = (RingtonePreference)findPreference("ringtone");
		duration = (Preference)findPreference("duration");
		durtime = pf.getLong("DURATION", 0);
		if(durtime == 0) durstr = getString(R.string.nolimit);
		else durstr = durtime+"초";
		duration.setSummary(durstr);
		Uri uri = Uri.parse( pf.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString()) );
		Log.i("STRING", Settings.System.DEFAULT_RINGTONE_URI.toString());
		Ringtone r = RingtoneManager.getRingtone(this, uri);
		try{
			ring.setDefaultValue(uri);
			ring.setSummary(r.getTitle(this));
		} catch(Exception e){
			e.getStackTrace();
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		Uri uri = Uri.parse( pf.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString()) );
		Ringtone r = RingtoneManager.getRingtone(this, uri);
		try{
			ring.setDefaultValue(uri);
			ring.setSummary(r.getTitle(this));
		} catch(Exception e){
			e.getStackTrace();
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference.getKey().equals("sense")){
			sense.setValue((String)newValue);
			sense.setSummary((String)newValue+getString(R.string.second));
			pf.edit().putString("sense", (String)newValue).commit();
		}
		if(preference.getKey().equals("volSense")){
			volSense.setValue((String)newValue);
			volSense.setSummary(volSense.getEntry());
			pf.edit().putString("volSense", (String)newValue).commit();			
		}
		return false;
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen ps, Preference p){
		if(p.getKey().equals("duration")){
			pf.getLong("DURATION", 0);
			final Dialog durdialog = new Dialog(this);
			LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
			View wrapper = inflater.inflate(R.layout.duration, (ViewGroup)findViewById(R.id.wrapper));
			wrapper.findViewById(R.id.btn_nolimit).setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					pf.edit().putLong("DURATION", 0);
					duration.setSummary(R.string.nolimit);
					durdialog.dismiss();
				}
			});
			wrapper.findViewById(R.id.btn_done).setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					durdialog.dismiss();
				}
			});
			durdialog.setContentView(wrapper);
			durdialog.show();
		}
		if(p.getKey().equals("info")){
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
		}
		return false;
	}
}