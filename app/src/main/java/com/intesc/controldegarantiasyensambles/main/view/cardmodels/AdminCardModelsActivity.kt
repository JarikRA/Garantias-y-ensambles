package com.intesc.controldegarantiasyensambles.main.view.cardmodels

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivityAdminSettingsBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.CardModelDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.CardModel
import com.intesc.controldegarantiasyensambles.main.repository.CardModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminCardModelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSettingsBinding
    private lateinit var cardModelAdapter: CardModelAdapter
    private lateinit var cardModelRepository: CardModelRepository

    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cardModelRepository = CardModelRepository(CardModelDaoImpl())
        fetchCardModels()

        binding.fbAdd.setOnClickListener { newCardModel() }
    }

    //Method to load all card models
    private fun fetchCardModels() {
        binding.pbModelCards.visibility = View.VISIBLE

        activityScope.launch {
            val cardModels = withContext(Dispatchers.IO) {
                cardModelRepository.getAllCardModels()
            }

            if (binding.rvModels.adapter == null) {
                cardModelAdapter = CardModelAdapter(cardModels.toMutableList(), { cardModel ->
                    editCardModel(cardModel)
                }) { cardModel ->
                    deleteCardModel(cardModel)
                }
                binding.rvModels.layoutManager = LinearLayoutManager(
                    this@AdminCardModelsActivity
                )
                binding.rvModels.adapter = cardModelAdapter
            } else {
                cardModelAdapter.updateData(cardModels.toMutableList())
            }

            binding.pbModelCards.visibility = View.GONE

            if (cardModels.isEmpty()) {
                binding.imPet.visibility = View.VISIBLE
                binding.tvModelListInfo.visibility = View.VISIBLE
            } else {
                binding.imPet.visibility = View.GONE
                binding.tvModelListInfo.visibility = View.GONE
            }
        }
    }

    // Method for adding a new card model
    private fun newCardModel() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_card_model, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val tieModelName = dialogView.findViewById<TextInputEditText>(R.id.tieModelName)
        val tieAbbreviatedModel =
            dialogView.findViewById<TextInputEditText>(R.id.tieAbbreviatedModel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piNewModel)

        btnConfirm.setOnClickListener {
            val modelName = tieModelName.text.toString().trim()
            val abbreviatedModel = tieAbbreviatedModel.text.toString().trim()

            if (modelName.isBlank() || abbreviatedModel.isBlank()) {
                showToast("Por favor, llena toda la información antes de agregar.")
                return@setOnClickListener
            }

            // Show progress indicator and disable confirm button
            progressIndicator.visibility = View.VISIBLE
            btnConfirm.isEnabled = false
            tieModelName.isEnabled = false
            tieAbbreviatedModel.isEnabled = false

            activityScope.launch {

                val newCardModel = CardModel(
                    id = null,
                    modelName = modelName,
                    abbreviatedModel = abbreviatedModel
                )

                val result = withContext(Dispatchers.IO) {
                    cardModelRepository.insertCardModel(newCardModel)
                }

                // Hide progress indicator and enable confirm button
                progressIndicator.visibility = View.GONE
                btnConfirm.isEnabled = true
                tieModelName.isEnabled = true
                tieAbbreviatedModel.isEnabled = true

                if (result) {
                    showToast("Nuevo modelo de tarjeta agregado correctamente.")

                    binding.pbModelCards.visibility = View.VISIBLE
                    cardModelAdapter.clear()
                    fetchCardModels()
                } else {
                    showToast("Error al agregar el nuevo modelo de tarjeta.")
                }
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            alertDialog.dismiss()
        }
    }

    // Method for editing a card model
    private fun editCardModel(cardModel: CardModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_card_model, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val tieModelName = dialogView.findViewById<TextInputEditText>(R.id.tieModelName)
        val tieAbbreviatedModel =
            dialogView.findViewById<TextInputEditText>(R.id.tieAbbreviatedModel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piEdit)

        tieModelName.setText(cardModel.modelName)
        tieAbbreviatedModel.setText(cardModel.abbreviatedModel)

        btnConfirm.setOnClickListener {
            val updatedCardModel = cardModel.copy(
                modelName = tieModelName.text.toString(),
                abbreviatedModel = tieAbbreviatedModel.text.toString()
            )

            progressIndicator.visibility = View.VISIBLE
            btnConfirm.isEnabled = false
            tieModelName.isEnabled = false
            tieAbbreviatedModel.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    cardModelRepository.updateCardModel(updatedCardModel)
                }

                progressIndicator.visibility = View.GONE
                btnConfirm.isEnabled = true
                tieModelName.isEnabled = true
                tieAbbreviatedModel.isEnabled = true

                if (result) {
                    showToast("Modelo de tarjeta actualizado correctamente.")
                    cardModelAdapter.updateCardModel(updatedCardModel)
                } else {
                    showToast("Error al actualizar el modelo de tarjeta.")
                }
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            alertDialog.dismiss()
        }
    }

    //Method to delete a card model
    private fun deleteCardModel(cardModel: CardModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete_card_model, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piEdit)

        btnConfirm.setOnClickListener {
            // Show progress indicator and disable confirm button
            progressIndicator.visibility = View.VISIBLE
            btnConfirm.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    cardModelRepository.deleteCardModel(cardModel.id!!)
                }
                // Hide progress indicator and enable confirm button
                progressIndicator.visibility = View.GONE
                btnConfirm.isEnabled = true

                if (result) {
                    cardModelAdapter.removeCardModel(cardModel)
                    showToast("Modelo de tarjeta eliminado correctamente.")
                } else {
                    showToast("Error al eliminar el modelo de tarjeta.")
                }
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            alertDialog.dismiss()
        }
    }

    //Method for displaying information through a snack bar
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Method to cancel the curtains and close the connection to the database
    private fun cancelCoroutinesAndCloseConnection() {
        activityJob.cancelChildren()
        DataBaseConnection.closeConnection()
    }

    //Method on writing in case of destruction of the activity
    override fun onDestroy() {
        super.onDestroy()
        cancelCoroutinesAndCloseConnection()
    }
}



