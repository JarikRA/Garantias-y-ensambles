package com.intesc.controldegarantiasyensambles.main.view.cardstages

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivityCardServiceBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.HistoryDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.ServiceDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.UserDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.repository.CardRepository
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.repository.ServiceRepository
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModel
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate

class CardServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardServiceBinding
    private lateinit var navController: NavController
    private var card: Card? = null

    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    private lateinit var cardRepository: CardRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var serviceRepository: ServiceRepository

    private val sharedTabViewModel: SharedTabViewModel by viewModels {
        SharedTabViewModelFactory(
            UserRepository(UserDaoImpl()),
            HistoryRepository(HistoryDaoImpl()),
            ServiceRepository(ServiceDaoImpl())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        card = intent.getSerializableExtra("selected_card") as? Card
        if (card == null) {
            Timber.e("No card data found in the Intent.")
            finish()
            return
        }

        sharedTabViewModel.setCard(card!!)
        setupUI()
    }

    private fun setupUI() {
        setupFabListener()
        setupRepositories()
        configureNavController()
        setupListenersTabLayout()
    }

    private fun setupFabListener() {
        binding.fabArchive.setOnClickListener { showArchiveDialog() }
    }

    private fun setupRepositories() {
        cardRepository = CardRepository(CardDaoImpl())
        historyRepository = HistoryRepository(HistoryDaoImpl())
        serviceRepository = ServiceRepository(ServiceDaoImpl())
    }

    private fun configureNavController() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(binding.fcvService.id) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.info_graph)
        } catch (e: Exception) {
            Timber.e(e, "Error configuring NavController")
        }
    }

    private fun setupListenersTabLayout() {
        binding.tlService.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val fragmentId = when (it.position) {
                        0 -> R.id.generalInfoFragment
                        1 -> R.id.historiesFragment
                        2 -> R.id.servicesFragment
                        else -> R.id.generalInfoFragment
                    }

                    if (navController.currentDestination?.id != fragmentId) {
                        navController.navigate(fragmentId)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showArchiveDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_archive, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.apply {
            configureDialogActions(dialogView, dialog)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
    }

    private fun configureDialogActions(dialogView: View, dialog: AlertDialog) {
        val btnArchive = dialogView.findViewById<MaterialButton>(R.id.btnArchive)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val tieResponsibleName = dialogView.findViewById<TextInputEditText>(R.id.tieResponsibleName)
        val tieRepair = dialogView.findViewById<TextInputEditText>(R.id.tieRepair)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piArchiving)

        btnArchive.setOnClickListener {
            val responsibleName = tieResponsibleName.text?.toString()?.trim()
            val repairDetails = tieRepair.text?.toString()?.trim()

            if (responsibleName.isNullOrEmpty()) {
                showToast("Ingresa el nombre del responsable")
            } else if (repairDetails.isNullOrEmpty()) {
                showToast("Ingresa la reparación")
            } else {
                archiveCard(
                    responsibleName,
                    repairDetails,
                    tieResponsibleName,
                    tieRepair,
                    progressIndicator,
                    btnArchive,
                    btnCancel,
                    dialog
                )
            }
        }

        btnCancel.setOnClickListener {
            cancelMainJobs()
            dialog.dismiss()
        }
    }

    private fun archiveCard(
        responsibleName: String,
        repairDetails: String,
        tieResponsibleName: TextInputEditText,
        tieRepair: TextInputEditText,
        progressIndicator: LinearProgressIndicator,
        btnArchive: MaterialButton,
        btnCancel: MaterialButton,
        dialog: AlertDialog
    ) {
        progressIndicator.visibility = View.VISIBLE
        tieResponsibleName.isEnabled = false
        tieRepair.isEnabled = false
        btnArchive.isEnabled = false
        btnCancel.isEnabled = false

        saveAllData(responsibleName, repairDetails) { success ->
            progressIndicator.visibility = View.GONE
            tieResponsibleName.isEnabled = true
            tieRepair.isEnabled = true
            btnArchive.isEnabled = true
            btnCancel.isEnabled = true

            if (success) {
                showToast("Tarjeta archivada con éxito")
                dialog.dismiss()
                finish()
            } else {
                showToast("Error al guardar los datos. Por favor, intenta nuevamente.")
            }
        }
    }

    private fun saveAllData(
        responsibleName: String,
        repairDetails: String,
        onComplete: (Boolean) -> Unit
    ) {
        val archivingDate = java.sql.Date.valueOf(LocalDate.now().toString())
        val updatedCard = card?.copy(
            updateDate = archivingDate,
            category = "Archivado",
        ) ?: return showToast("Error: No se encontró la tarjeta")

        val history = History(
            id = 0,
            idCard = updatedCard.id,
            date = archivingDate,
            status = updatedCard.status,
            assemblerName = responsibleName,
            record = "Tarjeta transferida a archivo",
            category = updatedCard.category
        )

        activityScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    cardRepository.updateCard(updatedCard) &&
                            historyRepository.insertHistory(history) &&
                            serviceRepository.insertRepairInLastServiceOfCard(
                                updatedCard.id,
                                repairDetails
                            )
                } catch (e: Exception) {
                    Timber.e(e, "Error saving data")
                    false
                }
            }
            onComplete(success)
        }
    }

    private fun cancelMainJobs() {
        try {
            activityJob.cancelChildren()
        } finally {
            DataBaseConnection.closeConnection()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelMainJobs()
    }

    private fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
