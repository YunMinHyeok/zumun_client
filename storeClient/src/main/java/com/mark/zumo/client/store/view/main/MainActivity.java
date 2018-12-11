/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.store.view.main;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.mark.zumo.client.core.entity.Store;
import com.mark.zumo.client.core.util.glide.GlideApp;
import com.mark.zumo.client.core.util.glide.transformation.LinearGradientTransformation;
import com.mark.zumo.client.core.view.BaseActivity;
import com.mark.zumo.client.store.R;
import com.mark.zumo.client.store.view.order.OrderFragment;
import com.mark.zumo.client.store.view.setting.fragment.SettingMainFragment;
import com.mark.zumo.client.store.viewmodel.MainViewModel;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mark on 18. 7. 1.
 */
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.nav_view) NavigationView navView;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;

    private MainViewModel mainViewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.content_desc_navigation_drawer_open, R.string.content_desc_navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(this);
        mainViewModel.findCustomer(this);
    }

    @Override
    public View onCreateView(final String name, final Context context, final AttributeSet attrs) {
        inflateStoreInformation();
        return super.onCreateView(name, context, attrs);
    }

    private void inflateStoreInformation() {
        mainViewModel.loadSessionStore().observe(this, this::onLoadStore);
    }

    private void onLoadStore(Store store) {
        setTitle(store.name);

        AppCompatTextView name = navView.findViewById(R.id.name);
        AppCompatTextView address = navView.findViewById(R.id.address);
        AppCompatImageView coverImage = navView.findViewById(R.id.cover_image);
        AppCompatImageView thumbnailImage = navView.findViewById(R.id.thumbnail_image);

        name.setText(store.name);
        address.setText(store.address);
        GlideApp.with(this)
                .load(store.thumbnailUrl)
                .apply(RequestOptions.circleCropTransform())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(thumbnailImage);

        GlideApp.with(this)
                .load(store.coverImageUrl)
                .apply(RequestOptions.centerCropTransform())
                .transform(new LinearGradientTransformation(this))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(coverImage);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu_order) {
            Fragment fragment = Fragment.instantiate(this, OrderFragment.class.getName());
            updateMainFragment(fragment);
        } else if (id == R.id.nav_setting) {
            Fragment fragment = Fragment.instantiate(this, SettingMainFragment.class.getName());
            updateMainFragment(fragment);
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateMainFragment(final Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_fragment, fragment)
                .commit();
    }
}
