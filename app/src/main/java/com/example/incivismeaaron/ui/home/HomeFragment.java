package com.example.incivismeaaron.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.incivismeaaron.databinding.FragmentHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastLocation;

    public View onCreateView(@NonNull LayoutInflater inflater,

                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());



        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        getLocation();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        getLocation();
                    } else {
                        Toast.makeText(requireContext(), "No concedeixen permisos", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.btGet.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getLocation();
                    }
                }
        );
        return root;
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(requireContext(), "Request permisssions", Toast.LENGTH_SHORT).show();
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            Toast.makeText(requireContext(), "getLocation: permissions granted", Toast.LENGTH_SHORT).show();
            mFusedLocationClient.getLastLocation().addOnSuccessListener(
                    location -> {
                        if (location != null) {
                            fetchAddress(location);
                        } else {
                            binding.tLocal.setText("Sense localització coneguda");
                        }
                    });
        }
    }

    private void fetchAddress(Location location) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        Geocoder geocoder = new Geocoder(requireContext(),
                Locale.getDefault());

        executor.execute(() -> {
            // Aquest codi s'executa en segon pla
            List<Address> addresses = null;
            String resultMessage = "";

            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        // En aquest cas, sols volem una única adreça:
                        1);


                if (addresses == null || addresses.size() == 0) {
                    if (resultMessage.isEmpty()) {
                        resultMessage = "No s'ha trobat cap adreça";
                        Log.e("INCIVISME", resultMessage);
                    }
                } else {
                    Address address = addresses.get(0);
                    ArrayList<String> addressParts = new ArrayList<>();

                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressParts.add(address.getAddressLine(i));
                    }

                    resultMessage = TextUtils.join("\n", addressParts);
                    String finalResultMessage = resultMessage;
                    handler.post(() -> {
                        // Aquest codi s'executa en primer pla.
                        binding.tLocal.setText(String.format(
                                "Direcció: %1$s \n Hora: %2$tr",
                                finalResultMessage, System.currentTimeMillis()));
                    });
                }

            } catch (IOException ioException) {
                resultMessage = "Servei no disponible";
                Log.e("INCIVISME", resultMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                resultMessage = "Coordenades no vàlides";
                Log.e("INCIVISME", resultMessage + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " +
                        location.getLongitude(), illegalArgumentException);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}