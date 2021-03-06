package com.izho.saveentry

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.izho.saveentry.data.getAppDatabase
import com.izho.saveentry.settings.SettingsActivity
import com.izho.saveentry.ui.ActiveFragment
import com.izho.saveentry.ui.FavoritesFragment
import com.izho.saveentry.ui.HistoryFragment
import com.izho.saveentry.utils.*

private val TAB_LABELS = arrayOf("Active", "History", "Favorites")
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pageAdapter = PageAdapter(this)
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = pageAdapter

        val tabs: TabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = TAB_LABELS[position]
        }.attach()

        setSelectedTab()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            if(BuildConfig.DEBUG) {
                val intent = Intent(this, CheckInOrOutActivity::class.java)
                intent.putExtra("url", "https://temperaturepass.ndi-api.gov.sg/login/PROD-200002019C-224153-XPNUS-SE")
                intent.putExtra("action", "checkIn")
                startActivity(intent)
            } else {
                startActivity(Intent(this, LiveBarcodeScanningActivity::class.java))
            }
        }

        tabs.post {
            if(!Prefs.hasSeenTutorial(this)) {
                try {
                    TutorialManager(this).showTutorial(R.id.view_pager)
                } catch (e:Throwable) {
                    //likely due to the device memory is unable to load the raw image
                    Prefs.setHasSeenTutorial(this, true)
                }
            }
        }
        SafeEntryHelper.update()
    }

    override fun onResume() {
        super.onResume()
        Utils.requestCameraPermissionIfNotGranted(this)
        setSelectedTab()
    }

    fun setSelectedTab() {
        if(intent.hasExtra("scrollToFavourtie")) {
            viewPager.setCurrentItem((TAB_LABELS.indexOf("Favorites")), false)
            //so that subsequent opening of the app wont be stucked to favorite
            intent.removeExtra("scrollToFavourtie")
        } else {
            val database = getAppDatabase(this, resetDb = false)
            val activeVisits = database.dao.getAllActiveVisitWithLocation().observe(this, Observer { activeVisits ->
                if (activeVisits == null || activeVisits.isEmpty()) {
                    viewPager.setCurrentItem((TAB_LABELS.indexOf("Favorites")), false)
                }
            })
        }
    }

    class PageAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> ActiveFragment.newInstance()
                1 -> HistoryFragment.newInstance()
                2 -> FavoritesFragment.newInstance()
                else -> throw ClassNotFoundException("Unknown type of fragment")
            }
        }

    }

    fun startSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}