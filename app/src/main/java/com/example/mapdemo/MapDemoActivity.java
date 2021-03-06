package com.example.mapdemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import model.Restaurant;


import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class git remote addMapDemoActivity extends AppCompatActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		LocationListener {

	private SupportMapFragment mapFragment;
	private GoogleMap map;
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
	private long UPDATE_INTERVAL = 60000;  /* 60 secs */
	private long FASTEST_IGITNTERVAL = 5000; /* 5 secs */

	/*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	//public static ArrayList<Restaurant> restaurantsList= new ArrayList<Restaurant>();
	Location currentLocation;
	Restaurant nearestRestaurant;
	//JSON info
	String strJSONUrl = "https://gist.githubusercontent.com/diamantoula/8389024a52b99cbd427cc500826c9227/raw/cd006a190e99f619e5942494bd50f34fe79ef656/restaurants";
	String strJSONArray = "restaurants";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_demo_activity);

		mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
		if (mapFragment != null) {
			mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    loadMap(map);
                }
            });
		} else {
			Toast.makeText(this, "Error - Map Fragment was null!!", Toast.LENGTH_SHORT).show();
		}

	}

    protected void loadMap(GoogleMap googleMap) {
        map = googleMap;
        if (map != null) {
            // Map is ready
			MapDemoActivityPermissionsDispatcher.getMyLocationWithCheck(this);
        } else {
            Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
        }
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		MapDemoActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
	}

	@SuppressWarnings("all")
	@NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
	void getMyLocation() {
		if (map != null) {
			// Now that map has loaded, let's get our location!
			map.setMyLocationEnabled(true);
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addApi(LocationServices.API)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this).build();
			connectClient();
		}
	}

    protected void connectClient() {
        // Connect the client.
        if (isGooglePlayServicesAvailable() && mGoogleApiClient != null) {
			//connects to the service in the backround
            mGoogleApiClient.connect();
        }
    }

    /*
     * Called when the Activity becomes visible.
    */
    @Override
    protected void onStart() {
        super.onStart();
        connectClient();
    }

    /*
	 * Called when the Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
		super.onStop();
	}

	/*
	 * Handle results returned to the FragmentActivity by Google Play services
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {

		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK:
				mGoogleApiClient.connect();
				break;
			}

		}
	}

	private boolean isGooglePlayServicesAvailable() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			return true;
		} else {
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
					CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(errorDialog);
				errorFragment.show(getSupportFragmentManager(), "Location Updates");
			}

			return false;
		}
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle dataBundle) {
		// Display the connection status
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (location != null) {
			Toast.makeText(this, "GPS location was found!", Toast.LENGTH_SHORT).show();
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

			currentLocation = location;
			String strCurrent = Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude());

			TextView displayCurrentLoc = (TextView) findViewById(R.id.textView2);
			displayCurrentLoc.setText( strCurrent );

			setMarker(currentLocation.getLatitude(), currentLocation.getLongitude(), "Current Location");

			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
			map.animateCamera(cameraUpdate);
        } else {
			Toast.makeText(this, "Current location was null, enable GPS!", Toast.LENGTH_SHORT).show();
		}
	}

    protected void locationUpdate() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Current Location: " + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

	public void searchOnClick(View v){
		//Button button = (Button) v;
		//new restaurantsAsyncTask().execute(strJSONUrl);
		locationUpdate();

		ArrayList<Restaurant> restaurants= new ArrayList<Restaurant>();
		restaurants.add(new Restaurant(2, "Vlaxos", "Kountouriwtou 12", 41.090189, 23.548197));
		restaurants.add(new Restaurant(3, "Kapileio tou Kwsti", "Megalou Alexandrou 28", 41.091384, 23.556460));
		restaurants.add(new Restaurant(4, "Hlias", "Spetswn 6", 41.085495, 23.545296));
		restaurants.add(new Restaurant(1, "Ellinwn Geuseis", "Makenomaxwn 35", 41.083073, 23.551652));
		restaurants.add(new Restaurant(5, "Ntomata", "Anapausews 7", 41.098007, 23.554818));

		nearestRestau
				rant = restaurants.get( findNearestRestaurant(restaurants, currentLocation) );
		String strNearest = nearestRestaurant.getName() + ", " + nearestRestaurant.getAddress();

		TextView displayNearestRes = (TextView) findViewById(R.id.textView4);
		displayNearestRes.setText( strNearest );

		setMarker(nearestRestaurant.getLat(), nearestRestaurant.getLng(), strNearest);
	}

	public int findNearestRestaurant(ArrayList<Restaurant> arrayList, Location currentLoc){
		//initialize position
		int pos=0;
		//initialize compareLoc with 1st element from arrayList
		Location compareLoc = new Location("");
		compareLoc.setLatitude(arrayList.get(0).getLat());
		compareLoc.setLongitude(arrayList.get(0).getLng());
		//initialize distance, between compareLoc and 1st element
		float distance = currentLoc.distanceTo(compareLoc);

		for(int i=1; i<arrayList.size(); i++){
			compareLoc.setLatitude(arrayList.get(i).getLat());
			compareLoc.setLongitude(arrayList.get(i).getLng());
			if( currentLoc.distanceTo(compareLoc) < distance ){
				distance = currentLoc.distanceTo(compareLoc);
				pos=i;
			}
		}
		return pos;
	}

	public void setMarker(double lat1, double lng1, String title1){
		LatLng latLng = new LatLng(lat1, lng1);
		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(latLng);
		markerOptions.title(title1);
		Marker marker = map.addMarker(markerOptions);
	}

    /*
     * Called by Location Services if the connection to the location client
     * drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			Toast.makeText(getApplicationContext(),
					"Sorry. Location services not available to you", Toast.LENGTH_LONG).show();
		}
	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {

		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	//This class allows you to perform background operations and publish results on the UI
	//<params, progress, result>
	/*public class restaurantsAsyncTask extends AsyncTask<String, String, ArrayList<Restaurant>> {

		//get restaurants info from json file and store it to an ArrayList
		@Override
		protected ArrayList<Restaurant> doInBackground(String... params) {
			HttpURLConnection connection = null;
			BufferedReader reader = null;

			try {
				//convert string url to URL
				URL url = new URL(params[0]);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				//connection returns InputStream so we create an InputStream obj
				InputStream stream = connection.getInputStream();
				//with a BufferedReader we read the InputStream
				reader = new BufferedReader(new InputStreamReader(stream));

				StringBuffer buffer = new StringBuffer();

				//read line by line
				String line = "";
				while ( (line = reader.readLine()) != null ){
					//add line to buffer
					buffer.append(line);
				}
				//buffer returns complete json
				String completeJson = buffer.toString();

				JSONObject parentObj = new JSONObject(completeJson);
				JSONArray jsonArray = parentObj.getJSONArray(strJSONArray);
				ArrayList<Restaurant> resList = new ArrayList<>();

				for (int i=0; i<jsonArray.length(); i++){
					JSONObject childObj = jsonArray.getJSONObject(i);
					Restaurant res = new Restaurant();
					res.id = childObj.getInt("id");
					res.name = childObj.getString("name");
					res.address = childObj.getString("address");
					res.lat = childObj.getDouble("lat");
					res.lng = childObj.getDouble("lng");
					resList.add(res);
				}
				return resList;

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				//if null there is no connection to disconnect
				if(connection != null){
					connection.disconnect();
				}
				try {
					//if null there is no reader to close
					if(reader != null){
						reader.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//if not successful
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<Restaurant> result) {

			super.onPostExecute(result);
		}
	}*/

}
