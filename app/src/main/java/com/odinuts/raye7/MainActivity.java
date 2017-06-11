package com.odinuts.raye7;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private GoogleMap map;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 3;
    int id;
    Marker marker;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.toolbar_tv)
    TextView toolbarTv;

    @BindView(R.id.notification_iv)
    ImageView notificationsIv;

    @OnClick(R.id.notification_iv)
    public void showNotifications(View view) {
        Snackbar.make(view, "You can view your notifications here.", Snackbar.LENGTH_SHORT).show();
    }

    @BindView(R.id.messages_iv)
    ImageView messagesIv;

    @OnClick(R.id.messages_iv)
    public void showMessages(View view) {
        Snackbar.make(view, "You can view your messages here.", Snackbar.LENGTH_SHORT).show();
    }

    @BindView(R.id.switcher_iv)
    ImageView switcher;

    @OnClick(R.id.switcher_iv)
    public void switchEntries() {
        String from = fromEt.getText().toString();
        fromEt.setText(toEt.getText());
        toEt.setText(from);
    }

    @BindView(R.id.from_et)
    EditText fromEt;

    @OnClick(R.id.from_et)
    public void selectFromPlace() {
        id = R.id.from_et;
        launchPlacesAutoComplete();
    }

    @BindView(R.id.to_et)
    EditText toEt;

    @OnClick(R.id.to_et)
    public void selectToPlace() {
        id = R.id.to_et;
        launchPlacesAutoComplete();
    }

    @BindView(R.id.take_car_btn)
    Button takeCarBtn;

    @OnClick(R.id.take_car_btn)
    public void draw(View view) {
        if (fromEt.getText().length() != 0 && toEt.getText().length() != 0) {
            Snackbar.make(view, "Your car will be used to take you from: " +
                    fromEt.getText().toString() + " to: "
                    + toEt.getText().toString(), Snackbar.LENGTH_SHORT).show();
        }
        drawLineBetweenLocations();
    }

    @BindView(R.id.req_pickup_btn)
    Button requestPickupBtn;

    @OnClick(R.id.req_pickup_btn)
    public void drawLine(View view) {
        if (fromEt.getText().length() != 0 && toEt.getText().length() != 0) {
            Snackbar.make(view, "A car will take you from: " + fromEt.getText().toString() + " to: "
                    + toEt.getText().toString(), Snackbar.LENGTH_SHORT).show();
        }
        drawLineBetweenLocations();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        id = 0;
        marker = null;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        } catch (Exception ex) {
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
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

    private void drawLineBetweenLocations() {
        // TODO
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                switch (id) {
                    case R.id.from_et:
                        fromEt.setText(place.getName());
                        break;
                    case R.id.to_et:
                        toEt.setText(place.getName());
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
}