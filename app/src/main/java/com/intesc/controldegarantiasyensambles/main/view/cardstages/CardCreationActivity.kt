package com.intesc.controldegarantiasyensambles.main.view.cardstages

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivityNewCardBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardModelDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.HistoryDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.CardModel
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.repository.CardModelRepository
import com.intesc.controldegarantiasyensambles.main.repository.CardRepository
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.view.search.SearchActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Date
import java.time.LocalDate

class CardCreationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewCardBinding
    private lateinit var currentDate: LocalDate

    private lateinit var cardModelRepository: CardModelRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var historyRepository: HistoryRepository

    private lateinit var cardModels: List<CardModel>
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRepositories()
        setupButtonsListeners()
        setDate()
        retrieveDefineCardModels()
    }

    companion object {
        private const val CATEGORY_MANUFACTURING = "Fabricación"
    }

    private fun setupButtonsListeners() {
        binding.btnCancel.setOnClickListener { cancel() }
        binding.btnCreateCard.setOnClickListener { checkCriteria() }
    }

    private fun setupRepositories() {
        val cardModelDao = CardModelDaoImpl()
        val cardDao = CardDaoImpl()
        val historyDao = HistoryDaoImpl()

        cardModelRepository = CardModelRepository(cardModelDao)
        cardRepository = CardRepository(cardDao)
        historyRepository = HistoryRepository(historyDao)
    }

    @SuppressLint("SetTextI18n")
    private fun setDate() {
        currentDate = LocalDate.now()
        binding.tvDate.text = "Fecha: $currentDate"
    }

    private fun retrieveDefineCardModels() {
        lifecycleScope.launch {
            cardModels = withContext(Dispatchers.IO) {
                cardModelRepository.getAllCardModels()
            }

            val modelNames = cardModels.map { it.modelName }

            val adapter = ArrayAdapter(
                this@CardCreationActivity,
                android.R.layout.simple_dropdown_item_1line,
                modelNames
            )
            binding.atvModels.setAdapter(adapter)

            binding.atvModels.isClickable = true
            binding.atvModels.isFocusable = true
            binding.tilModel.hint = "Selecciona el modelo"
        }
    }

    private fun checkCriteria() {
        val modelName = binding.atvModels.text.toString().trim()
        if (modelName.isEmpty()) {
            showToast("Seleccione un modelo")
            return
        }

        val amountString = binding.tieAmount.text.toString().trim()
        if (amountString.isEmpty()) {
            showToast("Ingrese la cantidad")
            return
        }

        val amount = amountString.toIntOrNull()
        if (amount == null || amount <= 0 || amount>300 ) {
            showToast("Ingresa una cantidad de 1 a 300")
            return
        }

        val assemblerName = binding.tieAssemblerName.text.toString().trim()
        if (assemblerName.isEmpty()) {
            showToast("Ingrese el nombre del ensamblador")
            return
        }

        val comments = binding.tieComments.text.toString().trim()
        if (comments.isEmpty()) {
            showToast("Ingrese los comentarios")
            return
        }

        val sqlDate = Date.valueOf(currentDate.toString())
        val modelId = cardModels.find { it.modelName == modelName }?.id
        val abbreviateCardModelName =
            cardModels.find { it.modelName == modelName }!!.abbreviatedModel
        val month = currentDate.monthValue
        val year = currentDate.year

        //Staring save cards process
        showLoadingDialog(amount)

        if (amount == 1) {
            lifecycleScope.launch {
                val serialNumber = generateSingleSerialNumber(abbreviateCardModelName, month, year)
                val card = Card(
                    id = 0,
                    idModel = modelId,
                    serialNumber = serialNumber,
                    creationDate = sqlDate,
                    updateDate = sqlDate,
                    status = "empty", //This is because it was requested
                    subStatus = null,
                    category = CATEGORY_MANUFACTURING,
                    userId = null
                )
                saveSingleCard(card, assemblerName, comments)
            }
        } else {
            lifecycleScope.launch {
                val serialNumbers =
                    generateMultipleSerialNumbers(abbreviateCardModelName, month, year, amount)
                val cards = serialNumbers.map { serialNumber ->
                    Card(
                        id = 0,
                        idModel = modelId,
                        serialNumber = serialNumber,
                        creationDate = sqlDate,
                        updateDate = sqlDate,
                        status = "empty", //This is because it was requested
                        subStatus = null,
                        category = CATEGORY_MANUFACTURING,
                        userId = null
                    )
                }
                saveMultipleCards(cards, assemblerName, comments)
            }
        }
    }

    private suspend fun generateSingleSerialNumber(
        abbreviateCardModelName: String,
        month: Int,
        year: Int
    ): String {
        return withContext(Dispatchers.IO) {
            val lastSerial = cardRepository.getLastSpecificSerialNumber(
                abbreviateCardModelName, month, year
            )

            val lastNumber = lastSerial?.takeLast(3)?.toIntOrNull() ?: 0
            val nextNumber = (lastNumber + 1).toString().padStart(3, '0')

            val monthAMN = month.toString().takeLast(2)
            val yearAMN = year.toString().takeLast(2)

            "$abbreviateCardModelName$monthAMN$yearAMN$nextNumber"
        }
    }

    private suspend fun generateMultipleSerialNumbers(
        abbreviateCardModelName: String,
        month: Int,
        year: Int,
        amount: Int
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val lastSerial = cardRepository.getLastSpecificSerialNumber(
                abbreviateCardModelName, month, year
            )
            val lastNumber = lastSerial?.takeLast(3)?.toIntOrNull() ?: 0

            val monthAMN = month.toString().takeLast(2)
            val yearAMN = year.toString().takeLast(2)

            (1..amount).map { index ->
                val nextNumber = (lastNumber + index).toString().padStart(3, '0')
                "$abbreviateCardModelName$monthAMN$yearAMN$nextNumber"
            }
        }
    }

    private fun saveSingleCard(
        card: Card,
        assemblerName: String,
        comments: String,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val generatedId = cardRepository.insertCard(card)
            if (generatedId > 0) {
                val history = History(
                    id = 0,
                    idCard = generatedId,
                    date = card.updateDate,
                    status = card.status,
                    assemblerName = assemblerName,
                    record = comments,
                    category = CATEGORY_MANUFACTURING
                )
                historyRepository.insertHistory(history)
            }
            withContext(Dispatchers.Main) {
                if (generatedId > 0) {
                    dismissLoadingDialog()
                    showToast("Tarjeta y historial creados con éxito")

                    backToSearchActivity()
                } else {
                    dismissLoadingDialog()
                    showToast("Error al crear la tarjeta y el historial")
                }
            }
        }
    }

    private fun saveMultipleCards(
        cards: List<Card>,
        assemblerName: String,
        comments: String,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val generatedIds = cardRepository.insertCards(cards)
            if (generatedIds.isNotEmpty() && generatedIds.size == cards.size) {
                val histories = cards.zip(generatedIds).map { (card, id) ->
                    History(
                        id = 0,
                        idCard = id,
                        date = card.updateDate,
                        status = card.status,
                        assemblerName = assemblerName,
                        record = comments,
                        category = CATEGORY_MANUFACTURING
                    )
                }
                historyRepository.insertHistories(histories)
            }
            withContext(Dispatchers.Main) {
                if (generatedIds.isNotEmpty() && generatedIds.size == cards.size) {

                    dismissLoadingDialog()
                    showToast("Tarjetas y sus historiales creados con éxito")

                    backToSearchActivity()
                } else {
                    dismissLoadingDialog()
                    showToast("Error al crear las tarjetas y los historiales")
                }
            }
        }
    }

    private fun showLoadingDialog(amount: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val tvInfo: TextView = dialogView.findViewById(R.id.tvInfo)

        tvInfo.text = if (amount == 1) "Guardando tarjeta" else "Guardando tarjetas"

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun backToSearchActivity() {
        startActivity(
            Intent(
                this@CardCreationActivity,
                SearchActivity::class.java
            )
        )
        finish()
    }

    private fun cancel() {
        startActivity(Intent(this, SearchActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        DataBaseConnection.closeConnection()
        backToSearchActivity()
    }
}
