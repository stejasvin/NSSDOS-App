package com.nssdos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.osmdroid.api.IGeoPoint;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapController.AnimationType;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;
import org.osmdroid.DefaultResourceProxyImpl;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

/**
 * Demo Activity using osmdroid and osmbonuspack
 * 
 * @see http://code.google.com/p/osmbonuspack/
 * @author M.Kergall
 * 
 */
public class MapActivity extends Activity implements LocationListener {
	protected static MapView map;

	protected static final int IMPORT_REQUEST = 10;
	boolean CREATE_MARKER_FROM_MAP_TOUCH = false;

	protected GeoPoint startPoint, destinationPoint;
	SimpleLocationOverlay myLocationOverlay;

	static ArrayList<HashMap<String, String>> locations;
	static MapController mapController;

	ArrayList<String> tempSearchList = new ArrayList<String>();
	public static ArrayList<String> allImports = new ArrayList<String>();
	ArrayAdapter<String> adapter = null;

	static File allFile;
	public static Context GLOBAL_CONTEXT;
	GeoPoint centerMe;
	EditText edSearch;

	LocationManager locationManager;

	private final int CREATE_MARKER_MODE = 20;
	
	MyItemizedOverlay tempItemizedOverlay;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		writeLocationsToFile();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		overridePendingTransition(R.anim.fadein, R.anim.fadeout);

		setContentView(R.layout.main);
		
		locations = new ArrayList<HashMap<String,String>>();
		GLOBAL_CONTEXT = MapActivity.this;
		
