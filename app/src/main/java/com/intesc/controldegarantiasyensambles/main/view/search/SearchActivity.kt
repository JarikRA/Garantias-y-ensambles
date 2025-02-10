package com.intesc.controldegarantiasyensambles.main.view.search

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivitySearchBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardModelDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.HistoryDaoImpl
import com.intesc.controldegarantiasyensambles.main.dao.implements.UserDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.CardModel
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.repository.CardModelRepository
import com.intesc.controldegarantiasyensambles.main.repository.CardRepository
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository
import com.intesc.controldegarantiasyensambles.main.view.cardmodels.AdminCardModelsActivity
import com.intesc.controldegarantiasyensambles.main.view.cardstages.CardArchivedActivity
import com.intesc.controldegarantiasyensambles.main.view.cardstages.CardCreationActivity
import com.intesc.controldegarantiasyensambles.main.view.cardstages.CardManufacturingActivity
import com.intesc.controldegarantiasyensambles.main.view.cardstages.CardServiceActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("SetTextI18n")
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    private lateinit var userRepository: UserRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var cardModelRepository: CardModelRepository
    private lateinit var historyRepository: HistoryRepository

    private lateinit var cardAdapter: CardAdapter

    private lateinit var cardModels: List<CardModel>

    private lateinit var cameraTextLauncher: ActivityResultLauncher<Intent>

    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.IO + activityJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEndIcon()
        setupRepositories()
        setupListeners()

        retrieveDefineCardModels()
        setYears()

        initRecyclerView()
        initializeCameraTextLauncher()
    }

    object CardStatus {
        const val ARCHIVED = "Archivado"
        const val EMPTY = "empty"
    }

    private fun setupEndIcon() {
        binding.tilSearchCards.endIconMode = TextInputLayout.END_ICON_CUSTOM
        binding.tilSearchCards.isEndIconVisible = false
    }

    private fun setupRepositories() {
        val cardModelDao = CardModelDaoImpl()
        val cardDao = CardDaoImpl()
        val userDao = UserDaoImpl()
        val historyDao = HistoryDaoImpl()

        cardModelRepository = CardModelRepository(cardModelDao)
        cardRepository = CardRepository(cardDao)
        userRepository = UserRepository(userDao)
        historyRepository = HistoryRepository(historyDao)
    }

    private fun setupListeners() {
        binding.apply {
            btnSearch.setOnClickListener { checkCriteria() }
            btnArchiveCards.setOnClickListener { showArchiveDialog() }
            fbAdd.setOnClickListener { sendToNewCard() }
            btnAdminSettings.setOnClickListener { sendToAdminSettings() }
            tilSearchCards.setEndIconOnClickListener { sendToCamera() }
        }
    }

    private fun retrieveDefineCardModels() {
        lifecycleScope.launch {
            cardModels = withContext(Dispatchers.IO) {
                cardModelRepository.getAllCardModels()
            }

            val modelNames = mutableListOf("Todos")
            modelNames.addAll(cardModels.map { it.modelName })

            val adapter = ArrayAdapter(
                this@SearchActivity,
                android.R.layout.simple_dropdown_item_1line,
                modelNames
            )
            binding.atvModels.setAdapter(adapter)

            binding.atvModels.isClickable = true
            binding.atvModels.isFocusable = true
            binding.tilModel.hint = "Modelo"
        }
    }

    //Method for staring camera text launcher
    private fun initializeCameraTextLauncher() {
        cameraTextLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val recognizedText = result.data?.getStringExtra("recognizedText") ?: ""
                binding.tieSearchCards.setText(recognizedText)
                filterCards(recognizedText, cardAdapter.cardList)
            }
        }
    }

    //Method for send to camera activity
    private fun sendToCamera() {
        val intent = Intent(this, TextRecognitionCameraActivity::class.java)
        cameraTextLauncher.launch(intent)
    }

    //Method for set text watcher and update card list
    private fun setTextWatcher(searchCardsResult: List<Card>) {
        binding.tieSearchCards.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCards(s.toString(), searchCardsResult)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    //Method for filters cards from searched cards
    private fun filterCards(query: String, searchCardsResult: List<Card>) {
        val filteredCards = searchCardsResult.filter {
            it.serialNumber.contains(query, ignoreCase = true)
        }
        cardAdapter.setCards(filteredCards)

        if (filteredCards.isEmpty()) {
            binding.imPet.visibility = View.VISIBLE
            binding.tvSearchInformation.visibility = View.VISIBLE
            binding.tvSearchInformation.text = "No se encontraron\n resultados"
        } else {
            binding.imPet.visibility = View.GONE
            binding.tvSearchInformation.visibility = View.GONE
        }
    }

    //Method for staring recyclerview (list of cards)
    private fun initRecyclerView() {
        cardAdapter = CardAdapter(emptyList()) { card ->
            onItemSelected(card)
        }

        binding.rvCards.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = cardAdapter
        }
    }

    //Action when selecting an item from the list
    private fun onItemSelected(card: Card) {
        if (card.category.isEmpty()) {
            Timber.e("Card category is null or empty for card: $card")
            return
        }

        val intent = when (card.category) {
            "Fabricación" -> Intent(this, CardManufacturingActivity::class.java)
            "Archivado" -> Intent(this, CardArchivedActivity::class.java)
            "Servicio" -> Intent(this, CardServiceActivity::class.java)
            else -> {
                Timber.e("Unknown Category: ${card.category}")
                return
            }
        }

        intent.putExtra("selected_card", card)
        startActivity(intent)
    }

    //Method for setting years
    private fun setYears() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf("Todos")
        years.addAll((2009..currentYear).map { it.toString() })
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years)

        binding.atvYears.setAdapter(adapter)
    }

    //Method to send to add a new card in manufacturing
    private fun sendToNewCard() {
        DataBaseConnection.closeConnection()
        val intent = Intent(this, CardCreationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showArchiveDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_archive_manufacturing_cards, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val tieResponsibleName = dialogView.findViewById<TextInputEditText>(R.id.tieResponsibleName)
        val btnArchive = dialogView.findViewById<MaterialButton>(R.id.btnArchive)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piArchiving)

        btnArchive.setOnClickListener {
            val responsibleName = tieResponsibleName.text.toString().trim()

            if (responsibleName.isEmpty()) {
                tieResponsibleName.error = "El nombre es obligatorio"
                return@setOnClickListener
            }

            tieResponsibleName.isEnabled = false
            btnArchive.isEnabled = false
            progressIndicator.visibility = View.VISIBLE

            activityScope.launch {
                val success = processArchivingCards(responsibleName)
                withContext(Dispatchers.Main) {
                    if (success) {
                        dialog.dismiss()

                        binding.apply {
                            imPet.visibility = View.VISIBLE
                            tvSearchInformation.visibility = View.VISIBLE
                            tvSearchInformation.text = "Tarjetas archivadas"
                        }

                    } else {
                        tieResponsibleName.isEnabled = true
                        btnArchive.isEnabled = true
                        progressIndicator.visibility = View.GONE
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private suspend fun processArchivingCards(responsibleName: String): Boolean {
        val currentDate = getCurrentDate()
        val cardList = cardAdapter.cardList

        if (cardList.isEmpty()) {
            withContext(Dispatchers.Main) {
                showToast("No hay tarjetas para archivar")
            }
            return false
        }

        val updatedCards = cardList.map { card ->
            card.copy(
                updateDate = currentDate,
                category = CardStatus.ARCHIVED
            )
        }

        val histories = updatedCards.map { card ->
            History(
                id = 0,
                idCard = card.id,
                date = currentDate,
                status = CardStatus.EMPTY,
                assemblerName = responsibleName,
                record = "Tarjetas entregadas",
                category = CardStatus.ARCHIVED
            )
        }

        return try {
            updateCardsAndInsertHistories(updatedCards, histories)
            withContext(Dispatchers.Main) {
                showToast("Tarjetas archivadas exitosamente")
                binding.btnArchiveCards.visibility = View.GONE
                updateUIWithCards(emptyList())
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Error during archiving process")
            withContext(Dispatchers.Main) {
                showToast("Ocurrió un error al archivar tarjetas")
            }
            false
        }
    }

    private suspend fun updateCardsAndInsertHistories(
        updatedCards: List<Card>,
        histories: List<History>
    ) {
        val cardsUpdated = cardRepository.updateMultipleCards(updatedCards)
        if (!cardsUpdated) throw Exception("Error al actualizar tarjetas")

        val historiesInserted = historyRepository.insertHistories(histories)
        if (!historiesInserted) throw Exception("Error al insertar historiales")
    }

    private fun getCurrentDate(): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return Date.valueOf(dateFormat.format(java.util.Date()))
    }

    // Method to send to admin settings
    private fun sendToAdminSettings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.tiePass)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piAccess)

        btnConfirm.setOnClickListener {
            val password = passwordInput.text.toString()

            if (password.isEmpty()) {
                showToast("Por favor, ingrese la contraseña")
            } else {
                progressIndicator.visibility = View.VISIBLE
                btnConfirm.isEnabled = false
                passwordInput.isEnabled = false

                checkAdminCredentials(
                    password,
                    passwordInput,
                    dialog,
                    progressIndicator,
                    btnConfirm
                )
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            dialog.dismiss()
        }
    }

    // Method for checking admin credentials
    private fun checkAdminCredentials(
        pass: String,
        passwordInput: TextInputEditText,
        dialog: AlertDialog,
        progressIndicator: LinearProgressIndicator,
        btnConfirm: MaterialButton
    ) {
        activityScope.launch {
            val isAuthenticated = userRepository.login(pass)

            withContext(Dispatchers.Main) {
                progressIndicator.visibility = View.GONE
                btnConfirm.isEnabled = true
                passwordInput.isEnabled = true

                if (isAuthenticated) {
                    val intent = Intent(
                        this@SearchActivity,
                        AdminCardModelsActivity::class.java
                    )
                    startActivity(intent)
                    dialog.dismiss()
                } else {
                    showToast("Contraseña incorrecta")
                }
            }
        }
    }

    // Method for checking criteria before search
    private fun checkCriteria() {
        // Checking if the status is selected
        val selectedCategory: String? = when (binding.tbCategories.checkedButtonId) {
            R.id.btnAll -> "Todos"
            R.id.btnManufacturing -> "Fabricación"
            R.id.btnFiled -> "Archivado"
            R.id.btnService -> "Servicio"
            else -> null
        }

        // Verify if no button is selected
        if (selectedCategory == null) {
            showToast("Selecciona una categoria")
            return
        }

        // Checking the selection of the model
        val selectedModel: String = binding.atvModels.text.toString()
        if (selectedModel.isEmpty()) {
            showToast("Selecciona un modelo")
            return
        }

        // Checking the selection of the month
        val selectedMonth: String = binding.atvMonths.text.toString()
        if (selectedMonth.isEmpty()) {
            showToast("Selecciona un mes")
            return
        }

        // Checking the selection of the year
        val selectedYear: String = binding.atvYears.text.toString()
        if (selectedYear.isEmpty()) {
            showToast("Selecciona un año")
            return
        }

        // If we reach here, everything is valid
        searchCards(selectedModel, selectedMonth, selectedYear, selectedCategory)
    }

    //Method to search for cards
    private fun searchCards(modelName: String, month: String, year: String, category: String) {
        binding.piCards.visibility = View.VISIBLE
        binding.btnSearch.isEnabled = false
        binding.tilSearchCards.isEndIconVisible = false
        binding.tvSearchInformation.text = "Buscando..."

        val cardModelId =
            if (modelName == "Todos") null else cardModels.find { it.modelName == modelName }?.id

        CoroutineScope(Dispatchers.IO).launch {
            val cards = cardRepository.getCardsByAdvanceSearch(cardModelId, month, year, category)

            withContext(Dispatchers.Main) {
                binding.piCards.visibility = View.INVISIBLE

                if (cards.isNotEmpty()) {
                    binding.tieSearchCards.isEnabled = true
                    binding.tilSearchCards.isEndIconVisible = true
                    binding.tilSearchCards.hint = "Filtra entre los resultados"

                    updateUIWithCards(cards)

                    val searchCardsResult = cardAdapter.cardList

                    setTextWatcher(searchCardsResult)

                    binding.imPet.visibility = View.GONE
                    binding.tvSearchInformation.visibility = View.GONE

                    showToast("Se encontraron ${cards.size} resultados")

                    binding.btnArchiveCards.visibility =
                        if (category == "Fabricación") View.VISIBLE else View.GONE

                } else {
                    updateUIWithCards(emptyList())

                    binding.tieSearchCards.isEnabled = false
                    binding.tilSearchCards.isEndIconVisible = false
                    binding.imPet.visibility = View.VISIBLE
                    binding.tvSearchInformation.visibility = View.VISIBLE
                    binding.tvSearchInformation.text = "Losiento, no se\n encontraron resultados"

                    showToast("No se encontraron resultados")
                    binding.btnArchiveCards.visibility = View.GONE
                }

                binding.btnSearch.isEnabled = true
            }
        }
    }

    //Method to update UI with results
    private fun updateUIWithCards(cards: List<Card>) {
        cardAdapter.setCards(cards)
    }

    private fun cancelCoroutinesAndCloseConnection() {
        activityJob.cancelChildren()
        DataBaseConnection.closeConnection()
    }

    //Method for simplify Toast creation
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}