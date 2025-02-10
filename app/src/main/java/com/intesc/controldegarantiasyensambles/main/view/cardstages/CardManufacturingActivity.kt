package com.intesc.controldegarantiasyensambles.main.view.cardstages

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivityCardManufacturingBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardModelDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.HistoryDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.repository.CardModelRepository
import com.intesc.controldegarantiasyensambles.main.repository.CardRepository
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.view.cardstages.adapters.HistoryAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@SuppressLint("SetTextI18n")
class CardManufacturingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardManufacturingBinding

    private lateinit var cardModelRepository: CardModelRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var historyRepository: HistoryRepository

    private lateinit var historyAdapter: HistoryAdapter

    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    private lateinit var card: Card
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardManufacturingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRepositories()
        initializeRecyclerView()
        setupListeners()
        loadSelectedCard()
    }

    //Method to initialize repositories
    private fun setupRepositories() {
        val cardModelDao = CardModelDaoImpl()
        val cardDao = CardDaoImpl()
        val historyDao = HistoryDaoImpl()

        cardModelRepository = CardModelRepository(cardModelDao)
        cardRepository = CardRepository(cardDao)
        historyRepository = HistoryRepository(historyDao)
    }

    //Method to configure button listeners
    private fun setupListeners() {
        binding.btnDisplayFieldsAddRecord.setOnClickListener { toggleAddRecordFields(true) }
        binding.btnCancel.setOnClickListener { toggleAddRecordFields(false) }
        binding.btnUpdate.setOnClickListener { checkCriteriaAddRecord() }
    }

    //Method to retrieve and display the selected card
    private fun loadSelectedCard() {
        card = ((intent.getSerializableExtra("selected_card") as? Card) ?: finish()) as Card
        displayCardData(card)
    }

    //Method for displaying card data
    private fun displayCardData(card: Card) {
        binding.apply {
            tvSerialNumber.text = "No. de serie: ${card.serialNumber}"
            tvCategory.text = "Categoria: ${card.category}"
            tvCreationDate.text = "Fecha de creación: ${card.creationDate}"
            tvUpdateDate.text = "Fecha de actualización: ${card.updateDate}"
            tvCardModel.text = "Modelo de tarjeta: Cargando..."
        }

        activityScope.launch {
            val cardModel = card.idModel?.let { id ->
                withContext(Dispatchers.IO) {
                    cardModelRepository.getCarModelById(id)
                }
            }

            val modelName = cardModel?.modelName ?: "Modelo desconocido"
            binding.tvCardModel.text = "Modelo de tarjeta: $modelName"

            fetchHistories(card.id)
        }
    }

    //Method for staring recyclerView
    private fun initializeRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList())
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@CardManufacturingActivity)
            adapter = historyAdapter
        }
    }

    //Method for recovering records
    private fun fetchHistories(cardId: Int?) {
        binding.piHistories.setVisible(true)

        if (cardId == null) return
        activityScope.launch {
            val histories = withContext(Dispatchers.IO) {
                historyRepository.getHistoriesByCardId(cardId)
            }
            if (histories.isEmpty()) {
                binding.tvHistory.text = "Sin historial"
            } else {
                historyAdapter.setHistories(histories)
            }
            binding.piHistories.setVisible(false)
        }
    }

    //Method to show/hide fields
    private fun toggleAddRecordFields(show: Boolean) {
        binding.apply {
            btnDisplayFieldsAddRecord.setVisible(!show)
            tvHistory.setVisible(!show)
            rvHistory.setVisible(!show)

            tvUpdate.setVisible(show)
            tvNewStatus.setVisible(show)
            tilCategory.setVisible(show)
            tilResponsibleName.setVisible(show)
            tilComments.setVisible(show)
            btnUpdate.setVisible(show)
            btnCancel.setVisible(show)
        }
    }

    //Validate fields and display error messages
    private fun validateField(field: String, errorMessage: String): Boolean {
        return if (field.isEmpty()) {
            showToast(errorMessage)
            false
        } else {
            true
        }
    }

    //Method for check criteria before update and insert
    private fun checkCriteriaAddRecord() {
        val category = binding.atvCategory.text.toString().trim()
        val responsibleName = binding.tilResponsibleName.editText?.text.toString().trim()
        val comments = binding.tieComments.text.toString().trim()

        if (validateField(category, "Seleccione una categoria") &&
            validateField(responsibleName, "Ingrese el nombre") &&
            validateField(comments, "Ingrese los comentarios")
        ) {
            updateCardAndInsertHistory(category, responsibleName, comments)
        }
    }

    //Method for update card and insert history
    private fun updateCardAndInsertHistory(
        category: String,
        responsibleName: String,
        comments: String
    ) {
        val currentDate = Date()
        val sqlDate = java.sql.Date(currentDate.time)

        showLoadingDialog()
        binding.btnUpdate.isEnabled = false

        activityScope.launch {
            try {
                val updateResult = withContext(Dispatchers.IO) {
                    val updatedCard = card.copy(
                        category = category,
                        updateDate = sqlDate
                    )
                    val cardUpdated = cardRepository.updateCard(updatedCard)
                    if (!cardUpdated) throw Exception("Error updating card")

                    val newHistory = History(
                        id = 0,
                        idCard = card.id,
                        date = sqlDate,
                        status = "empty",
                        assemblerName = responsibleName,
                        record = comments,
                        category = category
                    )
                    val historyInserted = historyRepository.insertHistory(newHistory)
                    if (!historyInserted) throw Exception("Error inserting history")

                    true
                }

                loadingDialog?.dismiss()
                if (updateResult) {
                    showToast("Cambios guardados")
                    if (category == "Archivado") finish()
                    displayCardData(card)
                }
            } catch (e: Exception) {
                loadingDialog?.dismiss()
                e.printStackTrace()
                showToast("Error al guardar cambios")
            }

            toggleAddRecordFields(false)
            binding.btnUpdate.isEnabled = true
        }
    }

    //Method to display the loading dialog when saving changes
    private fun showLoadingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val tvInfo: TextView = dialogView.findViewById(R.id.tvInfo)

        tvInfo.text = "Guardando cambios"

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    //Method to simplify the creation of snack bars
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Method for close all in case destroy activity
    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancelChildren()
        DataBaseConnection.closeConnection()
    }

    //Method to simplify show/hide
    private fun View.setVisible(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
