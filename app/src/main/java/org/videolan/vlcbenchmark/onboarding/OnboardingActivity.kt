package org.videolan.vlcbenchmark.onboarding

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_onboarding.*
import org.videolan.vlcbenchmark.MainPage
import org.videolan.vlcbenchmark.R

class OnboardingActivity : AppCompatActivity(), ViewPager.OnPageChangeListener {

    companion object {
        @SuppressWarnings("UNUSED")
        private val TAG = this::class.java.name

        private const val SHARED_PREFERENCE_ONBOARDING = "org.videolan.vlc.gui.video.benchmark.ONBOARDING"
        private const val PERMISSION_REQUEST = 1
    }
    private var mPermissions: ArrayList<String> = ArrayList()
    private lateinit var mPositionIndicators: Array<View>
    private lateinit var mPager: ViewPager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Checking if the onboarding has already been done
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val onboardingDone = sharedPref.getBoolean(SHARED_PREFERENCE_ONBOARDING, false)

        // The onboarding process has already been performed -> send user to the main page.
        // Though in case the user has reset his permissions, request permission is called.
        // If the permissions were not requested, requestPermission() will call startMainPage().
        if (onboardingDone) {
            requestPermissions()
        }

        setContentView(R.layout.activity_onboarding)

        mPositionIndicators = arrayOf(
                onboarding_indicator_0,
                onboarding_indicator_1,
                onboarding_indicator_2
        )

        mPager = onboardding_pager
        val layouts = arrayOf(
                R.layout.layout_onboarding_intro,
                R.layout.layout_onboarding_download,
                R.layout.layout_onboarding_warnings
        )
        mPager.adapter = OnboardingPagerAdapter(this, layouts)
        mPager.currentItem = 0
        updateIndicator(0)
        updateButtons(0)
        mPager.addOnPageChangeListener(this)

        // Check if Android TV / chromebook for pad / keyboard inputs -> manually handle focus
        if (packageManager.hasSystemFeature("android.software.leanback")
                or packageManager.hasSystemFeature("org.chromium.arc.device_management")) {
            setFocusListeners()
            onboarding_btn_next.requestFocus()
            // have to call onFocusChange manually here, requestFocus doesn't seem to call the callback
            onFocusChange(onboarding_btn_next, true)
        }
    }

    // In case of android tv, change the background color to clearly indicate focus
    private fun onFocusChange(view: View, hasFocus: Boolean) : Unit {
        if (hasFocus) {
            view.setBackgroundColor(resources.getColor(R.color.grey400transparent))
        } else {
            view.setBackgroundColor(resources.getColor(R.color.white))
        }
    }

    // Setting callbacks to change background on focus change
    private fun setFocusListeners() {
        onboarding_btn_next.setOnFocusChangeListener(::onFocusChange)
        onboarding_btn_previous.setOnFocusChangeListener(::onFocusChange)

        // in case of a tactile swipe, and then an keyboard / pad input, the viewpager
        // gets the focus. To stop that, focus is redirected to next button
        // or done button in the case of the last onboarding view
        mPager.setOnFocusChangeListener {_, _ ->
            if (mPager.currentItem == 2) {
                onboarding_btn_done.requestFocus()
            } else {
                onboarding_btn_next.requestFocus()
            }
        }
    }

    private fun updateIndicator(position: Int) {
        for (i in mPositionIndicators.indices) {
            var layoutParams: LinearLayout.LayoutParams
            if (i == position) {
                layoutParams = LinearLayout.LayoutParams(
                        resources.getDimension(R.dimen.selected_indicator).toInt(),
                        resources.getDimension(R.dimen.selected_indicator).toInt())
            } else {
                layoutParams = LinearLayout.LayoutParams(
                        resources.getDimension(R.dimen.unselected_indicator).toInt(),
                        resources.getDimension(R.dimen.unselected_indicator).toInt())
            }
            layoutParams.setMargins(
                    resources.getDimension(R.dimen.indicator_margin).toInt(),
                    resources.getDimension(R.dimen.indicator_margin).toInt(),
                    resources.getDimension(R.dimen.indicator_margin).toInt(),
                    resources.getDimension(R.dimen.indicator_margin).toInt())
            mPositionIndicators[i].layoutParams = layoutParams
        }
    }

    private fun updateButtons(position: Int) {
        val nextBtn = onboarding_btn_next
        val doneBtn = onboarding_btn_done
        val prevBtn = onboarding_btn_previous
        if (position == 2) {
            nextBtn.visibility = View.INVISIBLE
            doneBtn.visibility = View.VISIBLE
            doneBtn.requestFocus()
        } else {
            nextBtn.visibility = View.VISIBLE
            doneBtn.visibility = View.INVISIBLE
        }
        if (position == 0) {
            prevBtn.visibility = View.INVISIBLE
            nextBtn.requestFocus()
        } else {
            prevBtn.visibility = View.VISIBLE
        }
    }


    fun clickNextPage(view: View) {
        if (mPager.currentItem != mPager.childCount) {
            mPager.currentItem += 1
        }
    }

    fun clickPreviousPage(view: View) {
        if (mPager.currentItem != 0) {
            mPager.currentItem -= 1
        }
    }

    fun clickDone(view: View) {
        requestPermissions()
    }

    override fun onPageSelected(position: Int) {
        updateIndicator(position)
        updateButtons(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (mPermissions.size != 0) {
            val arrayPermissions: Array<String> = mPermissions.toTypedArray()
            ActivityCompat.requestPermissions(this, arrayPermissions, PERMISSION_REQUEST)
        } else {
            startMainPage()
        }
    }

    private fun startMainPage() {
        // Save the fact that the onboarding was done
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean(SHARED_PREFERENCE_ONBOARDING, true)
        editor.apply()

        // Start main page
        val intent = Intent(this, MainPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mPermissions.remove(permissions[i])
                }
            }
            if (mPermissions.isNotEmpty()) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_text_error_permission)
                        .setNeutralButton(R.string.dialog_btn_ok, {_, _ ->
                            finishAndRemoveTask()
                        })
                        .show()
            } else {
                startMainPage()
            }
        }
    }

    class OnboardingPagerAdapter(context: Context, layouts: Array<Int>) : PagerAdapter() {
        private val mLayouts: Array<Int> = layouts
        private val mContext : Context = context

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(mContext)
            val layout = inflater.inflate(mLayouts[position], container, false)
            container.addView(layout)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
                return view == `object`
        }

        override fun getCount(): Int {
            return mLayouts.size
        }
    }
}