		File dir = new File("/sdcard/NSSDOS");
		dir.mkdir();
		allFile = new File(dir,"allFile.txt");
		try {
			allFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		map = (MapView) findViewById(R.id.map);
		map.setTileSource(TileSourceFactory.MAPNIK);

		map.setBuiltInZoomControls(true);
		map.setMultiTouchControls(true);
		mapController = map.getController();

//		map.gett
		

		importMarkers("/sdcard/NSSDOS/allFile.txt", MapActivity.this);

		if (getIntent().getStringExtra("com.nssdos.filePathRet") != null)
			importMarkers(
					getIntent().getStringExtra(
							"com.nssdos.filePathRet"),
					MapActivity.this);

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				30 * 1000, 100.0f, this);
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 10000, 100.0f, this);

		Location l = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (l != null) {
			startPoint = new GeoPoint(l.getLatitude(), l.getLongitude());

		} else {
			startPoint = new GeoPoint(13.003036, 80.239847);
		}

		centerMe = startPoint;

		mapController.setZoom(15);

		myLocationOverlay = new SimpleLocationOverlay(this,
				new DefaultResourceProxyImpl(this));
		map.getOverlays().add(myLocationOverlay);

		mapController.setCenter(startPoint);

		edSearch = (EditText) findViewById(R.id.ed_searchPOI);

		edSearch.addTextChangedListener(filterTextWatcher);

		Button bSearch = (Button) findViewById(R.id.b_searchPOI);

		bSearch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String search = edSearch.getText().toString();
				boolean flag = true;
				for (int i = 0; i < locations.size(); i++) {
					if (locations.get(i).get("abt").equalsIgnoreCase(search)) {
						String[] pt = locations.get(i).get("pt").split(",");
						GeoPoint point = new GeoPoint(Integer.decode(pt[0]),
								Integer.decode(pt[1]));
						// mapController.setCenter(point);
						mapController.animateTo(point,
								AnimationType.MIDDLEPEAKSPEED);
						// mapController.setZoom(18);
						flag = false;
						edSearch.setText("");
						break;
					}
				}
				if (flag)
					Toast.makeText(MapActivity.this, "Location not found",
							Toast.LENGTH_LONG).show();

				LinearLayout l = (LinearLayout) findViewById(R.id.layout_search);
				l.setVisibility(LinearLayout.GONE);

			}
		});


		Button bCreateMarker2 = (Button) findViewById(R.id.b_create_marker_2);

		bCreateMarker2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				CREATE_MARKER_FROM_MAP_TOUCH = false;
				EditText edLat = (EditText) findViewById(R.id.lat);
				EditText edLong = (EditText) findViewById(R.id.lon);
				EditText edDesc = (EditText) findViewById(R.id.create_marker_desc);
				String lat = edLat.getText().toString();
				String lon = edLong.getText().toString();
				lat = lat.replace(".", "");
				lon = lon.replace(".", "");
				if (!lat.equals("") && !lon.equals("")
						&& !edDesc.getText().toString().equals("")) {
					// float fLat = Float.parseFloat(lat) * 1000000;
					// float fLong = Float.parseFloat(lon) * 1000000;

					int iLat = Integer.decode(lat);
					int iLong = Integer.decode(lon);
					GeoPoint point = new GeoPoint(iLat, iLong);
					OverlayItem overlayitem = new OverlayItem("What\'s Here?",
							edDesc.getText().toString(), point);
					List<OverlayItem> aList = new LinkedList<OverlayItem>();
					aList.add(overlayitem);
					MyItemizedOverlay mItemizedOverlay = new MyItemizedOverlay(
							MapActivity.this, aList);
					map.getOverlays().add(mItemizedOverlay);
					map.invalidate();
					
					HashMap<String, String> tempHash = new HashMap<String, String>();
					tempHash.put("abt", edDesc.getText().toString());
					tempHash.put(
							"pt",
							point.getLatitudeE6() + ","
									+ point.getLongitudeE6());
					if(!locations.contains(tempHash))
					locations.add(tempHash);
					writeLocationsToFile();

					LinearLayout l = (LinearLayout) findViewById(R.id.create_marker_layout);
					l.setVisibility(LinearLayout.GONE);

				}
			}
		});

		Button bCreateMarkerFromMap = (Button) findViewById(R.id.b_create_marker_from_map);

		bCreateMarkerFromMap.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				CREATE_MARKER_FROM_MAP_TOUCH= true;
				hideAllLayouts();

			}
		});

		
		Button bClearAll = (Button) findViewById(R.id.b_clear_mark);

		bClearAll.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				locations.clear();
				writeLocationsToFile();
				importMarkers("/sdcard/NSSDOS/allFile.txt", GLOBAL_CONTEXT);

			}
		});

		Button bCenter = (Button) findViewById(R.id.b_center);

		bCenter.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mapController.setCenter(centerMe);

			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == IMPORT_REQUEST) {

				importMarkers(
						data.getStringExtra("com.nssdos.filePathRet"),
						MapActivity.this);

			}

		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		if(!CREATE_MARKER_FROM_MAP_TOUCH)
		{
			hideAllLayouts();
			return false;
		}
		if (CREATE_MARKER_FROM_MAP_TOUCH) {
			Projection proj = map.getProjection();
			final IGeoPoint topLeft = proj.fromPixels(0, 0);

			
			Projection projection = map.getProjection();
		    GeoPoint geoPointTopLeft = (GeoPoint) projection.fromPixels(0,0);
		    Point topLeftPoint = new Point();
		    // Get the top left Point (includes osmdroid offsets)
		    projection.toPixels(geoPointTopLeft, topLeftPoint);
		    // get the GeoPoint of any point on screen 
		    GeoPoint rtnGeoPoint = (GeoPoint) projection.fromPixels(event.getX(), event.getY());
			
			OverlayItem overlayitem = new OverlayItem("What\'s Here?",
					"",rtnGeoPoint);
			List<OverlayItem> aList = new LinkedList<OverlayItem>();
			aList.add(overlayitem);
			if(tempItemizedOverlay!=null){
				map.getOverlays().remove(tempItemizedOverlay);
			}
			tempItemizedOverlay = new MyItemizedOverlay(
					MapActivity.this, aList);
			map.getOverlays().add(tempItemizedOverlay);
			map.invalidate();

			LinearLayout lMain = (LinearLayout) findViewById(R.id.main_layout);
			lMain.setVisibility(LinearLayout.VISIBLE);

			LinearLayout l1 = (LinearLayout) findViewById(R.id.create_marker_layout);
			l1.setVisibility(LinearLayout.VISIBLE);
			
			EditText edLat = (EditText) findViewById(R.id.lat);
			EditText edLong = (EditText) findViewById(R.id.lon);
			edLat.setText(Integer.toString(rtnGeoPoint.getLatitudeE6()));
			edLong.setText(Integer.toString(rtnGeoPoint.getLongitudeE6()));
			
			
			return true;
		}
		return false;
		
	}

	void hideAllLayouts() {

		LinearLayout l1 = (LinearLayout) findViewById(R.id.create_marker_layout);
		l1.setVisibility(LinearLayout.GONE);
		LinearLayout l2 = (LinearLayout) findViewById(R.id.layout_search);
		l2.setVisibility(LinearLayout.GONE);
		LinearLayout l = (LinearLayout) findViewById(R.id.main_layout);
		l.setVisibility(LinearLayout.GONE);

	}

	public static void searchAndRemoveAllImports(String abt) {

		for (int i = 0; i < locations.size(); i++) {
			if (locations.get(i).get("abt").equals(abt))
				locations.remove(i);
		}
		writeLocationsToFile();
		importMarkers("/sdcard/NSSDOS/allFile.txt", GLOBAL_CONTEXT);

	}

	static void writeLocationsToFile() {
		try {
			FileOutputStream fos = new FileOutputStream(allFile);
			String temp = new String();
			byte[] buffer;
			temp = "#DOS#";
			buffer = temp.getBytes();
			fos.write(buffer);

			for (int i = 0; i < locations.size(); i++) {
				temp = "";
				temp += locations.get(i).get("abt");
				temp += ":";
				temp += locations.get(i).get("pt");
				if (i != locations.size() - 1)
					temp += "--";

				buffer = temp.getBytes();
				fos.write(buffer);

			}
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private TextWatcher filterTextWatcher = new TextWatcher() {

		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			int k = 0;
			tempSearchList.clear();
			for (int i = 0; i < locations.size(); i++) {
				if (!s.toString().equals("")
						&& locations.get(i).get("abt").toLowerCase()
								.startsWith(s.toString().toLowerCase()))
					tempSearchList.add(locations.get(i).get("abt"));
			}

			if (tempSearchList.size() > 0) {
				adapter = new ArrayAdapter<String>(MapActivity.this,
						android.R.layout.simple_list_item_1, tempSearchList);
				ListView list = (ListView) findViewById(R.id.list_search);

				list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						edSearch.setText(tempSearchList.get(arg2));
					}
				});

				// adapter.getFilter().filter(s);
				list.setAdapter(adapter);
				list.invalidate();
			}
		}

	};

	private static void importMarkers(String filePath, Context ctx) {

		File importFile = new File(filePath);
	if (importFile.exists()) {

			String content = "";
			//locations = new ArrayList<HashMap<String, String>>();
			String[] tempArray, tempArray1;
			try {
				importFile.createNewFile();

				FileInputStream fis = new FileInputStream(importFile);
				byte[] input;

				input = new byte[fis.available()];
				if (fis.available() > 0) {
					while (fis.read(input) != -1) {
					}

					content += new String(input);

					if (!content.startsWith("#DOS#")) {
						Toast.makeText(ctx, "Invalid File Format",
								Toast.LENGTH_LONG).show();
						fis.close();
						return;
					} else {
						content = content.replace("#DOS#", "");
						content = content.replace("\n", "");
						content = content.replace("\r", "");
						content = content.trim();
					}

					map.getOverlays().clear();

					if (!content.equals("")) {
						tempArray = content.split("--");

						HashMap<String, String> tempHash;
						for (int i = 0; i < tempArray.length; i++) {
							tempHash = new HashMap<String, String>();
							tempArray1 = tempArray[i].split(":");
							tempHash.put("abt", tempArray1[0]);
							tempHash.put("pt", tempArray1[1]);
							if(!locations.contains(tempHash))
							locations.add(tempHash);

							allImports.add(tempArray1[0]);
						}

						String[] pt = new String[2];
						GeoPoint point;
						List<OverlayItem> aList = new LinkedList<OverlayItem>();
						for (int i = 0; i < locations.size(); i++) {

							pt = locations.get(i).get("pt").split(",");
							point = new GeoPoint(Integer.decode(pt[0]),
									Integer.decode(pt[1]));
							// point.setCoordsE6(Integer.decode(pt[0]),
							// Integer.decode(pt[1]));
							OverlayItem overlayitem = new OverlayItem(
									"What\'s Here?", locations.get(i)
											.get("abt"), point);
							aList.add(overlayitem);

						}
						MyItemizedOverlay mItemizedOverlay = new MyItemizedOverlay(
								ctx, aList);
						map.getOverlays().add(mItemizedOverlay);
					}
					// mapController.setCenter(startPoint);
					map.invalidate();
				}
				fis.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	// ------------ Option Menu implementation

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent myIntent;
		LinearLayout l, lMain;
		switch (item.getItemId()) {
		case R.id.menu_search:

			hideAllLayouts();

			lMain = (LinearLayout) findViewById(R.id.main_layout);
			lMain.setVisibility(LinearLayout.VISIBLE);

			l = (LinearLayout) findViewById(R.id.layout_search);
			l.setVisibility(LinearLayout.VISIBLE);

			return true;

		case R.id.menu_create_marker:

			hideAllLayouts();

			lMain = (LinearLayout) findViewById(R.id.main_layout);
			lMain.setVisibility(LinearLayout.VISIBLE);

			l = (LinearLayout) findViewById(R.id.create_marker_layout);
			l.setVisibility(LinearLayout.VISIBLE);
			return true;

		case R.id.menu_clear:
			locations.clear();
			writeLocationsToFile();
			importMarkers("/sdcard/NSSDOS/allFile.txt", GLOBAL_CONTEXT);
			return true;

		case R.id.menu_centre_me:
			// mapController.setCenter(centerMe);
			mapController.animateTo(centerMe, AnimationType.MIDDLEPEAKSPEED);

			return true;

		case R.id.menu_import_data:
			// mapController.setCenter(centerMe);
			Intent i = new Intent(MapActivity.this, ActivityImport.class);
			startActivityForResult(i, IMPORT_REQUEST);

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// ------------ LocationListener implementation
	@Override
	public void onLocationChanged(final Location pLoc) {
		centerMe = new GeoPoint(pLoc);
		myLocationOverlay.setLocation(centerMe);

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		writeLocationsToFile();
		locationManager.removeUpdates(this);
		super.onDestroy();

	}

	static double pixelYToLatitude(double pixelY, byte zoom, int TILE_SIZE) {
        double y = 0.5 - (pixelY / ((long) TILE_SIZE << zoom));
        return 90 - 360 * Math.atan(Math.exp(-y * (2 * Math.PI))) / Math.PI;
    }
	
	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
