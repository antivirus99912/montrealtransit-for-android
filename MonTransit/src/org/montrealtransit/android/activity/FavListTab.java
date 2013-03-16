package org.montrealtransit.android.activity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
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
	 * The favorite bike station list.
	 */
	private List<DataStore.Fav> currentBikeStationFavList;

	/**
	 * The favorite subway stations list.
	 */
	private List<DataStore.Fav> currentSubwayStationFavList;

	/**
	 * The favorite bus stops list.
	 */
	private List<DataStore.Fav> currentBusStopFavList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.fav_list_tab);
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
			private Map<String, SubwayStation> subwayStations;
			private Map<String, List<SubwayLine>> otherSubwayLines;
			private List<DataStore.Fav> newBikeFavList;
			private Map<String, BikeStation> bikeStations;

			@Override
			protected Void doInBackground(Void... params) {
				// MyLog.v(TAG, "doInBackground()");
				// BUS STOPs
				this.newBusStopFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
				if (FavListTab.this.currentBusStopFavList == null || !Fav.listEquals(FavListTab.this.currentBusStopFavList, this.newBusStopFavList)) {
					if (Utils.getCollectionSize(this.newBusStopFavList) > 0) {
						MyLog.d(TAG, "Loading bus stop favorites from DB...");
						this.busStopsExtendedList = StmManager.findBusStopsExtendedList(getContentResolver(),
								Utils.extractBusStopIDsFromFavList(this.newBusStopFavList));
						MyLog.d(TAG, "Loading bus stop favorites from DB... DONE");
					}
				}
				// SUBWAY STATIONs
				this.newSubwayFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
				if (FavListTab.this.currentSubwayStationFavList == null || !Fav.listEquals(FavListTab.this.currentSubwayStationFavList, this.newSubwayFavList)) {
					MyLog.d(TAG, "Loading subway station favorites from DB...");
					this.subwayStations = new HashMap<String, SubwayStation>();
					this.otherSubwayLines = new HashMap<String, List<SubwayLine>>();
					for (Fav subwayFav : this.newSubwayFavList) {
						SubwayStation station = StmManager.findSubwayStation(getContentResolver(), subwayFav.getFkId());
						this.subwayStations.put(subwayFav.getFkId(), station);
						if (station != null) {
							this.otherSubwayLines.put(station.getId(), StmManager.findSubwayStationLinesList(getContentResolver(), station.getId()));
						}
					}
					MyLog.d(TAG, "Loading subway station favorites from DB... DONE");
				}
				// BIKE STATIONs
				this.newBikeFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
				if (FavListTab.this.currentBikeStationFavList == null || !Fav.listEquals(FavListTab.this.currentBikeStationFavList, this.newBikeFavList)) {
					if (Utils.getCollectionSize(this.newBikeFavList) > 0) {
						MyLog.d(TAG, "Loading bike station favorites from DB...");
						this.bikeStations = BixiManager.findBikeStationsMap(getContentResolver(),
								Utils.extractBikeStationTerminNamesFromFavList(this.newBikeFavList));
						MyLog.d(TAG, "Loading bike station favorites from DB... DONE");
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (newBusStopFavList != null) { // IF favorite bus stop list was refreshed DO update the UI
					refreshBusStopsUI(this.newBusStopFavList, this.busStopsExtendedList);
				}
				if (newSubwayFavList != null) { // IF favorite subway station list was refreshed DO update the UI
					refreshSubwayStationsUI(this.newSubwayFavList, this.subwayStations, this.otherSubwayLines);
				}
				if (newBikeFavList != null) { // IF favorite bike station list was refreshed DO update the UI
					refreshBikeStationsUI(this.newBikeFavList, this.bikeStations);
				}
				showEmptyFav();
				UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV, isThereAtLeastOneFavorite());
			}

		}.execute();
	}

	public boolean isThereAtLeastOneFavorite() {
		return Utils.getCollectionSize(FavListTab.this.currentBusStopFavList) > 0 || Utils.getCollectionSize(FavListTab.this.currentSubwayStationFavList) > 0
				|| Utils.getCollectionSize(FavListTab.this.currentBikeStationFavList) > 0;
	}

	/**
	 * Show 'no favorite' view if necessary.
	 */
	private void showEmptyFav() {
		MyLog.v(TAG, "showEmptyFav()");
		findViewById(R.id.loading).setVisibility(View.GONE);
		if (!isThereAtLeastOneFavorite()) {
			findViewById(R.id.lists).setVisibility(View.GONE);
			findViewById(R.id.empty).setVisibility(View.VISIBLE);
		} else { // at least 1 favorite
			findViewById(R.id.empty).setVisibility(View.GONE);
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			// IF there is no favorite bus stops DO
			if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() == 0) {
				findViewById(R.id.fav_bus_stops).setVisibility(View.GONE);
				findViewById(R.id.bus_stops_list).setVisibility(View.GONE);
			}
			// IF there is no favorite subway stations DO
			if (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() == 0) {
				findViewById(R.id.fav_subway_stations).setVisibility(View.GONE);
				findViewById(R.id.subway_stations_list).setVisibility(View.GONE);
			}
			// IF there is no favorite bike stations DO
			if (this.currentBikeStationFavList == null || this.currentBikeStationFavList.size() == 0) {
				findViewById(R.id.fav_bike_stations).setVisibility(View.GONE);
				findViewById(R.id.bike_stations_list).setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Refresh the favorite bus stops UI
	 * @param newBusStopFavList the new favorite bus stops
	 * @param busStopsExtendedList the bus stops (extended)
	 */
	private void refreshBusStopsUI(List<DataStore.Fav> newBusStopFavList, List<BusStop> busStopsExtendedList) {
		// MyLog.v(TAG, "refreshBusStopsUI(%s,%s)", Utils.getCollectionSize(newBusStopFavList), Utils.getCollectionSize(busStopsExtendedList));
		if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() != newBusStopFavList.size()) {
			// remove all favorite bus stop views
			LinearLayout busStopsLayout = (LinearLayout) findViewById(R.id.bus_stops_list);
			busStopsLayout.removeAllViews();
			// use new favorite bus stops
			this.currentBusStopFavList = newBusStopFavList;
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_bus_stops).setVisibility(View.VISIBLE);
			busStopsLayout.setVisibility(View.VISIBLE);
			// FOR EACH bus stop DO
			if (busStopsExtendedList != null) {
				for (final BusStop busStop : busStopsExtendedList) {
					// list view divider
					if (busStopsLayout.getChildCount() > 0) {
						busStopsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, busStopsLayout, false));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_bus_stop_item, busStopsLayout, false);
					// bus stop code
					((TextView) view.findViewById(R.id.stop_code)).setText(busStop.getCode());
					// bus stop place
					String busStopPlace = BusUtils.cleanBusStopPlace(busStop.getPlace());
					((TextView) view.findViewById(R.id.label)).setText(busStopPlace);
					// bus stop line number
					TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
					lineNumberTv.setText(busStop.getLineNumber());
					int color = BusUtils.getBusLineTypeBgColor(busStop.getLineTypeOrNull(), busStop.getLineNumber());
					lineNumberTv.setBackgroundColor(color);
					// bus stop line direction
					int busLineDirection = BusUtils.getBusLineSimpleDirection(busStop.getDirectionId());
					((TextView) view.findViewById(R.id.line_direction)).setText(getString(busLineDirection).toUpperCase(Locale.getDefault()));
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
									.setItems(
											new CharSequence[] { getString(R.string.view_bus_stop), getString(R.string.view_bus_stop_line),
													getString(R.string.remove_fav) }, new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int item) {
													MyLog.v(TAG, "onClick(%s)", item);
													switch (item) {
													case 0:
														Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
														intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
														intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busStop.getLineNameOrNull());
														intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busStop.getLineTypeOrNull());
														intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
														intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStop.getPlace());
														startActivity(intent);
														break;
													case 1:
														Intent intent2 = new Intent(FavListTab.this, SupportFactory.getInstance(FavListTab.this)
																.getBusLineInfoClass());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, busStop.getLineNumber());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_NAME, busStop.getLineNameOrNull());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_TYPE, busStop.getLineTypeOrNull());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_DIRECTION_ID, busStop.getDirectionId());
														startActivity(intent2);
														break;
													case 2:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.bus_stops_list)).removeView(theViewToDelete);
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
														UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV,
																isThereAtLeastOneFavorite());
														// find the favorite to delete
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, busStop.getCode(), busStop.getLineNumber());
														// delete the favorite
														if (findFav != null) {
															DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														}
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
					busStopsLayout.addView(view);
				}
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
			LinearLayout subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
			// remove all subway station views
			subwayStationsLayout.removeAllViews();
			// use new favorite subway station
			this.currentSubwayStationFavList = newSubwayFavList;
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_subway_stations).setVisibility(View.VISIBLE);
			subwayStationsLayout.setVisibility(View.VISIBLE);
			// FOR EACH favorite subway DO
			for (Fav subwayFav : this.currentSubwayStationFavList) {
				final SubwayStation station = stations == null ? null : stations.get(subwayFav.getFkId());
				if (station != null) {
					List<SubwayLine> otherLinesId = otherLines.get(station.getId());
					// list view divider
					if (subwayStationsLayout.getChildCount() > 0) {
						subwayStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, subwayStationsLayout, false));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_subway_station_item, subwayStationsLayout, false);
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
													case 0:
														Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
														intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, station.getId());
														intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, station.getName());
														startActivity(intent);
														break;
													case 1:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.subway_stations_list)).removeView(theViewToDelete);
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
														UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV,
																isThereAtLeastOneFavorite());
														// delete the favorite
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION, station.getId(), null);
														// delete the favorite
														if (findFav != null) {
															DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														}
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
					subwayStationsLayout.addView(view);
				} else {
					MyLog.w(TAG, "Can't find the favorite subway station (ID:%s)", subwayFav.getFkId());
				}
			}
		}
	}

	/**
	 * Refresh bike station UI.
	 * @param newBikeFavList the new favorite bike stations list
	 * @param bikeStations the new favorite bike stations
	 */
	private void refreshBikeStationsUI(List<DataStore.Fav> newBikeFavList, Map<String, BikeStation> bikeStations) {
		// MyLog.v(TAG, "refreshBikeStationsUI(%s,%s)", Utils.getCollectionSize(newBikeFavList), Utils.getMapSize(bikeStations));
		if (this.currentBikeStationFavList == null || this.currentBikeStationFavList.size() != newBikeFavList.size()) {
			LinearLayout bikeStationsLayout = (LinearLayout) findViewById(R.id.bike_stations_list);
			// remove all bike station views
			bikeStationsLayout.removeAllViews();
			// use new favorite bike station
			this.currentBikeStationFavList = newBikeFavList;
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_bike_stations).setVisibility(View.VISIBLE);
			bikeStationsLayout.setVisibility(View.VISIBLE);
			// FOR EACH favorite bike DO
			if (bikeStations != null) {
				for (final BikeStation bikeStation : bikeStations.values()) {
					// list view divider
					if (bikeStationsLayout.getChildCount() > 0) {
						bikeStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, bikeStationsLayout, false));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_bike_station_item, bikeStationsLayout, false);
					// subway station name
					((TextView) view.findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(bikeStation.getName()));
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(FavListTab.this, BikeStationInfo.class);
							intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, bikeStation.getTerminalName());
							intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, bikeStation.getName());
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
									.setTitle(bikeStation.getName())
									.setItems(new CharSequence[] { getString(R.string.view_bike_station), getString(R.string.remove_fav) },
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int item) {
													MyLog.v(TAG, "onClick(%s)", item);
													switch (item) {
													case 0:
														Intent intent = new Intent(FavListTab.this, BikeStationInfo.class);
														intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, bikeStation.getTerminalName());
														intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, bikeStation.getName());
														startActivity(intent);
														break;
													case 1:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.bike_stations_list)).removeView(theViewToDelete);
														// remove the favorite from the current list
														Iterator<Fav> it = FavListTab.this.currentBikeStationFavList.iterator();
														while (it.hasNext()) {
															DataStore.Fav fav = (DataStore.Fav) it.next();
															if (fav.getFkId().equals(bikeStation.getTerminalName())) {
																it.remove();
																break;
															}
														}
														// refresh empty
														showEmptyFav();
														UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV,
																isThereAtLeastOneFavorite());
														// delete the favorite
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS, bikeStation.getTerminalName(), null);
														// delete the favorite
														if (findFav != null) {
															DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														}
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
					bikeStationsLayout.addView(view);
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
