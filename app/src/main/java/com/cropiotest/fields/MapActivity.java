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

import com.cropiotest.fields.model.Field;
import com.cropiotest.fields.model.Polygon;
import com.cropiotest.fields.model.Ring;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;

    private static final int PERMISSIONS_LOCATION = 0;

    List<Field> fields;

    boolean isMyLocationEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxAccountManager.start(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        fields = bundle.getParcelableArrayList(API.KEY_FIELDS);

        if (savedInstanceState != null && savedInstanceState.containsKey(API.KEY_IS_MY_LOCATION_ENABLED)) {
            isMyLocationEnabled = savedInstanceState.getBoolean(API.KEY_IS_MY_LOCATION_ENABLED);
        }

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fabLocation);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });
        locationServices = LocationServices.getLocationServices(MapActivity.this);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                if (isMyLocationEnabled) {
                    toggleGps(map.isMyLocationEnabled());
                }
                if (map.getPolygons().size() == 0) {
                    setPolygons(isMyLocationEnabled);
                }

                map.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {

                        for (Field field : fields) {
                            for (Map.Entry<Integer, Polygon> entry : field.polygons.entrySet()) {
                                Polygon polygon = entry.getValue();
                                for (Map.Entry<Integer, Ring> entry2 : polygon.rings.entrySet()) {
                                    Ring ring = entry2.getValue();
                                    if (PointPolygonComparator.isPointInPolygon(new LatLng(point.getLatitude(), point.getLongitude()), ring.coordinates)) {
                                        Intent intent = new Intent(getApplicationContext(), FieldInfoActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putParcelable(API.KEY_FIELD, field);
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        return;
                                    }
                                 //   break;//no holes
                                }
                            }
                        }
                    }
                });

            }
        });

    }

    void setPolygons(boolean isMyLocationEnabled) {
        Map<String, Integer> cropColors = getMapCropColor();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Field field : fields) {
            for (Map.Entry<Integer, Polygon> entry : field.polygons.entrySet()) {
                Polygon polygon = entry.getValue();
                for (Map.Entry<Integer, Ring> entry2 : polygon.rings.entrySet()) {
                    Ring ring = entry2.getValue();
                    for (LatLng point : ring.coordinates) {
                        builder.include(point);
                    }

                    if (entry.getKey() == 0 && entry2.getKey() == 0) {
                        LatLng centroid = getPolygonCentroid(ring.coordinates);
                        MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                                .position(centroid).title(field.name);

                        map.addMarker(markerViewOptions);
                    }

                    map.addPolygon(new PolygonOptions()
                            .alpha(0.6f)
                            .addAll(ring.coordinates)
                            .strokeColor(Color.WHITE)
                            .fillColor(cropColors.get(field.crop)));
                  //  break;//no holes
                }
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

    Map<String, Integer> getMapCropColor() {
        Map<String, Integer> crops = new HashMap();
        for (Field field : fields) {
            if (!crops.containsKey(field.crop)) {
                crops.put(field.crop, getRandomColor());
            }
        }
        return crops;
    }

    public LatLng getPolygonCentroid(List<LatLng> points)  {
        double centroidX = 0, centroidY = 0;

        for (LatLng point : points) {
            centroidX += point.getLatitude();
            centroidY += point.getLongitude();
        }
        return new LatLng(centroidX / points.size(), centroidY / points.size());
    }

    public int getRandomColor(){
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
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
