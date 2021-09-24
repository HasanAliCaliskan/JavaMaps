package com.hasanali.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.hasanali.javamaps.R;
import com.hasanali.javamaps.adapter.PlaceAdapter;
import com.hasanali.javamaps.databinding.ActivityMainBinding;
import com.hasanali.javamaps.model.Place;
import com.hasanali.javamaps.roomdb.PlaceDao;
import com.hasanali.javamaps.roomdb.PlaceDatabase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CompositeDisposable compositeDisposable;
    public SharedPreferences sharedPreferences;
    public boolean info;
    ActivityResultLauncher<String> permissionLauncher;
    PlaceDatabase db;
    PlaceDao placeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        registerLauncher();
        sharedPreferences = this.getSharedPreferences("com.hasanali.javamaps.view",MODE_PRIVATE);
        info = sharedPreferences.getBoolean("info",true);

        compositeDisposable = new CompositeDisposable();
        db = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places").build();
        placeDao = db.placeDao();

        if (info) {
            famousPlaces();
        }
        compositeDisposable.add(placeDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MainActivity.this :: handleResponse)
        );
    }

    private void handleResponse(List<Place> placeList) {
        PlaceAdapter placeAdapter = new PlaceAdapter(placeList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(placeAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.travel_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add_place) {
            setPermissionLauncher();
        }
        return super.onOptionsItemSelected(item);
    }

    private void registerLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        intent.putExtra("info","new");
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(MainActivity.this,"Permission needed!",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setPermissionLauncher() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(findViewById(R.id.linearLayout_for_snackbar),"Permission needed for location.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                            }
                        }).show();
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }
    }

    private void famousPlaces() {
        Place place = new Place("Times Square",40.75795247860753, -73.98556004829943);
        Place place2 = new Place("Pyramid of Cheops",29.979169320397418, 31.13423090780226);
        Place place3 = new Place("Eiffel Tower",48.85808786473996, 2.294534432090054);
        Place place4 = new Place("Colosseum",41.889938647524495, 12.492273815343998);
        Place place5 = new Place("Hagia Sophia",41.00869632461792, 28.980164268491333);
        Place place6 = new Place("Taj Mahal",27.175077971300297, 78.04214219691592);
        Place place7 = new Place("Sydney Opera House",-33.85679894627444, 151.21529698503042);
        Place place8 = new Place("Statue of Liberty",40.68924049257648, -74.04449661905508);
        Place place9 = new Place("Sagrada Familia",41.40361328645801, 2.174361317076419);
        Place place0 = new Place("Leaning Tower of Pisa",43.72294098133854, 10.396599074142177);
        ArrayList<Place> arrayList = new ArrayList<>();
        arrayList.add(0,place);
        arrayList.add(1,place2);
        arrayList.add(2,place3);
        arrayList.add(3,place4);
        arrayList.add(4,place5);
        arrayList.add(5,place6);
        arrayList.add(6,place7);
        arrayList.add(7,place8);
        arrayList.add(8,place9);
        arrayList.add(9,place0);
        for (int i = 0; i < arrayList.size(); i++) {
            compositeDisposable.add(placeDao.insert(arrayList.get(i))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
            );
        }
        sharedPreferences.edit().putBoolean("info",false).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}