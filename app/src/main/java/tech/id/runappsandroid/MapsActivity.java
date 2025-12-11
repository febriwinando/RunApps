package tech.id.runappsandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private static final int ACCESS_LOCATION_REQUEST_CODE = 10001;

    private GoogleMap mMap;
    private Geocoder geocoder;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;

    private Marker userLocationMarker;
    private Polyline currentPolyline;

    private LatLng destinationLatLng = null;

    private DirectionsApiService apiService;
    private final String API_KEY = "AIzaSyCoaTdmJpOr4pcmlU6HFjXtuX0e7HUKYpc"; // Ganti

    // throttling: interval time & distance
    private long lastDirectionUpdateTime = 0;
    private final long DIRECTION_UPDATE_INTERVAL_MS = 3000; // 3 detik
    private LatLng lastDirectionOrigin = null;
    private final float DIRECTION_UPDATE_DISTANCE_M = 12f; // 12 meter

    // UI overlay
    private TextView tvStatus, tvDistance, tvDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        tvStatus = findViewById(R.id.tvStatus);
        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);

        geocoder = new Geocoder(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // LocationRequest (sesuaikan jika menggunakan versi Play Services terbaru)
        locationRequest = LocationRequest.create();
        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000 // interval (ms)
        ).setMinUpdateIntervalMillis(2000)      // fastest interval
                .setMaxUpdateDelayMillis(0)            // no batching
                .build();

//        locationRequest.setInterval(1500);
//        locationRequest.setFastestInterval(1000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Retrofit init
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(DirectionsApiService.class);

        // set statis destination (coordinate yang Anda berikan)
        destinationLatLng = new LatLng(3.3273234739040842, 99.16632742970482);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Location callback
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location loc = locationResult.getLastLocation();
            if (loc != null && mMap != null) {
                setUserLocationMarker(loc);
            }
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // add destination marker
        if (destinationLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title("Tujuan")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // check permission & zoom user location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableAndZoomToUser();
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
        }
    }

    // enable my-location layer and move camera to last known location
    private void enableAndZoomToUser() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        mMap.setMyLocationEnabled(false); // we use custom marker, set false to avoid default blue dot

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, (OnSuccessListener<Location>) location -> {
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        tvStatus.setText("Tracking started");
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        tvStatus.setText("Tracking stopped");
    }

    private void setUserLocationMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.delivery)) // pakai icon delivery Anda
                    .anchor(0.5f, 0.5f)
                    .rotation(location.getBearing());
            userLocationMarker = mMap.addMarker(markerOptions);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        } else {
            // smooth-ish update: setPosition + rotation
            userLocationMarker.setPosition(latLng);
            userLocationMarker.setRotation(location.getBearing());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }

        // update direction dengan throttling (time + distance)
        if (destinationLatLng != null) {
            long now = System.currentTimeMillis();

            boolean timeOk = (now - lastDirectionUpdateTime) > DIRECTION_UPDATE_INTERVAL_MS;
            boolean distOk = true;
            if (lastDirectionOrigin != null) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                        lastDirectionOrigin.latitude, lastDirectionOrigin.longitude,
                        latLng.latitude, latLng.longitude, results);
                distOk = results[0] >= DIRECTION_UPDATE_DISTANCE_M;
            }

            if (lastDirectionOrigin == null || (timeOk && distOk)) {
                lastDirectionUpdateTime = now;
                lastDirectionOrigin = latLng;
                fetchDirection(latLng, destinationLatLng);
            }
        }
    }

    // fetch direction from Google Directions API and draw polyline + update UI
    private void fetchDirection(LatLng origin, LatLng destination) {
        tvStatus.setText("Fetching route...");
        String originStr = origin.latitude + "," + origin.longitude;
        String destStr = destination.latitude + "," + destination.longitude;

        apiService.getDirection(originStr, destStr, API_KEY, false)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            tvStatus.setText("Route error");
                            Log.w(TAG, "Direction response null / unsuccessful");
                            return;
                        }

                        DirectionsResponse body = response.body();
                        if (body.routes == null || body.routes.size() == 0) {
                            tvStatus.setText("No route");
                            return;
                        }

                        DirectionsResponse.Route route = body.routes.get(0);

                        // decode polyline
                        String polylineEncoded = route.overview_polyline.points;
                        List<LatLng> decodedPath = DecodePoly.decodePoly(polylineEncoded);

                        // remove previous polyline
                        if (currentPolyline != null) {
                            currentPolyline.remove();
                        }

                        PolylineOptions opts = new PolylineOptions()
                                .addAll(decodedPath)
                                .width(12)
                                .color(Color.parseColor("#1976D2"))
                                .geodesic(true);
                        currentPolyline = mMap.addPolyline(opts);

                        // update distance & duration from first leg if available
                        if (route.legs != null && route.legs.size() > 0) {
                            DirectionsResponse.Leg leg = route.legs.get(0);
                            if (leg.distance != null) {
                                tvDistance.setText("Distance: " + leg.distance.text);
                            }
                            if (leg.duration != null) {
                                tvDuration.setText("ETA: " + leg.duration.text);
                            }
                        }

                        tvStatus.setText("Route updated");
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        tvStatus.setText("Route failed");
                        Log.e(TAG, "Directions API failure", t);
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    // simple permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ACCESS_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    enableAndZoomToUser();
                }
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
