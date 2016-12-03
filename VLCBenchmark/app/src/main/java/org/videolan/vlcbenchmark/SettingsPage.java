package org.videolan.vlcbenchmark;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;

public class SettingsPage extends AppCompatActivity {

    private ListView listView = null;
    private Toolbar toolbar = null;
    private BottomNavigationView bottomNavigationView = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("VLCBench", "create settings page");
        setupUi();
    }

    private void setupUi() {
        setContentView(R.layout.activity_settings_page);
        listView = (ListView) findViewById(R.id.settings_listview);
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getResources().getString(R.string.title_settings));
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.home_nav:
                                Intent homeIntent = new Intent(SettingsPage.this, MainPage.class);
                                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(homeIntent);
                                break;
                            case R.id.results_nav:
                                break;
                            case R.id.settings_nav:
                                break;
                        }
                        return false;
                    }
                }
        );
    }
}
