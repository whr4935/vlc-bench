package org.videolan.vlcbenchmark;

import android.graphics.pdf.PdfDocument;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class AboutActivity extends AppCompatActivity {

    private final static String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        String tab_about = getResources().getString(R.string.tab_about);
        String tab_licence = getResources().getString(R.string.tab_licence);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(tab_about);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(0);
        } else {
            Log.e(TAG, "onCreate: getSupportActionBar is null");
            //TODO handle failure
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText(tab_about));
        tabLayout.addTab(tabLayout.newTab().setText(tab_licence));

        View aboutLayout = findViewById(R.id.layout_about);
        View licenceLayout = findViewById(R.id.layout_licence);

        View[] views = new View[]{aboutLayout, licenceLayout};
        String[] titles = new String[]{tab_about, tab_licence};

        ViewPager viewPager = (ViewPager)findViewById(R.id.pager);
        viewPager.setAdapter(new AboutPagerAdapter(views, titles));
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class AboutPagerAdapter extends PagerAdapter {

        private View[] mViews;
        private String[] mTitles;

        AboutPagerAdapter(View[] views, String[] titles) {
            this.mViews = views;
            this.mTitles = titles;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) { return mViews[position]; }

        @Override
        public CharSequence getPageTitle(int position) { return mTitles[position]; }

        @Override
        public boolean isViewFromObject(View view, Object object) { return view == object; }

        @Override
        public int getCount() { return mViews == null ? 0 : mViews.length; }
    }
}
