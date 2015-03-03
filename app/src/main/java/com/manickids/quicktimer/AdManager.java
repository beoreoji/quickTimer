package com.manickids.quicktimer;

import java.util.Random;

import net.daum.adam.publisher.AdView.OnAdFailedListener;
import net.daum.adam.publisher.AdView.OnAdLoadedListener;
import net.daum.adam.publisher.impl.AdError;

import com.flurry.android.FlurryAgent;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.inmobi.androidsdk.IMAdListener;
import com.inmobi.androidsdk.IMAdRequest;
import com.inmobi.androidsdk.IMAdView;
import com.inmobi.androidsdk.IMAdRequest.ErrorCode;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

public class AdManager implements AdListener, IMAdListener {
	Activity activity;
	Context context;
	LinearLayout adFrame;
	String locale;
	com.google.ads.AdView admob;
	net.daum.adam.publisher.AdView adam;
	com.inmobi.androidsdk.IMAdView inmobi;
	com.inmobi.androidsdk.IMAdRequest inmobiRequest;
	boolean retryAdam;
	static final String ADMOB = "a14f20284358fe3";
	static final String INMOBI = "84032dd28ce04d3c9bc8b15102e2a73f";
	static final String ADAM = "1ef4Z9fT13515b81710";
	
	public AdManager(Activity a, Context c, LinearLayout adFrame, String locale){
		this.activity = a;
		this.context = c;
		this.adFrame = adFrame;
		this.locale = locale;
		
        // 50% 의 확률로 인모비 호출.
		if(new Random().nextBoolean()) callInmobi();
		else {
			// 로케일이 한국어 -> 아담 호출.
			if(locale.equals("ko")) callAdam();
			// 로케일이 비한국어 -> 애드몹 호출
			else callAdMob();
		}
	}

    /* AdMob */
    private void callAdMob(){    	
    	admob = new com.google.ads.AdView((Activity)context, AdSize.SMART_BANNER, ADMOB);
    	adFrame.addView(admob);
        admob.loadAd(new AdRequest());
    }
    
	@Override
	public void onDismissScreen(Ad arg0) {}

	@Override
	public void onFailedToReceiveAd(Ad arg0, com.google.ads.AdRequest.ErrorCode arg1) {	
		adFrame.removeAllViews();
		admob.destroy();
    	callInmobi();
    	FlurryAgent.logEvent("애드몹 실패 후 인모비를 대신 호출함");
	}

	@Override
	public void onLeaveApplication(Ad arg0) {}

	@Override
	public void onPresentScreen(Ad arg0) {}

	@Override
	public void onReceiveAd(Ad arg0) {
        if(locale.equals("ko")){
        	FlurryAgent.logEvent("한국어에서 애드몹 호출 성공");
        } else {
        	FlurryAgent.logEvent("한국어 이외에서 애드몹 호출 성공");
        }
	}

    /* InMobi */
    private void callInmobi(){
    	inmobi = new IMAdView((Activity)context, IMAdView.INMOBI_AD_UNIT_320X50, INMOBI );
    	adFrame.addView(inmobi);
		inmobiRequest = new IMAdRequest();
		inmobiRequest.setTestMode(false);
		inmobi.setIMAdRequest(inmobiRequest);
		inmobi.loadNewAd(inmobiRequest);
    }
	@Override
	public void onAdRequestCompleted(IMAdView arg0) {
		FlurryAgent.logEvent("인모비 호출 성공");
	}

	@Override
	public void onAdRequestFailed(IMAdView arg0, ErrorCode arg1) {
		adFrame.removeAllViews();
		inmobi.stopLoading();
		inmobiRequest = null;
		inmobi = null;
        if(locale.equals("ko")){
    		callAdam();
    		FlurryAgent.logEvent("인모비 실패하여 아담 호출 시도");
        } else {
        	callAdMob();
        	FlurryAgent.logEvent("인모비 실패하여 애드몹 호출 시도");
        }
	}

	@Override
	public void onDismissAdScreen(IMAdView arg0) {}

	@Override
	public void onLeaveApplication(IMAdView arg0) {}

	@Override
	public void onShowAdScreen(IMAdView arg0) {}
		
	
	
    /* Ad@m */
    private void callAdam(){
    	adam = new net.daum.adam.publisher.AdView(context);
    	adam.setClientId(ADAM);
    	adam.setRequestInterval(15);    	
    	adFrame.addView(adam);
    	adam.setVisibility(View.VISIBLE);
    	adam.setOnAdFailedListener(new OnAdFailedListener(){
    		@Override
    		public void OnAdFailed(AdError arg0, String arg1) {
	    		adFrame.removeAllViews();
	    		adam.destroy();
    			if(!retryAdam){
    	    		retryAdam = true;
    	    		callInmobi();
    	    		FlurryAgent.logEvent("아담 실패 후 인모비를 대신 호출함");
    	    	} else {
    	    		callAdMob();
    	    		FlurryAgent.logEvent("아담이 두번째 실패하여 애드몹을 대신 호출함");    	    		
    	    	}    	    	
    		}
    	});
    	adam.setOnAdLoadedListener(new OnAdLoadedListener(){
    		@Override
    		public void OnAdLoaded(){
    			FlurryAgent.logEvent("아담 호출 성공");
    		}
    	});
    }
    
    
    public void destroy(){
    	try {
	    	if( admob != null) {
	    		admob.destroy();
	    		admob = null;
	    	}
	    	if(adam != null) {
	    		adam.destroy();
	    		adam = null;
	    	}
	    	if(inmobi != null) {
	    		inmobi.stopLoading();
	    		inmobi = null;
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    		FlurryAgent.logEvent("광고 destroy 실패 : "+e.getMessage());
    	}
    }
}
