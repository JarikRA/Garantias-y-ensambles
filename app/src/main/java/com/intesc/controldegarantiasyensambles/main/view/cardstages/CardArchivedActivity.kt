package com.intesc.controldegarantiasyensambles.main.view.cardstages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivityCardArchivedBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.HistoryDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.ServiceDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.UserDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.repository.ServiceRepository
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModel
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModelFactory
import timber.log.Timber

class CardArchivedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardArchivedBinding
    private lateinit var navController: NavController
    private var card: Card? = null

    private val sharedTabViewModel: SharedTabViewModel by viewModels {
        SharedTabViewModelFactory(
            UserRepository(UserDaoImpl()),
            HistoryRepository(HistoryDaoImpl()),
            ServiceRepository(ServiceDaoImpl())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardArchivedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        card = intent.getSerializableExtra("selected_card") as? Card
        if (card != null) {
            sharedTabViewModel.setCard(card!!)
        } else {
            Timber.e("No card data found in the Intent.")
            finish()
        }

        binding.fabCreateService.setOnClickListener { sendToCreateService() }
        setupListenersTabLayout()
        configureNavController()
    }

    private fun configureNavController() {
        try {
            val navHostFragment =
                supportFragmentManager.findFragmentById(binding.fcvArchived.id) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.info_graph)
        } catch (e: Exception) {
            Timber.e(e, "Error configuring NavController")
        }
    }

    private fun setupListenersTabLayout() {
        binding.tlArchived.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val fragmentId = when (tab?.position) {
                    0 -> R.id.generalInfoFragment
                    1 -> R.id.historiesFragment
                    2 -> R.id.servicesFragment
                    else -> R.id.generalInfoFragment
                }
                if (navController.currentDestination?.id != fragmentId) {
                    navController.navigate(fragmentId)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private val createServiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private fun sendToCreateService() {
        val intent = Intent(this, ServiceCreateActivity::class.java)
        intent.putExtra("card", card)
        intent.putExtra("user_id", card?.userId)
        createServiceLauncher.launch(intent)
    }

    private fun cancelJobsAndCloseConnection() {
        DataBaseConnection.closeConnection()
        sharedTabViewModel.cancelJobs()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelJobsAndCloseConnection()
    }
}
