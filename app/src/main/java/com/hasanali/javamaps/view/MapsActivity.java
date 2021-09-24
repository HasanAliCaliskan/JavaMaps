package com.hasanali.javamaps.view;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hasanali.javamaps.R;
import com.hasanali.javamaps.databinding.ActivityMapsBinding;
import com.hasanali.javamaps.model.Place;
import com.hasanali.javamaps.roomdb.PlaceDao;
import com.hasanali.javamaps.roomdb.PlaceDatabase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener{

    private ActivityMapsBinding binding;
    private CompositeDisposable compositeDisposable;
    LocationListener locationListener;
    private GoogleMap mMap;
    LocationManager locationManager;
    SharedPreferences sharedPreferences;
    PlaceDatabase db;
    PlaceDao placeDao;
    Place selectedPlace;
    Double selectedLatitude;
    Double selectedLongitude;
    String intentInfo;
    boolean info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        compositeDisposable = new CompositeDisposable();
        sharedPreferences = this.getSharedPreferences("com.hasanali.javamaps",MODE_PRIVATE);
        info = false;

        db = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places").build();
        placeDao = db.placeDao();

        selectedLatitude = 0.0;
        selectedLongitude = 0.0;
        binding.saveButton.setEnabled(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        getIntentInfos();
        setIntentInfos();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;
        binding.saveButton.setEnabled(true);
    }

    public void save(View view) {
        if (binding.placeNameText.getText().toString().matches("")) {
            Toast.makeText(MapsActivity.this,"Place name cannot be left blank!",Toast.LENGTH_LONG).show();
        } else {
            String placeName = binding.placeNameText.getText().toString();
            Place place = new Place(placeName, selectedLatitude, selectedLongitude);
            compositeDisposable.add(placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this :: handleResponse)
            );
        }
    }

    public void delete(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete").setMessage("Are you sure want to delete this location?");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (selectedPlace != null) {
                    compositeDisposable.add(placeDao.delete(selectedPlace)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(MapsActivity.this::handleResponse)
                    );
                }
                Toast.makeText(MapsActivity.this,"Deleted.",Toast.LENGTH_LONG).show();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MapsActivity.this,"Not deleted.",Toast.LENGTH_LONG).show();
            }
        }).show();
    }

    private void handleResponse() {
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void getIntentInfos() {
        Intent intent = getIntent();
        intentInfo = intent.getStringExtra("info");
        selectedPlace = (Place) intent.getSerializableExtra("place");
    }

    private void setIntentInfos() {
        if (intentInfo.equals("new")) {
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);
            setLocationManager();

            if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null) {
                    LatLng lastL = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(lastL));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastL,13));
                }
            }
        } else {
            mMap.clear();
            LatLng selectedLatLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
            mMap.addMarker(new MarkerOptions().position(selectedLatLng));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng,13));
            binding.placeNameText.setText(selectedPlace.name);
            binding.placeNameText.setEnabled(false);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);
        }
    }

    private void setLocationManager() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                info = sharedPreferences.getBoolean("info",false);
                if (!info) {
                    LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(userLocation));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,13));
                    sharedPreferences.edit().putBoolean("info", true).apply();
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}