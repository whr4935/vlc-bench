/*****************************************************************************
 * AboutActivity.java
 *****************************************************************************
 * Copyright © 2017 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlcbenchmark;

import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.Util;

public class AboutActivity extends AppCompatActivity {

    private final static String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        String tab_about = getResources().getString(R.string.tab_about);
        String tab_licence = getResources().getString(R.string.tab_licence);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle(tab_about);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            onBackPressed();
            return;
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText(tab_about));
        tabLayout.addTab(tabLayout.newTab().setText(tab_licence));

        View aboutLayout = findViewById(R.id.layout_about);
        WebView licenceWebView = (WebView) findViewById(R.id.licence_webview);

        TextView link = (TextView) aboutLayout.findViewById(R.id.about_link);
        link.setText(R.string.about_link);
        TextView revisionText = (TextView) aboutLayout.findViewById(R.id.revision);
        String revisionStr = getString(R.string.about_revision) + " " + getString(R.string.build_revision) +
                " ( " + getString(R.string.build_time) + " ) " + BuildConfig.BUILD_TYPE;
        revisionText.setText(revisionStr);
        TextView compiledText = (TextView) aboutLayout.findViewById(R.id.about_compiled);
        compiledText.setText(R.string.build_host);
        TextView minVlc = (TextView) aboutLayout.findViewById(R.id.vlc_min_version);
        minVlc.setText(String.format(getString(R.string.about_vlc_min),  BuildConfig.VLC_VERSION));

        licenceWebView.loadData(Util.readAsset("licence.htm",
                getResources().getAssets()).replace("!COMMITID!",
                getString(R.string.build_revision)), "text/html", "UTF8");

        View[] views = new View[]{aboutLayout, licenceWebView};
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
