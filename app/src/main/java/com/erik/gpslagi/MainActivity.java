package com.erik.gpslagi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(-7.28,112.79);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted = false;

    private Location lastKnownLocation;
    private LocationRequest mLocationRequest;
    private GoogleMap gMap;
    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    ImageButton btnIts;
    ImageButton btnGo;
    ImageButton btnStartStop;
    EditText etLng;
    EditText etLat;
    TextView tvLastUp;

    String lastUpdate;

    private boolean isLocationRealtime = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupComponentView();

        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.typeNormal: gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); break;
            case R.id.typeHybrid: gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); break;
            case R.id.typeTerrain: gMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN); break;
            case R.id.typeSattelite: gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE   ); break;
            case R.id.typeNone: gMap.setMapType(GoogleMap.MAP_TYPE_NONE); break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupComponentView(){
        btnIts = findViewById(R.id.btnIts);
        btnGo = findViewById(R.id.btnGo);
        btnStartStop = findViewById(R.id.btnStartStop);
        etLng = findViewById(R.id.etLng);
        etLat = findViewById(R.id.etLat);
        tvLastUp = findViewById(R.id.tvLastUp);

        btnGo.setOnClickListener(op);
        btnIts.setOnClickListener(op);
        btnStartStop.setOnClickListener(op);
    }

    View.OnClickListener op = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnIts:
                    goToIts();
                    break;
                case R.id.btnGo:
                    goToLatLng();
                    break;
                case R.id.btnStartStop:
                    handleRealtimeLocation();
                    break;
            }
        }
    };

    private void goToLatLng(){
        Double lat, lng;

        if(!etLat.getText().toString().isEmpty() && !etLat.getText().toString().isEmpty()){
            lat = Double.parseDouble(etLat.getText().toString());
            lng = Double.parseDouble(etLng.getText().toString());

            LatLng latLng = new LatLng(lat, lng);
            Toast.makeText(this, "Going to "+ lat + "," + lng, Toast.LENGTH_SHORT).show();
//            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

        }
        else{
            Toast.makeText(this, "LatLng cannot be empty", Toast.LENGTH_LONG).show();
//            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));

        }
    }

    private void goToIts(){
//        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "On Map Ready", Toast.LENGTH_SHORT).show();

        gMap = googleMap;
        gMap.setOnMyLocationButtonClickListener(this);

        gMap.addMarker(new MarkerOptions()
                .position(defaultLocation)
                .title("Institut Teknologi Sepuluh Nopember"));
//        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));


        updateLocationUI();
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        updateLocationUI();
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (gMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                gMap.setMyLocationEnabled(true);
                gMap.getUiSettings().setMyLocationButtonEnabled(true);
                getRealtimeLocation();
            } else {
                gMap.setMyLocationEnabled(false);
                gMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            LatLng curLoc = new LatLng(location.getLatitude(), location.getLongitude());

            lastUpdate = DateFormat.getTimeInstance().format(new Date());
            tvLastUp.setText("Last Update : "+ lastUpdate);

            etLat.setText(String.valueOf(location.getLatitude()));
            etLng.setText(String.valueOf(location.getLongitude()));

//            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLoc, DEFAULT_ZOOM));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLoc, DEFAULT_ZOOM));

        }
    };

    @Override
    public boolean onMyLocationButtonClick() {
        getRealtimeLocation();
        stopRealtimeLocation();
        return false;
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();

                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
//                            gMap.moveCamera(CameraUpdateFactory .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            gMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();

            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @SuppressLint("MissingPermission")
    private void getRealtimeLocation(){
        if(isLocationRealtime)
            return;

        btnStartStop.setImageResource(R.drawable.ic_baseline_stop_24);
        isLocationRealtime = true;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (locationPermissionGranted) {
            fusedLocationProviderClient.requestLocationUpdates(
                    mLocationRequest,
                    locationCallback,
                    Looper.myLooper()
            );
        }
        else{
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();

        }
    }

    private void stopRealtimeLocation(){
        if(!isLocationRealtime)
            return;

        btnStartStop.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        isLocationRealtime = false;

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void handleRealtimeLocation(){
        if(isLocationRealtime){
            stopRealtimeLocation();
        }
        else{
            getRealtimeLocation();
        }
    }


}