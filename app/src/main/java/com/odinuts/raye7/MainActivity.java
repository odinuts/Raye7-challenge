package com.odinuts.raye7;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 3;
    int id;
    Marker marker;
    LatLng origin;
    LatLng destination;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.toolbar_tv)
    TextView toolbarTv;
    @BindView(R.id.notification_iv)
    ImageView notificationsIv;
    @BindView(R.id.messages_iv)
    ImageView messagesIv;
    @BindView(R.id.switcher_iv)
    ImageView switcher;
    @BindView(R.id.from_et)
    EditText fromEt;
    @BindView(R.id.to_et)
    EditText toEt;
    @BindView(R.id.take_car_btn)
    Button takeCarBtn;
    @BindView(R.id.req_pickup_btn)
    Button requestPickupBtn;
    private GoogleMap map;

    @OnClick(R.id.notification_iv)
    public void showNotifications(View view) {
        Snackbar.make(view, "You can view your notifications here.", Snackbar.LENGTH_SHORT).show();
    }

    @OnClick(R.id.messages_iv)
    public void showMessages(View view) {
        Snackbar.make(view, "You can view your messages here.", Snackbar.LENGTH_SHORT).show();
    }

    @OnClick(R.id.switcher_iv)
    public void switchEntries() {
        String from = fromEt.getText().toString();
        fromEt.setText(toEt.getText());
        toEt.setText(from);
    }

    @OnClick(R.id.from_et)
    public void selectFromPlace() {
        id = R.id.from_et;
        launchPlacesAutoComplete();
    }

    @OnClick(R.id.to_et)
    public void selectToPlace() {
        id = R.id.to_et;
        launchPlacesAutoComplete();
    }

    @OnClick(R.id.take_car_btn)
    public void draw(View view) {
        if (fromEt.getText().length() != 0 && toEt.getText().length() != 0) {
            Snackbar.make(view, "Your car will be used to take you from: " +
                    fromEt.getText().toString() + " to: "
                    + toEt.getText().toString(), Snackbar.LENGTH_SHORT).show();
            String url = getUrl(origin, destination);
            FetchUrl fetchUrl = new FetchUrl();
            fetchUrl.execute(url);
            map.animateCamera(CameraUpdateFactory.newLatLng(origin));
            map.animateCamera(CameraUpdateFactory.zoomTo(10));
        }
    }

    @OnClick(R.id.req_pickup_btn)
    public void drawLine(View view) {
        if (fromEt.getText().length() != 0 && toEt.getText().length() != 0) {
            Snackbar.make(view, "A car will take you from: " +
                    fromEt.getText().toString() + " to: "
                    + toEt.getText().toString(), Snackbar.LENGTH_SHORT).show();
            String url = getUrl(origin, destination);
            FetchUrl fetchUrl = new FetchUrl();
            fetchUrl.execute(url);
            map.animateCamera(CameraUpdateFactory.newLatLng(origin));
            map.animateCamera(CameraUpdateFactory.zoomTo(10));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        id = 0;
        marker = null;
        origin = null;
        destination = null;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        buildGoogleApiClient();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                .ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        if (!gpsEnabled && !networkEnabled) {
            // notify user
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(this.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(this.getResources().getString(R.string.open_location_settings),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            MainActivity.this.startActivity(myIntent);
                        }
                    });

            dialog.setNegativeButton(this.getString(R.string.cancel), new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            Toast.makeText(MainActivity.this,
                                    "The app won't work until you enable location services",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            dialog.show();
        }
    }

    private void buildGoogleApiClient() {
        GoogleApiClient googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(MainActivity.this, "Connection failed. " +
                                "Please try again later", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.
                checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        map.setMyLocationEnabled(true);
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                Location location = map.getMyLocation();
                List<Address> addresses = null;
                Geocoder geo = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    addresses = geo.getFromLocation(location.getLatitude(),
                            location.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses != null) {
                    fromEt.setText(addresses.get(0).getAddressLine(0));
                }
                return false;
            }
        });

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (marker != null) {
                    marker.remove();
                } else {
                    map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("You're here!")
                            .icon(BitmapDescriptorFactory.
                                    defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                }

                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                toEt.setText(addresses != null ? addresses.get(0).getAddressLine(0) : null);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                switch (id) {
                    case R.id.from_et:
                        fromEt.setText(place.getName());
                        origin = place.getLatLng();
                        break;
                    case R.id.to_et:
                        toEt.setText(place.getName());
                        destination = place.getLatLng();
                        break;
                    default:
                        break;
                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Toast.makeText(this, R.string.err, Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.request_canceled, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchPlacesAutoComplete() {

        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException |
                GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private String getUrl(LatLng origin, LatLng dest) {

        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        String output = "json";
        return getString(R.string.url) + output + "?" + parameters;
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = null;
            try {
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }

        private String downloadUrl(String strUrl) throws IOException {
            String data = null;
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(strUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                iStream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();
                br.close();

            } catch (Exception ignored) {

            } finally {
                if (iStream != null) {
                    iStream.close();
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return data;
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DataParser parser = new DataParser();
                routes = parser.parse(jObject);

            } catch (Exception ignored) {
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(20);
                lineOptions.color(Color.RED);
            }
            if (lineOptions != null) {
                map.addPolyline(lineOptions);
            }
        }
    }
}