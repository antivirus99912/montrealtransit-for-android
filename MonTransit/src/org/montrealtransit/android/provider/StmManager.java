package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

/**
 * This manager provide methods to access STM static information about bus stops, bus lines, subway lines, subway stations. Use the content provider
 * {@link StmProvider}
 * @author Mathieu M�a
 */
public class StmManager {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmManager.class.getSimpleName();

	/**
	 * Represents the fields the content provider will return for a subway station.
	 */
	private static final String[] PROJECTION_SUBWAY_STATION = new String[] { StmStore.SubwayStation._ID, StmStore.SubwayStation.STATION_ID,
	        StmStore.SubwayStation.STATION_NAME, StmStore.SubwayStation.STATION_LNG, StmStore.SubwayStation.STATION_LAT };

	/**
	 * Represents the fields the content provider will return for a bus stop.
	 */
	private static final String[] PROJECTION_BUS_STOP = new String[] { StmStore.BusStop._ID, StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE,
	        StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, StmStore.BusStop.STOP_LINE_NUMBER, StmStore.BusStop.STOP_SUBWAY_STATION_ID };

	/**
	 * Represents the fields the content provider will return for an extended bus stop (including bus line info).
	 */
	private static final String[] PROJECTION_BUS_STOP_EXTENDED = new String[] { StmStore.BusStop._ID, StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE,
	        StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, StmStore.BusStop.STOP_LINE_NUMBER, StmStore.BusStop.LINE_NAME, StmStore.BusStop.LINE_TYPE,
	        StmStore.BusStop.LINE_HOURS, StmStore.BusStop.STOP_SUBWAY_STATION_ID };
	
	/**
	 * Represents the fields the content provider will return for an extended bus stop (including subway station name).
	 */
	private static final String[] PROJECTION_BUS_STOP_AND_SUBWAY_STATION = new String[] {
		StmStore.BusStop._ID, StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE,
        StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, StmStore.BusStop.STOP_LINE_NUMBER,
        StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmStore.BusStop.STATION_NAME
	};

	/**
	 * Represents the fields the content provider will return for a bus line direction.
	 */
	private static final String[] PROJECTION_BUS_LINE_DIRECTION = new String[] { StmStore.BusLineDirection._ID, StmStore.BusLineDirection.DIRECTION_ID,
	        StmStore.BusLineDirection.DIRECTION_LINE_ID, StmStore.BusLineDirection.DIRECTION_NAME };

	/**
	 * Represents the fields the content provider will return for a subway line.
	 */
	private static final String[] PROJECTION_SUBWAY_LINE = new String[] { StmStore.SubwayLine._ID, StmStore.SubwayLine.LINE_NUMBER,
	        StmStore.SubwayLine.LINE_NAME };

	/**
	 * Represents the fields the content provider will return for a bus line.
	 */
	private static final String[] PROJECTION_BUS_LINE = new String[] { StmStore.BusLine._ID, StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
	        StmStore.BusLine.LINE_HOURS, StmStore.BusLine.LINE_TYPE };

	/**
	 * Find a subway station from it URI.
	 * @param contentResolver the content resolver
	 * @param uri the subway station URI
	 * @return the subway station or <b>NULL</b>
	 */
	public static StmStore.SubwayStation findSubwayStation(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findSubwayStation(" + uri.getPath() + ")");
		StmStore.SubwayStation subwayStation = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					subwayStation = StmStore.SubwayStation.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return subwayStation;
	}

