package com.cropiotest.fields;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.cropiotest.fields.model.Field;
import com.cropiotest.fields.model.Polygon;
import com.cropiotest.fields.model.Ring;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.Locale;
import java.util.Map;

public class FieldInfoActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;

    private static final int PERMISSIONS_LOCATION = 0;

    TextView tvName;
    TextView tvCrop;
    TextView tvTillArea;

    Field field;

    boolean isMyLocationEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxAccountManager.start(this, getString(R.string.access_token));
        setContentView(R.layout.activity_field_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        field = bundle.getParcelable(API.KEY_FIELD);

        if (savedInstanceState != null && savedInstanceState.containsKey(API.KEY_IS_MY_LOCATION_ENABLED)) {
            isMyLocationEnabled = savedInstanceState.getBoolean(API.KEY_IS_MY_LOCATION_ENABLED);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(field.name);
        }

        tvName = (TextView) findViewById(R.id.tvName);
        tvCrop = (TextView) findViewById(R.id.tvCrop);
        tvTillArea = (TextView) findViewById(R.id.tvTillArea);

        tvName.setText(field.name);
        tvCrop.setText(field.crop);
        tvTillArea.setText(String.format(Locale.US, "%d ha", (int) field.tillArea));

        locationServices = LocationServices.getLocationServices(FieldInfoActivity.this);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                if (isMyLocationEnabled) {
                    toggleGps(map.isMyLocationEnabled());
                }

                setPolygons();
            }
        });

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fabLocation);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });
    }

    void setPolygons() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Map.Entry<Integer, Polygon> entry : field.polygons.entrySet()) {
            Polygon polygon = entry.getValue();
            for (Map.Entry<Integer, Ring> entry2 : polygon.rings.entrySet()) {
                Ring ring = entry2.getValue();
                for (LatLng point : ring.coordinates) {
                    builder.include(point);
                }
                map.addPolygon(new PolygonOptions()
                        .alpha(0.6f)
                        .addAll(ring.coordinates)
                        .strokeColor(Color.WHITE)
                        .fillColor(Color.WHITE));
                // break;//no holes
            }
        }
        LatLngBounds latLngBounds = builder.build();
        if (!isMyLocationEnabled) {
            map.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50), 500);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        isMyLocationEnabled = enabled;
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationServices.removeLocationListener(this);
                    }
                }
            });
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(API.KEY_IS_MY_LOCATION_ENABLED, isMyLocationEnabled);
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

}
