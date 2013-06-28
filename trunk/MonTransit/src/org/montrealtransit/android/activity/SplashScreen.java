package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.StmDbHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * This class is the 1st screen [not] displayed by the application.
 * @author Mathieu Méa
 */
public class SplashScreen extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = SplashScreen.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SplashScreen";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// if (org.montrealtransit.android.BuildConfig.DEBUG) {
		// org.montrealtransit.android.api.SupportFactory.getInstance().enableStrictMode();
		// }
		Utils.logAppVersion(this);
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// check if update required => show toast
		StmDbHelper.showUpdateRequiredIfNecessary(this);
		showMainScreen();
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * Show the main screen.
	 */
	private void showMainScreen() {
		MyLog.v(TAG, "showMainScreen()");
		startActivity(new Intent(this, MainScreen.class));
		this.finish();
	}
}