	/**
	 * Find the subway station from its ID.
	 * @see {@link StmManager#findSubwayLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the subway station
	 */
	public static StmStore.SubwayStation findSubwayStation(ContentResolver contentResolver, String subwayStationId) {
		return findSubwayStation(contentResolver, Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationId));
	}

	/**
	 * Find a bus line stop.
	 * @param contentResolver the content resolver.
	 * @param uri the content URI
	 * @return the bus stop
	 */
	public static StmStore.BusStop findBusStop(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBusStop(" + uri.getPath() + ")");
		StmStore.BusStop busStop = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					busStop = StmStore.BusStop.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return busStop;
	}

	/**
	 * <b>WARNING</b> May not return the expected bus stop since multiple bus stop can share a stop code.
	 * @see {@link StmManager#findBusStop(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busStopCode the bus stop code.
	 * @return the first bus stop found with this stop code.
	 */
	public static StmStore.BusStop findBusStop(ContentResolver contentResolver, String busStopCode) {
		return findBusStop(contentResolver, Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, busStopCode));
	}

	/**
	 * Find a bus stop matching the bus stop code and the bus line number.
	 * @see {@link StmManager#findBusStop(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busStopCode the bus stop code
	 * @param busLineNumber the bus line number
	 * @return a bus stop
	 */
	public static StmStore.BusStop findBusLineStop(ContentResolver contentResolver, String busStopCode, String busLineNumber) {
		// MyTrace.v(TAG, "findBusLineStop("+busStopCode+", "+busLineNumber+")");
		Uri busLineUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber);
		Uri busLineStopsUri = Uri.withAppendedPath(busLineUri, StmStore.BusLine.BusStops.CONTENT_DIRECTORY);
		Uri busLineStopUri = Uri.withAppendedPath(busLineStopsUri, busStopCode);
		return findBusStop(contentResolver, busLineStopUri);
	}

	/**
	 * Find a subway line.
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return a subway line
	 */
	public static StmStore.SubwayLine findSubwayLine(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findSubwayLine(" + uri.getPath() + ")");
		StmStore.SubwayLine subwayLine = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					subwayLine = StmStore.SubwayLine.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return subwayLine;
	}

	/**
	 * Find a bus line
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return a bus line
	 */
	public static StmStore.BusLine findBusLine(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBusLine(" + uri.getPath() + ")");
		StmStore.BusLine busLine = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					busLine = StmStore.BusLine.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return busLine;
	}

	/**
	 * Find a bus line direction.
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return the bus line direction
	 */
	public static StmStore.BusLineDirection findBusLineDirection(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBusLineDirection(" + uri.getPath() + ")");
		StmStore.BusLineDirection busLine = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, PROJECTION_BUS_LINE_DIRECTION, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					busLine = StmStore.BusLineDirection.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return busLine;
	}
	
	/**
	 * Find bus line direction.
	 * @see {@link StmManager#findBusLineDirection(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busLineDirectionId the bus line direction ID
	 * @return the bus line direction
	 */
	public static StmStore.BusLineDirection findBusLineDirection(ContentResolver contentResolver, String busLineDirectionId) {
		// MyTrace.v(TAG, "findBusLineDirection("+busLineDirectionId+")");
		return findBusLineDirection(contentResolver, Uri.withAppendedPath(StmStore.BusLineDirection.CONTENT_URI, busLineDirectionId));
	}

	/**
	 * Find a subway line
	 * @see {@link StmManager#findSubwayLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @return a subway line
	 */
	public static StmStore.SubwayLine findSubwayLine(ContentResolver contentResolver, int subwayLineId) {
		// MyTrace.v(TAG, "findSubwayLine("+subwayLineId+")");
		return findSubwayLine(contentResolver, ContentUris.withAppendedId(StmStore.SubwayLine.CONTENT_URI, subwayLineId));
	}

	/**
	 * Find a bus lines list
	 * @param contentResolver the content resolver
	 * @param busLineIds the bus lines IDs
	 * @return the bus lines list
	 */
	public static Cursor findBusLines(ContentResolver contentResolver, List<String> busLineIds) {
		MyLog.v(TAG, "findBusLine(" + busLineIds.size() + ")");
		String busLineIdsS = "";
		for (String busLineId : busLineIds) {
			if (busLineIdsS.length() > 0) {
				busLineIdsS += "+";
			}
			busLineIdsS += busLineId;
		}
		return contentResolver.query(Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineIdsS), PROJECTION_BUS_LINE, null, null,
		        StmStore.BusLine.DEFAULT_SORT_ORDER);
	}
	
	/**
	 * Find distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops
	 */
	public static Cursor findBusStops(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStops(" + busStopIdsString + ")");
		return contentResolver.query(Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, busStopIdsString), PROJECTION_BUS_STOP, null, null,
		        StmStore.BusStop.ORDER_BY_LINE_CODE);
	}
	
	/**
	 * Find a list of distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops list
	 */
	public static List<StmStore.BusStop> findBusStopsList(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStopsList(" + busStopIdsString + ")");
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findBusStops(contentResolver, busStopIdsString);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						result.add(StmStore.BusStop.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "No result found for bus stops \"" + busStopIdsString + "\"");
				}
			} else {
				MyLog.w(TAG, "No result found for bus stops \"" + busStopIdsString + "\"");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}
	
	/**
	 * Find distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops
	 */
	public static Cursor findBusStopsExtended(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStopsExtended(" + busStopIdsString + ")");
		return contentResolver.query(Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, busStopIdsString), PROJECTION_BUS_STOP_EXTENDED, null, null,
		        StmStore.BusStop.ORDER_BY_LINE_CODE);
	}
	
	/**
	 * Find a list of distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops list
	 */
	public static List<StmStore.BusStop> findBusStopsExtendedList(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStopsExtendedList(" + busStopIdsString + ")");
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findBusStopsExtended(contentResolver, busStopIdsString);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						result.add(StmStore.BusStop.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "No result found for bus stops \"" + busStopIdsString + "\"");
				}
			} else {
				MyLog.w(TAG, "No result found for bus stops \"" + busStopIdsString + "\"");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Return the bus stops URI for the live folder.
	 * @param favList the favorite list
	 * @return the bus stops URI
	 */
	public static Uri getBusStopsFavUri(List<DataStore.Fav> favList) {
		MyLog.v(TAG, "getBusStopsFavUri(" + favList.size() + ")");
		String favIdsS = "";
		for (DataStore.Fav favId : favList) {
			if (favIdsS.length() > 0) {
				favIdsS += "+";
			}
			favIdsS += favId.getFkId() + "-" + favId.getFkId2();
		}
		return Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI_FAV, favIdsS);
	}

	/**
	 * Find a bus line
	 * @see {@link StmManager#findBusLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busLineId the bus line ID
	 * @return the bus line
	 */
	public static StmStore.BusLine findBusLine(ContentResolver contentResolver, String busLineId) {
		MyLog.v(TAG, "findBusLine(" + busLineId + ")");
		return findBusLine(contentResolver, Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineId));
	}

	/**
	 * Find bus line directions for a bus line.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @return the bus line directions
	 */
	public static List<StmStore.BusLineDirection> findBusLineDirections(ContentResolver contentResolver, String busLineNumber) {
		MyLog.v(TAG, "fingBusLineDirections(" + busLineNumber + ")");
		List<StmStore.BusLineDirection> result = null;
		Cursor c = null;
		try {
			Uri busLinesUri = StmStore.BusLine.CONTENT_URI;
			Uri theBusLineUri = Uri.withAppendedPath(busLinesUri, busLineNumber);
			Uri busLinesDurectionsUri = Uri.withAppendedPath(theBusLineUri, StmStore.BusLine.BusLineDirections.CONTENT_DIRECTORY);
			MyLog.v(TAG, "busLinesDurectionsUri>" + busLinesDurectionsUri.getPath());
			c = contentResolver.query(busLinesDurectionsUri, PROJECTION_BUS_LINE_DIRECTION, null, null, StmStore.BusLine.DEFAULT_SORT_ORDER);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLineDirection>();
					do {
						result.add(StmStore.BusLineDirection.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "Bus Line Directions is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "Bus Line Directions is EMPTY !!!");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway line first station in this order
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @param sortOrder the sort order
	 * @return the subway line
	 */
	public static StmStore.SubwayStation findSubwayLineFirstSubwayStation(ContentResolver contentResolver, String subwayLineId, String sortOrder) {
		MyLog.v(TAG, "findSubwayLineFirstSubwayStation(" + subwayLineId + ")");
		StmStore.SubwayStation subwayStation = null;
		Cursor c = null;
		try {
			Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
			Uri subwayLineUri = Uri.withAppendedPath(subwayLinesUri, subwayLineId);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			MyLog.v(TAG, "subwayLineStationUri>" + subwayLineStationsUri.getPath());
			c = contentResolver.query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					subwayStation = StmStore.SubwayStation.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return subwayStation;
	}

	/**
	 * Find the last subway line station in this order.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @param sortOrder the sort order
	 * @return the subway station
	 */
	public static StmStore.SubwayStation findSubwayLineLastSubwayStation(ContentResolver contentResolver, int subwayLineId, String sortOrder) {
		MyLog.v(TAG, "findSubwayLineLastSubwayStation(" + subwayLineId + ")");
		StmStore.SubwayStation subwayStation = null;
		Cursor c = null;
		try {
			Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
			Uri subwayLineUri = ContentUris.withAppendedId(subwayLinesUri, subwayLineId);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			MyLog.v(TAG, "subwayLineStationUri>" + subwayLineStationsUri.getPath());
			c = contentResolver.query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToLast()) {
					subwayStation = StmStore.SubwayStation.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return subwayStation;
	}

	/**
	 * Return a cursor containing all subway lines.
	 * @param contentResolver the content resolver
	 * @return the cursor
	 */
	public static Cursor findAllSubwayLines(ContentResolver contentResolver) {
		return contentResolver.query(StmStore.SubwayLine.CONTENT_URI, PROJECTION_SUBWAY_LINE, null, null, null);
	}
	
	/**
	 * Search all subway lines.
	 * @param contentResolver the content resolver
	 * @param search the keywords
	 * @return the cursor
	 */
	public static Cursor searchAllSubwayLines(ContentResolver contentResolver, String search) {
		MyLog.v(TAG, "searchAllSubwayLines(" + search + ")");
		if (!TextUtils.isEmpty(search)) {
			Uri searchUri = Uri.withAppendedPath(Uri.withAppendedPath(StmStore.SubwayLine.CONTENT_URI,
			        StmStore.SEARCH_URI), search);
			return contentResolver.query(searchUri, PROJECTION_SUBWAY_LINE, null, null, null);
		} else {
			return findAllSubwayLines(contentResolver);
		}
	}

	/**
	 * Find a bus stop line. <b>WARNING</b> Is this working?
	 * @param contentResolver the content resolver
	 * @param stopCode the bus stop code
	 * @return the bus line
	 */
	public static StmStore.BusLine findBusStopLine(ContentResolver contentResolver, String stopCode) {
		MyLog.v(TAG, "findBusStopLine(" + stopCode + ")");
		Uri busStopsUri = Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, stopCode);
		Uri busLinesUri = Uri.withAppendedPath(busStopsUri, StmStore.BusStop.BusLines.CONTENT_DIRECTORY);
		return findBusLine(contentResolver, busLinesUri);
	}

	/**
	 * Find bus stop lines.
	 * @param contentResolver the content resolver
	 * @param stopCode the bus stop code
	 * @return the bus lines cursor
	 */
	public static Cursor findBusStopLines(ContentResolver contentResolver, String stopCode) {
		MyLog.v(TAG, "findBusStopLines(" + stopCode + ")");
		Uri busStopsUri = Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, stopCode);
		Uri busLinesUri = Uri.withAppendedPath(busStopsUri, StmStore.BusStop.BusLines.CONTENT_DIRECTORY);
		return contentResolver.query(busLinesUri, PROJECTION_BUS_LINE, null, null, null);
	}

	/**
	 * Return the list of the bus lines for the bus stop.
	 * @param contentResolver the content resolver
	 * @param stopCode the bus stop code
	 * @return the bus lines list
	 */
	public static List<StmStore.BusLine> findBusStopLinesList(ContentResolver contentResolver, String stopCode) {
		MyLog.v(TAG, "findBusStopLinesList(" + stopCode + ")");
		List<StmStore.BusLine> result = null;
		Cursor c = null;
		try {
			c = findBusStopLines(contentResolver, stopCode);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLine>();
					do {
						result.add(StmStore.BusLine.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "No bus lines found for bus stop \"" + stopCode + "\"");
				}
			} else {
				MyLog.w(TAG, "No bus lines found for bus stop \"" + stopCode + "\"");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find subway station lines list.
	 * @param contentResolver the content resolver
	 * @param stationId the subway station ID
	 * @return the subway lines
	 */
	public static List<StmStore.SubwayLine> findSubwayStationLinesList(ContentResolver contentResolver, String stationId) {
		MyLog.v(TAG, "findSubwayStationLinesList(" + stationId + ")");
		List<StmStore.SubwayLine> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationLines(contentResolver, stationId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayLine>();
					do {
						result.add(StmStore.SubwayLine.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "SubwayLines is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "SubwayLines.SIZE = 0 !!!");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Return a cursor containing subway station lines.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the subway lines
	 */
	public static Cursor findSubwayStationLines(ContentResolver contentResolver, String subwayStationId) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri subwayLinesUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY);
		MyLog.v(TAG, "subwayLinesUri>" + subwayLinesUri.getPath());
		return contentResolver.query(subwayLinesUri, PROJECTION_SUBWAY_LINE, null, null, null);
	}

	/**
	 * Return the subway line stations in the specified order.
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @return the subway stations
	 */
	public static Cursor findSubwayLineStations(ContentResolver contentResolver, int subwayLineNumber, String order) {
		Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
		Uri subwayLineUri = ContentUris.withAppendedId(subwayLinesUri, subwayLineNumber);
		Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
		MyLog.v(TAG, "subwayLineStationsUri>" + subwayLineStationsUri.getPath());
		return contentResolver.query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, order);
	}

	/**
	 * Return the subway line stations matching the subway line number and the search (subway line name).
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @param search the search
	 * @return the subway stations
	 */
	public static Cursor searchSubwayLineStations(ContentResolver contentResolver, int subwayLineNumber, String order,
	        String search) {
		if (!TextUtils.isEmpty(search)) {
			Uri subwayLineUri = ContentUris.withAppendedId(StmStore.SubwayLine.CONTENT_URI, subwayLineNumber);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri,
			        StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			Uri searchSubwayLineStationsUri = Uri.withAppendedPath(Uri.withAppendedPath(subwayLineStationsUri,
			        StmStore.SEARCH_URI), search);
			MyLog.v(TAG, "searchSubwayLineStationsUri>" + searchSubwayLineStationsUri.getPath());
			return contentResolver.query(searchSubwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, order);
		} else {
			return findSubwayLineStations(contentResolver, subwayLineNumber, order);
		}
	}

	/**
	 * Return the subway line stations list in the specified order.
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @return the subway stations
	 */
	public static List<StmStore.SubwayStation> findSubwayLineStationsList(ContentResolver contentResolver, int subwayLineNumber, String order) {
		List<StmStore.SubwayStation> result = null;
		Cursor c = null;
		try {
			c = findSubwayLineStations(contentResolver, subwayLineNumber, order);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayStation>();
					do {
						result.add(StmStore.SubwayStation.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway stations with those IDs
	 * @param contentResolver the content resolver
	 * @param subwayStationIds the subway station IDs
	 * @return the subway stations
	 */
	public static Cursor findSubwayStations(ContentResolver contentResolver, List<String> subwayStationIds) {
		MyLog.v(TAG, "findSubwayStations(" + subwayStationIds.size() + ")");
		String subwayStationIdsS = "";
		for (String subwayStationId : subwayStationIds) {
			if (subwayStationIdsS.length() > 0) {
				subwayStationIdsS += "+";
			}
			subwayStationIdsS += subwayStationId;
		}
		return contentResolver.query(Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationIdsS), PROJECTION_SUBWAY_STATION, null,
		        null, null);
	}

	/**
	 * Find bus line stops matching the bus line number and the direction ID.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @param directionId the direction ID
	 * @return the bus stops
	 */
	public static Cursor findBusLineStops(ContentResolver contentResolver, String busLineNumber, String directionId) {
		Uri busLineUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber);
		Uri busLineDirectionsUri = Uri.withAppendedPath(busLineUri, StmStore.BusLine.BusLineDirections.CONTENT_DIRECTORY);
		Uri busLineDirectionUri = Uri.withAppendedPath(busLineDirectionsUri, directionId);
		Uri busStopsUri = Uri.withAppendedPath(busLineDirectionUri, StmStore.BusLine.BusLineDirections.BusStops.CONTENT_DIRECTORY);
		MyLog.v(TAG, "URI: " + busStopsUri.getPath());
		return contentResolver.query(busStopsUri, PROJECTION_BUS_STOP_AND_SUBWAY_STATION, null, null, null);
	}
	
	/**
	 * Search bus line stops matching the bus line number and the direction ID.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @param directionId the direction ID
	 * @param search the search
	 * @return the bus stops
	 */
	public static Cursor searchBusLineStops(ContentResolver contentResolver, String busLineNumber, String directionId,
	        String search) {
		if (!TextUtils.isEmpty(search)) {
			Uri busLineUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber);
			Uri busLineDirectionsUri = Uri.withAppendedPath(busLineUri,
			        StmStore.BusLine.BusLineDirections.CONTENT_DIRECTORY);
			Uri busLineDirectionUri = Uri.withAppendedPath(busLineDirectionsUri, directionId);
			Uri busStopsUri = Uri.withAppendedPath(busLineDirectionUri,
			        StmStore.BusLine.BusLineDirections.BusStops.CONTENT_DIRECTORY);
			Uri searchUri = Uri.withAppendedPath(Uri.withAppendedPath(busStopsUri, StmStore.SEARCH_URI), search);
			MyLog.v(TAG, "URI: " + searchUri.getPath());
			return contentResolver.query(searchUri, PROJECTION_BUS_STOP_AND_SUBWAY_STATION, null, null, null);
		} else {
			return findBusLineStops(contentResolver, busLineNumber, directionId);
		}
	}

	/**
	 * Find subway station bus lines
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the bus lines
	 */
	public static Cursor findSubwayStationBusLines(ContentResolver contentResolver, String subwayStationId) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri busLinesUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.BusLines.CONTENT_DIRECTORY);
		MyLog.v(TAG, "busLinesUri>" + busLinesUri.getPath());
		return contentResolver.query(busLinesUri, PROJECTION_BUS_LINE, null, null, null);
	}

	/**
	 * Find the subway station extended bus stops (with bus line info)
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the extended bus stops
	 */
	public static Cursor findSubwayStationBusStopsExtended(ContentResolver contentResolver, String subwayStationId) {
		MyLog.v(TAG, "findSubwayStationBusStopsExtended(" + subwayStationId + ")");
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri busStopsUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.BusStops.CONTENT_DIRECTORY);
		return contentResolver.query(busStopsUri, PROJECTION_BUS_STOP_EXTENDED, null, null, StmStore.SubwayStation.BusStops.DEFAULT_SORT_ORDER);
	}

	/**
	 * Find subway station extended bus stops (with bus line info) list.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the extended bus stops list
	 */
	public static List<StmStore.BusStop> findSubwayStationBusStopsExtendedList(ContentResolver contentResolver, String subwayStationId) {
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationBusStopsExtended(contentResolver, subwayStationId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						result.add(StmStore.BusStop.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find all bus lines.
	 * @param contentResolver the content resolver
	 * @return the bus lines
	 */
	public static Cursor findAllBusLines(ContentResolver contentResolver) {
		return contentResolver.query(StmStore.BusLine.CONTENT_URI, PROJECTION_BUS_LINE, null, null, null);
	}
	
	/**
	 * Search from all bus lines.
	 * @param contentResolver the content resolver
	 * @param search
	 * @return the bus lines
	 */
	public static Cursor searchAllBusLines(ContentResolver contentResolver, String search) {
		if (!TextUtils.isEmpty(search)) {
			Uri searchUri = Uri.withAppendedPath(Uri
			        .withAppendedPath(StmStore.BusLine.CONTENT_URI, StmStore.SEARCH_URI), search);
			MyLog.v(TAG, "searchUri>" + searchUri.getPath());
			return contentResolver.query(searchUri, PROJECTION_BUS_LINE, null, null, null);
		} else {
			return findAllBusLines(contentResolver);
		}
	}

	/**
	 * Find all bus lines list.
	 * @param contentResolver the content resolver
	 * @return the bus lines list
	 */
	public static List<StmStore.BusLine> findAllBusLinesList(ContentResolver contentResolver) {
		List<StmStore.BusLine> result = null;
		Cursor c = null;
		try {
			c = findAllBusLines(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLine>();
					do {
						result.add(StmStore.BusLine.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}
}
