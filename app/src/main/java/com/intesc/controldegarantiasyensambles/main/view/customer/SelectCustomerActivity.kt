package com.intesc.controldegarantiasyensambles.main.view.customer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.intesc.controldegarantiasyensambles.R
import com.intesc.controldegarantiasyensambles.databinding.ActivitySelectCustomerBinding
import com.intesc.controldegarantiasyensambles.main.dao.implements.UserDaoImpl
import com.intesc.controldegarantiasyensambles.main.database.DataBaseConnection
import com.intesc.controldegarantiasyensambles.main.model.User
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectCustomerBinding
    private lateinit var customerAdapter: CustomerAdapter
    private lateinit var userRepository: UserRepository
    private lateinit var allCustomers: List<User>

    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(UserDaoImpl())
        binding.fbRegisterCustomer.setOnClickListener { newCustomer() }

        fetchCustomers()
        setupTextWatcher()
    }

    private fun fetchCustomers() {
        binding.pbCustomers.visibility = View.VISIBLE

        activityScope.launch {
            val customers = withContext(Dispatchers.IO) {
                userRepository.getAllUsers()
            }

            allCustomers = customers

            if (binding.rvCustomers.adapter == null) {
                customerAdapter = CustomerAdapter(customers.toMutableList(),
                    onEditClick = { customer ->
                        editCustomer(customer)
                    },
                    onSelectClick = { selectedCustomer ->
                        cancelCoroutinesAndCloseConnection()
                        val resultIntent = Intent().apply {
                            putExtra("selectedCustomer", selectedCustomer)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }

                )
                binding.rvCustomers.layoutManager = LinearLayoutManager(this@SelectCustomerActivity)
                binding.rvCustomers.adapter = customerAdapter
            } else {
                customerAdapter.updateData(customers.toMutableList())
            }

            binding.pbCustomers.visibility = View.GONE

            if (customers.isEmpty()) {
                binding.imPet.visibility = View.VISIBLE
                binding.tvCustomersListInfo.visibility = View.VISIBLE
                binding.tieSearchCustomers.isEnabled = false
            } else {
                binding.imPet.visibility = View.GONE
                binding.tvCustomersListInfo.visibility = View.GONE
                binding.tieSearchCustomers.isEnabled = true
            }
        }
    }

    private fun setupTextWatcher() {
        binding.tieSearchCustomers.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                filterCustomers(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    @SuppressLint("SetTextI18n")
    private fun filterCustomers(query: String) {
        val filteredList = if (query.isBlank()) {
            allCustomers
        } else {
            allCustomers.filter {
                it.name.lowercase().contains(query) ||
                        it.phoneNumber.lowercase().contains(query) ||
                        it.email.lowercase().contains(query) ||
                        it.address.lowercase().contains(query)
            }
        }

        customerAdapter.updateData(filteredList.toMutableList())

        if (filteredList.isEmpty()) {
            binding.imPet.visibility = View.VISIBLE
            binding.tvCustomersListInfo.visibility = View.VISIBLE
            binding.tvCustomersListInfo.text = "No se encontraron resultados"
        } else {
            binding.imPet.visibility = View.GONE
            binding.tvCustomersListInfo.visibility = View.GONE
        }
    }

    private fun newCustomer() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_customer, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val tieName = dialogView.findViewById<TextInputEditText>(R.id.tieCustomerName)
        val tiePhoneNumber = dialogView.findViewById<TextInputEditText>(R.id.tiePhoneNumber)
        val tieEmail = dialogView.findViewById<TextInputEditText>(R.id.tieEmail)
        val tieAddress = dialogView.findViewById<TextInputEditText>(R.id.tieAddress)
        val tieZipCode = dialogView.findViewById<TextInputEditText>(R.id.tieZipCode)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnRegister)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.piNewCustomer)

        btnConfirm.setOnClickListener {
            val name = tieName.text.toString().trim()
            val phoneNumber = tiePhoneNumber.text.toString().trim()
            val email = tieEmail.text.toString().trim()
            val address = tieAddress.text.toString().trim()
            val zipCode = tieZipCode.text.toString().trim()

            if (name.isBlank() || phoneNumber.isBlank() || email.isBlank() || address.isBlank()
                || zipCode.isBlank()
            ) {
                showToast("Por favor, llena toda la información antes de agregar")
                return@setOnClickListener
            }

            progressIndicator.visibility = View.VISIBLE
            btnConfirm.isEnabled = false
            tieName.isEnabled = false
            tiePhoneNumber.isEnabled = false
            tieEmail.isEnabled = false
            tieAddress.isEnabled = false
            tieZipCode.isEnabled = false

            activityScope.launch {
                val newCustomer = User(
                    id = 0,
                    name = name,
                    phoneNumber = phoneNumber,
                    email = email,
                    address = address,
                    zipCode = zipCode
                )

                val result = withContext(Dispatchers.IO) {
                    userRepository.insertUser(newCustomer)
                }

                progressIndicator.visibility = View.GONE
                btnConfirm.isEnabled = true
                tieName.isEnabled = true
                tiePhoneNumber.isEnabled = true
                tieEmail.isEnabled = true
                tieAddress.isEnabled = true
                tieZipCode.isEnabled = true

                if (result != null) {
                    showToast("Nuevo cliente agregado correctamente.")
                    customerAdapter.addCustomer(newCustomer)
                } else {
                    showToast("Error al agregar el nuevo cliente.")
                }
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            alertDialog.dismiss()
        }
    }

    private fun editCustomer(customer: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_customer, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val tieName = dialogView.findViewById<TextInputEditText>(R.id.tieCustomerName)
        val tiePhoneNumber = dialogView.findViewById<TextInputEditText>(R.id.tiePhoneNumber)
        val tieEmail = dialogView.findViewById<TextInputEditText>(R.id.tieEmail)
        val tieAddress = dialogView.findViewById<TextInputEditText>(R.id.tieAddress)
        val tieZipCode = dialogView.findViewById<TextInputEditText>(R.id.tieZipCode)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdate)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val progressIndicator =
            dialogView.findViewById<LinearProgressIndicator>(R.id.piEditCustomer)

        tieName.setText(customer.name)
        tiePhoneNumber.setText(customer.phoneNumber)
        tieEmail.setText(customer.email)
        tieAddress.setText(customer.address)
        tieZipCode.setText(customer.zipCode)

        btnUpdate.setOnClickListener {
            val name = tieName.text.toString().trim()
            val phoneNumber = tiePhoneNumber.text.toString().trim()
            val email = tieEmail.text.toString().trim()
            val address = tieAddress.text.toString().trim()
            val zipCode = tieZipCode.text.toString().trim()

            if (name.isBlank() || phoneNumber.isBlank() || email.isBlank() || address.isBlank()
                || zipCode.isBlank()
            ) {
                showToast("Por favor, llena toda la información antes de actualizar")
                return@setOnClickListener
            }

            val updatedCustomer = customer.copy(
                name = name,
                phoneNumber = phoneNumber,
                email = email,
                address = address,
                zipCode = zipCode
            )

            progressIndicator.visibility = View.VISIBLE
            btnUpdate.isEnabled = false
            tieName.isEnabled = false
            tiePhoneNumber.isEnabled = false
            tieEmail.isEnabled = false
            tieAddress.isEnabled = false
            tieZipCode.isEnabled = false

            activityScope.launch {
                val result = withContext(Dispatchers.IO) {
                    userRepository.updateUser(updatedCustomer)
                }

                progressIndicator.visibility = View.GONE
                btnUpdate.isEnabled = true
                tieName.isEnabled = true
                tiePhoneNumber.isEnabled = true
                tieEmail.isEnabled = true
                tieAddress.isEnabled = true
                tieZipCode.isEnabled = true

                if (result) {
                    showToast("Cliente actualizado correctamente.")
                    customerAdapter.updateCustomer(updatedCustomer)
                } else {
                    showToast("Error al actualizar el cliente.")
                }
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            cancelCoroutinesAndCloseConnection()
            alertDialog.dismiss()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun cancelCoroutinesAndCloseConnection() {
        activityJob.cancelChildren()
        DataBaseConnection.closeConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCoroutinesAndCloseConnection()
    }
}
