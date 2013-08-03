package org.montrealtransit.android;

import org.montrealtransit.android.api.SupportFactory;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * This class contains useful methods to interact with Google Analytics API.
 * @author Mathieu Méa
 */
public class AnalyticsUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = AnalyticsUtils.class.getSimpleName();

	/**
	 * Use this boolean to disable the tracker.
	 */
	private static boolean TRACKING = true;

	// /**
	// * Event category "show".
	// */
	// public static final String CAT_SHOW = "show";
	//
	// /**
	// * Event action "view".
	// */
	// public static final String ACT_VIEW = "view";

	/**
	 * Custom variables scope levels.
	 */
	public static final int SCOPE_VISITOR_LEVEL = 1;
	public static final int SCOPE_SESSION_LEVEL = 2;
	public static final int SCOPE_PAGE_LEVEL = 3;

	/**
	 * Custom variables slot (1-5).
	 */
	private static final int CUSTOM_VAR_INDEX_DEVICE = 1;
	private static final int CUSTOM_VAR_INDEX_APP_VERSION = 2;
	private static final int CUSTOM_VAR_INDEX_ANDROID_VERSION = 3;
	private static final int CUSTOM_VAR_INDEX_SDK_VERSION = 4;
	private static final int CUSTOM_VAR_INDEX_OPERATOR = 5;

	/**
	 * Category for errors.
	 */
	public static final String CATEGORY_ERROR = "error";

	/**
	 * Action for removed bus stops.
	 */
	public static final String ACTION_BUS_STOP_REMOVED = "bus_stop_removed";
	/**
	 * Action for no info for bus stops.
	 */
	public static final String ACTION_BUS_STOP_NO_INFO = "bus_stop_no_info";
	/**
	 * Action for bus stop source error code.
	 */
	public static final String ACTION_BUS_STOP_SOURCE_ERROR = "bus_stop_source_error";
	/**
	 * Action for DB initialization fail.
	 */
	public static final String ACTION_DB_INIT_FAIL = "db_init_failt";
	/**
	 * Action for Bixi data loading fail.
	 */
	public static final String ACTION_BIXI_DATA_LOADING_FAIL = "bixi_data_loading_fail";
	/**
	 * Action for HTTP errors.
	 */
	public static final String ACTION_HTTP_ERROR = "http_error";

	/**
	 * The instance.
	 */
	private static AnalyticsUtils instance;

	/**
	 * @return the instance
	 */
	public static AnalyticsUtils getInstance() {
		MyLog.v(TAG, "getInstance()");
		if (instance == null) {
			instance = new AnalyticsUtils();
		}
		return instance;
	}

	/**
	 * The Google Analytics tracker.
	 */
	private static GoogleAnalyticsTracker tracker;

	/**
	 * True if the tracker is started.
	 */
	private static boolean trackerStarted = false;

	/**
	 * @param context the context
	 * @return the Google Analytics tracker
	 */
	private synchronized static GoogleAnalyticsTracker getGoogleAnalyticsTracker(final Context context) {
		MyLog.v(TAG, "getGoogleAnalyticsTracker()");
		if (tracker == null) {
			MyLog.v(TAG, "Initializing the Google Analytics tracker...");
			tracker = GoogleAnalyticsTracker.getInstance();
			// initTrackerWithUserData(context);
			MyLog.v(TAG, "Initializing the Google Analytics tracker... DONE");
		}
		if (!trackerStarted) {
			MyLog.v(TAG, "Starting the Google Analytics tracker...");
			tracker.startNewSession(context.getString(R.string.google_analytics_id), context);
			trackerStarted = true;
			MyLog.v(TAG, "Starting the Google Analytics tracker... DONE");
		}
		return tracker;
	}

	/**
	 * Initialize the Google Analytics tracker with user device properties.
	 * @param context the context
	 */
	private static void initTrackerWithUserData(Context context, GoogleAnalyticsTracker tracker) {
		// only 5 Custom Variables allowed!

		// 1 - Application version
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			String appVersion = packageInfo.versionName;
			// MyLog.d(TAG, "app_version: '%s'.", appVersion);
			tracker.setCustomVar(CUSTOM_VAR_INDEX_APP_VERSION, "version", appVersion, SCOPE_VISITOR_LEVEL);
			// getTracker(context).setProductVersion("MonTransit", appVersion);
		} catch (Exception e) {
		}

		// 2 - Android version
		String androidVersion = Build.VERSION.RELEASE;
		// MyLog.d(TAG, "os_rel: '%s'.", androidVersion);
		tracker.setCustomVar(CUSTOM_VAR_INDEX_ANDROID_VERSION, "android", androidVersion, SCOPE_VISITOR_LEVEL);

		// 3 - Android SDK
		@SuppressWarnings("deprecation")
		// TODO use Build.VERSION.SDK_INT
		String sdk = Build.VERSION.SDK;
		// MyLog.d(TAG, "sdk_version: '%s'.", sdk);
		tracker.setCustomVar(CUSTOM_VAR_INDEX_SDK_VERSION, "sdk", sdk, SCOPE_VISITOR_LEVEL);

		// 4 - Device
		String device = "";
		if (Integer.parseInt(sdk) >= Build.VERSION_CODES.DONUT) {
			device += SupportFactory.get().getBuildManufacturer() + " ";
		}
		device += Build.MODEL;
		// MyLog.d(TAG, "device: '%s'.", device);
		tracker.setCustomVar(CUSTOM_VAR_INDEX_DEVICE, "device", device, SCOPE_VISITOR_LEVEL);

		// 5 - Network Operator
		try {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String operator = telephonyManager.getNetworkOperatorName();
			// MyLog.d(TAG, "operator: '%s'.", operator);
			tracker.setCustomVar(CUSTOM_VAR_INDEX_OPERATOR, "operator", operator, SCOPE_VISITOR_LEVEL);
		} catch (Exception e) {
		}
		// already provided by Analytics:
		// user language + country
		// service provider
	}

	/**
	 * Track an event.
	 * @param context the context
	 * @param category the event category
	 * @param action the event action
	 * @param label the event label
	 * @param value the event value
	 */
	public static void trackEvent(Context context, final String category, final String action, final String label, final int value) {
		MyLog.v(TAG, "trackEvent()");
		if (TRACKING) {
			new AsyncTask<Context, Void, Void>() {
				@Override
				protected Void doInBackground(Context... params) {
					try {
						GoogleAnalyticsTracker gaTracker = getGoogleAnalyticsTracker(params[0]);
						initTrackerWithUserData(params[0], gaTracker);
						gaTracker.trackEvent(category, action, label, value);
					} catch (Throwable t) {
						MyLog.w(TAG, t, "Error while tracing view!");
					}
					return null;
				}
			}.execute(context);
		}
	}

	/**
	 * Track a page view.
	 * @param context the context
	 * @param page the viewed page.
	 */
	public static void trackPageView(Context context, final String page) {
		MyLog.v(TAG, "trackPageView(%s)", page);
		if (TRACKING) {
			new AsyncTask<Context, Void, Void>() {
				@Override
				protected Void doInBackground(Context... params) {
					try {
						GoogleAnalyticsTracker gaTracker = getGoogleAnalyticsTracker(params[0]);
						initTrackerWithUserData(params[0], gaTracker);
						gaTracker.trackPageView(page);
					} catch (Throwable t) {
						MyLog.w(TAG, t, "Error while tracing view!");
					}
					return null;
				}
			}.execute(context);
		}
	}

	/**
	 * Dispatch the Google Analytics tracker content.
	 * @param context the context
	 */
	public static void dispatch(Context context) {
		MyLog.v(TAG, "dispatch()");
		if (TRACKING) {
			try {
				getGoogleAnalyticsTracker(context).dispatch();
			} catch (Throwable t) {
				MyLog.w(TAG, t, "Error while dispatching analytics data.");
			}
		}
	}

	/**
	 * Stop the Google Analytics tracker
	 * @param context the context
	 */
	public static void stop(Context context) {
		MyLog.v(TAG, "stop()");
		if (TRACKING) {
			getGoogleAnalyticsTracker(context).stopSession();
			trackerStarted = false;
		}
	}
}
