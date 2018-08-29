/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.customer.view.place;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mark.zumo.client.core.entity.Store;
import com.mark.zumo.client.customer.R;
import com.mark.zumo.client.customer.view.place.adapter.NearbyStoreAdapter;
import com.mark.zumo.client.customer.viewmodel.PlaceViewModel;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mark on 18. 5. 18.
 */
public class PlaceFragment extends Fragment {

    private static final int REQUEST_CODE_PLACE_PICKER = 15;

    @BindView(R.id.near_by_store) RecyclerView nearByStore;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

    private PlaceViewModel placeViewModel;
    private SupportMapFragment mapFragment;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        placeViewModel = ViewModelProviders.of(this).get(PlaceViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place, container, false);
        ButterKnife.bind(this, view);
        inflateSwipeRefreshLayout();
        inflateNearbyStoreRecyclerView();
        inflateMapFragment();
        return view;
    }

    private void inflateSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this::inflateNearbyStoreRecyclerView);
    }

    @SuppressLint("MissingPermission")
    private void onReadyMap(GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMapClickListener(this::onClickMapFragment);

        placeViewModel.myLocation().observe(this, location -> onLocationChanged(googleMap, location));
    }

    private void inflateMapFragment() {
        mapFragment = new SupportMapFragment();
        FragmentManager fragmentManager = getFragmentManager();
        Objects.requireNonNull(fragmentManager).beginTransaction()
                .replace(R.id.map_fragment, mapFragment)
                .commit();

        mapFragment.getMapAsync(this::onReadyMap);
    }

    private void onLocationChanged(GoogleMap googleMap, Location location) {
        googleMap.clear();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate locationUpdate = CameraUpdateFactory.newLatLngZoom(latLng, REQUEST_CODE_PLACE_PICKER);

        googleMap.moveCamera(locationUpdate);
        googleMap.animateCamera(locationUpdate);
    }


    private void onClickMapFragment(LatLng view) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            Intent pickerIntent = builder.build(Objects.requireNonNull(getActivity()));
            startActivityForResult(pickerIntent, REQUEST_CODE_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException ignored) {
        }
    }

    private void inflateNearbyStoreRecyclerView() {
        nearByStore.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        nearByStore.setLayoutManager(layoutManager);

        NearbyStoreAdapter adapter = new NearbyStoreAdapter(placeViewModel, this);
        nearByStore.setAdapter(adapter);
        placeViewModel.nearByStore().observe(this, list -> onLoadNearByStoreList(adapter, list));
    }

    private void onLoadNearByStoreList(NearbyStoreAdapter adapter, List<Store> storeList) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setStoreList(storeList);
        mapFragment.getMapAsync(googleMap -> {
            googleMap.clear();

            for (Store store : storeList) {
                LatLng selectedLatLng = new LatLng(store.latitude, store.longitude);

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(selectedLatLng)
                        .title(store.name);

                googleMap.addMarker(markerOptions).showInfoWindow();
            }
        });
    }
}
