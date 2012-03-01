package org.montrealtransit.android.activity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This activity list the favorite bus stops.
 * @author Mathieu Méa
 */
public class FavListTab extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = FavListTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/FavList";

	/**
	 * The favorite subway stations list.
	 */
	private List<DataStore.Fav> currentSubwayStationFavList;

	/**
	 * The favorite bus stops list.
	 */
	private List<DataStore.Fav> currentBusStopFavList;

	/**
	 * The favorite subway stations layout.
	 */
	private LinearLayout subwayStationsLayout;
	/**
	 * The favorite bus stops layout.
	 */
	private LinearLayout busStopsLayout;
	/**
	 * The favorite bus stop title layout.
	 */
	private View busStopsTitle;
	/**
	 * The favorite subway stations title layout.
	 */
	private View subwayStationsTitle;
	/**
	 * The empty layout.
	 */
	private View emptyLayout;
	/**
	 * The lists layout.
	 */
	private View listsLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.fav_list_tab);

		this.emptyLayout = findViewById(R.id.empty);
		this.listsLayout = findViewById(R.id.lists);

		this.busStopsTitle = findViewById(R.id.fav_bus_stops);
		this.busStopsLayout = (LinearLayout) findViewById(R.id.bus_stops_list);

		this.subwayStationsTitle = findViewById(R.id.fav_subway_stations);
		this.subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		setUpUI();
		super.onResume();
	}

	/**
	 * Refresh all the UI.
	 */
	private void setUpUI() {
		MyLog.v(TAG, "setUpUI()");
		loadFavoritesFromDB();
	}

	private void loadFavoritesFromDB() {
		MyLog.v(TAG, "loadFavoritesFromDB()");
		new AsyncTask<Void, Void, Void>() {

			private List<DataStore.Fav> newBusStopFavList;
			private List<BusStop> busStopsExtendedList;
			private List<DataStore.Fav> newSubwayFavList;
			private Map<String, SubwayStation> stations;
			private Map<String, List<SubwayLine>> otherLines;

			@Override
			protected Void doInBackground(Void... params) {
				// MyLog.v(TAG, "doInBackground()");
				this.newBusStopFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
				if (FavListTab.this.currentBusStopFavList == null || FavListTab.this.currentBusStopFavList.size() != this.newBusStopFavList.size()) {
					MyLog.d(TAG, "Loading bus stop favorites from DB...");
					if (Utils.getCollectionSize(this.newBusStopFavList) > 0) {
						this.busStopsExtendedList = StmManager.findBusStopsExtendedList(FavListTab.this.getContentResolver(),
								Utils.extractBusStopIDsFromFavList(this.newBusStopFavList));
					}
					MyLog.d(TAG, "Loading bus stop favorites from DB... DONE");
				}

				this.newSubwayFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
				if (FavListTab.this.currentSubwayStationFavList == null || FavListTab.this.currentSubwayStationFavList.size() != this.newSubwayFavList.size()) {
					MyLog.d(TAG, "Loading subway station favorites from DB...");
					this.stations = new HashMap<String, SubwayStation>();
					this.otherLines = new HashMap<String, List<SubwayLine>>();
					for (Fav subwayFav : this.newSubwayFavList) {
						SubwayStation station = StmManager.findSubwayStation(getContentResolver(), subwayFav.getFkId());
						this.stations.put(subwayFav.getFkId(), station);
						if (station != null) {
							this.otherLines.put(station.getId(), StmManager.findSubwayStationLinesList(getContentResolver(), station.getId()));
						}
					}
					MyLog.d(TAG, "Loading subway station favorites from DB... DONE");
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (this.newBusStopFavList != null && this.busStopsExtendedList != null) { // IF favorite bus stop list was refreshed DO update the UI
					refreshBusStopsUI(this.newBusStopFavList, this.busStopsExtendedList);
				}
				if (this.newSubwayFavList != null) { // IF favorite subway station list was refreshed DO update the UI
					refreshSubwayStationsUI(this.newSubwayFavList, this.stations, this.otherLines);
				}
				showEmptyFav();
				UserPreferences.savePrefLcl(
						FavListTab.this,
						UserPreferences.PREFS_LCL_IS_FAV,
						Utils.getCollectionSize(FavListTab.this.currentBusStopFavList) > 0
								|| Utils.getCollectionSize(FavListTab.this.currentSubwayStationFavList) > 0);
			}

		}.execute();
	}

	/**
	 * Show 'no favorite' view if necessary.
	 */
	private void showEmptyFav() {
		MyLog.v(TAG, "showEmptyFav()");
		findViewById(R.id.loading).setVisibility(View.GONE);
		if ((this.currentBusStopFavList == null || this.currentBusStopFavList.size() == 0)
				&& (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() == 0)) {
			this.listsLayout.setVisibility(View.GONE);
			this.emptyLayout.setVisibility(View.VISIBLE);
		} else {
			this.emptyLayout.setVisibility(View.GONE);
			this.listsLayout.setVisibility(View.VISIBLE);
			// IF there is no favorite bus stops DO
			if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() == 0) {
				this.busStopsTitle.setVisibility(View.GONE);
				this.busStopsLayout.setVisibility(View.GONE);
				return;
			}
			// IF there is no favorite bus stops DO
			if (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() == 0) {
				this.subwayStationsTitle.setVisibility(View.GONE);
				this.subwayStationsLayout.setVisibility(View.GONE);
				return;
			}
		}
	}

	/**
	 * Context menu to view the favorite.
	 */
	private static final int VIEW_CONTEXT_MENU_INDEX = 0;
	/**
	 * Context menu to delete the favorite.
	 */
	private static final int DELETE_CONTEXT_MENU_INDEX = 1;

	private void refreshBusStopsUI(List<DataStore.Fav> newBusStopFavList, List<BusStop> busStopsExtendedList) {
		// MyLog.v(TAG, "refreshBusStopsUI(%s,%s)", Utils.getCollectionSize(newBusStopFavList), Utils.getCollectionSize(busStopsExtendedList));
		if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() != newBusStopFavList.size()) {
			// remove all favorite bus stop views
			this.busStopsLayout.removeAllViews();
			// use new favorite bus stops
			this.currentBusStopFavList = newBusStopFavList;
			this.listsLayout.setVisibility(View.VISIBLE);
			this.busStopsTitle.setVisibility(View.VISIBLE);
			this.busStopsLayout.setVisibility(View.VISIBLE);
			// FOR EACH bus stop DO
			for (final BusStop busStop : busStopsExtendedList) {
				// list view divider
				if (this.busStopsLayout.getChildCount() > 0) {
					this.busStopsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// create view
				View view = getLayoutInflater().inflate(R.layout.fav_list_tab_bus_stop_item, null);
				// bus stop code
				((TextView) view.findViewById(R.id.stop_code)).setText(busStop.getCode());
				// bus stop place
				String busStopPlace = BusUtils.cleanBusStopPlace(busStop.getPlace());
				((TextView) view.findViewById(R.id.label)).setText(busStopPlace);
				// bus stop line number
				((TextView) view.findViewById(R.id.line_number)).setText(busStop.getLineNumber());
				// bus stop line name
				((TextView) view.findViewById(R.id.line_name)).setText(busStop.getLineNameOrNull());
				// bus stop line direction
				int busLineDirection = BusUtils.getBusLineSimpleDirection(busStop.getDirectionId());
				((TextView) view.findViewById(R.id.line_direction)).setText(busLineDirection);
				// add click listener
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)", v.getId());
						Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
						intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
						intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busStop.getLineNameOrNull());
						intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busStop.getLineTypeOrNull());
						intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
						intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStop.getPlace());
						startActivity(intent);
					}
				});
				// add context menu
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						MyLog.v(TAG, "onLongClick(%s)", v.getId());
						final View theViewToDelete = v;
						new AlertDialog.Builder(FavListTab.this)
								.setTitle(getString(R.string.bus_stop_and_line_short, busStop.getCode(), busStop.getLineNumber()))
								.setItems(new CharSequence[] { getString(R.string.view_bus_stop), getString(R.string.remove_fav) },
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int item) {
												MyLog.v(TAG, "onClick(%s)", item);
												switch (item) {
												case VIEW_CONTEXT_MENU_INDEX:
													Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
													intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
													intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busStop.getLineNameOrNull());
													intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busStop.getLineTypeOrNull());
													intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
													intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStop.getPlace());
													startActivity(intent);
													break;
												case DELETE_CONTEXT_MENU_INDEX:
													// remove the view from the UI
													FavListTab.this.busStopsLayout.removeView(theViewToDelete);
													// remove the favorite from the current list
													Iterator<Fav> it = FavListTab.this.currentBusStopFavList.iterator();
													while (it.hasNext()) {
														DataStore.Fav fav = (DataStore.Fav) it.next();
														if (fav.getFkId().equals(busStop.getCode()) && fav.getFkId2().equals(busStop.getLineNumber())) {
															it.remove();
															break;
														}
													}
													// refresh empty
													showEmptyFav();
													UserPreferences.savePrefLcl(
															FavListTab.this,
															UserPreferences.PREFS_LCL_IS_FAV,
															Utils.getCollectionSize(FavListTab.this.currentBusStopFavList) > 0
																	|| Utils.getCollectionSize(FavListTab.this.currentSubwayStationFavList) > 0);
													// find the favorite to delete
													Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
															DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, busStop.getCode(), busStop.getLineNumber());
													// delete the favorite
													DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
													SupportFactory.getInstance(FavListTab.this).backupManagerDataChanged();
													break;
												default:
													break;
												}
											}
										}).create().show();
						return true;
					}
				});
				this.busStopsLayout.addView(view);
			}
		}
	}

	/**
	 * Refresh subway station UI.
	 * @param newSubwayFavList the new favorite subway stations list
	 * @param stations the new favorite subway stations
	 * @param otherLines the new favorite subway stations "other lines"
	 */
	private void refreshSubwayStationsUI(List<DataStore.Fav> newSubwayFavList, Map<String, SubwayStation> stations, Map<String, List<SubwayLine>> otherLines) {
		// MyLog.v(TAG, "refreshSubwayStationsUI()", Utils.getCollectionSize(newSubwayFavList), Utils.getMapSize(stations), Utils.getMapSize(otherLines));
		if (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() != newSubwayFavList.size()) {
			// remove all subway station views
			this.subwayStationsLayout.removeAllViews();
			// use new favorite subway station
			this.currentSubwayStationFavList = newSubwayFavList;
			this.listsLayout.setVisibility(View.VISIBLE);
			this.subwayStationsTitle.setVisibility(View.VISIBLE);
			this.subwayStationsLayout.setVisibility(View.VISIBLE);
			// FOR EACH favorite subway DO
			for (Fav subwayFav : this.currentSubwayStationFavList) {
				final SubwayStation station = stations.get(subwayFav.getFkId());
				if (station != null) {
					List<SubwayLine> otherLinesId = otherLines.get(station.getId());
					// list view divider
					if (this.subwayStationsLayout.getChildCount() > 0) {
						this.subwayStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_subway_station_item, null);
					// subway station name
					((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
					// station lines color
					if (otherLinesId != null && otherLinesId.size() > 0) {
						int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(0).getNumber());
						((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(subwayLineImg1);
						if (otherLinesId.size() > 1) {
							int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(1).getNumber());
							((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(subwayLineImg2);
							if (otherLinesId.size() > 2) {
								int subwayLineImg3 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(2).getNumber());
								((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(subwayLineImg3);
							} else {
								view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
							}
						} else {
							view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
							view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
						}
					} else {
						view.findViewById(R.id.subway_img_1).setVisibility(View.GONE);
						view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
						view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
					}
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
							intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, station.getId());
							intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, station.getName());
							startActivity(intent);
						}
					});
					// add context menu
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							MyLog.v(TAG, "onLongClick(%s)", v.getId());
							final View theViewToDelete = v;
							new AlertDialog.Builder(FavListTab.this)
									.setTitle(getString(R.string.subway_station_with_name_short, station.getName()))
									.setItems(new CharSequence[] { getString(R.string.view_subway_station), getString(R.string.remove_fav) },
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int item) {
													MyLog.v(TAG, "onClick(%s)", item);
													switch (item) {
													case VIEW_CONTEXT_MENU_INDEX:
														Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
														intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, station.getId());
														intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, station.getName());
														startActivity(intent);
														break;
													case DELETE_CONTEXT_MENU_INDEX:
														// remove the view from the UI
														FavListTab.this.subwayStationsLayout.removeView(theViewToDelete);
														// remove the favorite from the current list
														Iterator<Fav> it = FavListTab.this.currentSubwayStationFavList.iterator();
														while (it.hasNext()) {
															DataStore.Fav fav = (DataStore.Fav) it.next();
															if (fav.getFkId().equals(station.getId())) {
																it.remove();
																break;
															}
														}
														// refresh empty
														showEmptyFav();
														UserPreferences.savePrefLcl(
																FavListTab.this,
																UserPreferences.PREFS_LCL_IS_FAV,
																Utils.getCollectionSize(FavListTab.this.currentBusStopFavList) > 0
																		|| Utils.getCollectionSize(FavListTab.this.currentSubwayStationFavList) > 0);
														// delete the favorite
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION, station.getId(), null);
														// delete the favorite
														DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														SupportFactory.getInstance(FavListTab.this).backupManagerDataChanged();
														break;
													default:
														break;
													}
												}
											}).create().show();
							return true;
						}
					});
					this.subwayStationsLayout.addView(view);
				} else {
					MyLog.w(TAG, "Can't find the favorite subway station (ID:%s)", subwayFav.getFkId());
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.createMainMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}
