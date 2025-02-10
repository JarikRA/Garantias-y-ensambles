package com.intesc.controldegarantiasyensambles.main.view.cardstages

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivityServiceCreateBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.HistoryDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.ServiceDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.UserDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.model.Service
import com.intesc.controldegarantiasyensambles.main.model.User
import com.intesc.controldegarantiasyensambles.main.repository.CardRepository
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.repository.ServiceRepository
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository
import com.intesc.controldegarantiasyensambles.main.view.customer.SelectCustomerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate

@SuppressLint("SetTextI18n")
class ServiceCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceCreateBinding

    private lateinit var card: Card
    private var userId: Int? = null

    private lateinit var currentDate: LocalDate
    private var loadingDialog: AlertDialog? = null

    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    private lateinit var userRepository: UserRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var serviceRepository: ServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Hide text views while customer is loading
        toggleCustomerSection(show = false)

        loadIntentData()
        setupRepositories()
        setupButtonListeners()
        displayCurrentDate()
    }

    private fun loadIntentData() {
        card = (intent.getSerializableExtra("card") as? Card)!!
        userId = intent.getIntExtra("user_id", -1).takeIf { it != -1 }

        if (userId != null) {
            Timber.i("userId = $userId")
            loadCustomerData(userId!!)
        } else {
            binding.tvCustomerInfo.text = "No hay cliente asignado"
            toggleCustomerButtons(showSetCustomer = true)
        }
    }

    private fun setupRepositories() {
        userRepository = UserRepository(UserDaoImpl())
        cardRepository = CardRepository(CardDaoImpl())
        historyRepository = HistoryRepository(HistoryDaoImpl())
        serviceRepository = ServiceRepository(ServiceDaoImpl())
    }

    private fun setupButtonListeners() {
        binding.apply {
            btnSetCustomer.setOnClickListener { navigateToCustomerSelection() }
            btnChangeCustomer.setOnClickListener { navigateToCustomerSelection() }
            btnCreateService.setOnClickListener { handleCreateService() }
            btnCancel.setOnClickListener { finish() }
        }
    }

    private fun displayCurrentDate() {
        currentDate = LocalDate.now()
        binding.tvDate.text = "Fecha: $currentDate"
    }

    private val selectCustomerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedCustomer =
                result.data?.getSerializableExtra("selectedCustomer") as? User
            if (selectedCustomer != null) {
                userId = selectedCustomer.id
                displayCustomerInfo(selectedCustomer)

                toggleCustomerSection(show = true)
                toggleCustomerButtons(showSetCustomer = false)
            } else {
                showToast("Cliente inválido")
            }
        }
    }

    private fun navigateToCustomerSelection() {
        cancelJobsAndCloseConnection()
        val intent = Intent(this, SelectCustomerActivity::class.java)
        selectCustomerLauncher.launch(intent)
    }

    private fun loadCustomerData(userId: Int) {
        binding.piLoadingCustomer.visibility = View.VISIBLE
        toggleCustomerSection(false)
        activityScope.launch {
            val user = withContext(Dispatchers.IO) {
                userRepository.getUserById(userId)
            }

            binding.piLoadingCustomer.visibility = View.GONE

            if (user == null) {
                binding.tvCustomerInfo.text = "Esta tarjeta no tiene cliente asignado"
                toggleCustomerSection(show = false)
                toggleCustomerButtons(showSetCustomer = true)
            } else {
                displayCustomerInfo(user)
                toggleCustomerSection(show = true)
            }
        }
    }

    private fun displayCustomerInfo(user: User) {
        binding.apply {
            tvCustomerInfo.text = "Información del cliente"
            tvCustomerName.text = "Nombre: ${user.name}"
            tvPhoneNumber.text = "Teléfono: ${user.phoneNumber}"
            tvEmail.text = "Correo: ${user.email}"
            tvZipCode.text = "Código postal: ${user.zipCode}"
            tvAddress.text = "Dirección: ${user.address}"
        }
    }

    private fun toggleCustomerSection(show: Boolean) {
        binding.apply {
            tvCustomerName.setVisible(show)
            tvPhoneNumber.setVisible(show)
            tvEmail.setVisible(show)
            tvZipCode.setVisible(show)
            tvAddress.setVisible(show)
        }
    }

    private fun toggleCustomerButtons(showSetCustomer: Boolean) {
        binding.btnSetCustomer.setVisible(showSetCustomer)
        binding.btnChangeCustomer.setVisible(!showSetCustomer)
    }

    private fun handleCreateService() {
        val responsibleName = binding.tieResponsibleName.text.toString().trim()
        val faultDescription = binding.tieFaultDescription.text.toString().trim()

        if (responsibleName.isEmpty()) {
            showToast("Ingresa el responsable del registro")
            return
        }

        if (faultDescription.isEmpty()) {
            showToast("Ingrese la descripción de la falla")
            return
        }

        if (userId == null) {
            showToast("Debes asignar un cliente antes de crear un servicio")
            return
        }

        val serviceDate = java.sql.Date.valueOf(currentDate.toString())

        val updatedCard = card.copy(
            updateDate = serviceDate,
            category = "Servicio",
            userId = userId!!
        )

        val history = History(
            id = 0,
            idCard = updatedCard.id,
            date = serviceDate,
            status = updatedCard.status,
            assemblerName = responsibleName,
            record = "Tarjeta transferida a servicio",
            category = updatedCard.category
        )

        val service = Service(
            id = 0,
            idCard = updatedCard.id,
            date = serviceDate,
            faultDescription = faultDescription,
            cardFailure = "empty",
            reasonForFailure = null,
            repair = null
        )

        saveData(updatedCard, history, service)
    }

    private fun saveData(updatedCard: Card, history: History, service: Service) {
        showLoadingDialog()

        activityScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    cardRepository.updateCard(updatedCard)
                    historyRepository.insertHistory(history)
                    serviceRepository.insertService(service)
                    true
                } catch (e: Exception) {
                    Timber.e(e, "Error saving data")
                    false
                }
            }

            dismissLoadingDialog()

            if (success) {
                showToast("Servicio creado con éxito")
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                showToast("Error al guardar los datos. Por favor, intenta nuevamente.")
            }
        }
    }


    @SuppressLint("InflateParams")
    private fun showLoadingDialog() {
        loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading, null).apply {
                findViewById<TextView>(R.id.tvInfo).text = "Creando servicio"
            })
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun View.setVisible(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun cancelJobsAndCloseConnection() {
        activityJob.cancelChildren()
        DataBaseConnection.closeConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelJobsAndCloseConnection()
    }
}
