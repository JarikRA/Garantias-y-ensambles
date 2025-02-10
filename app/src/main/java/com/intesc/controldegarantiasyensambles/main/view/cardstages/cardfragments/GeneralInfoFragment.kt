package com.intesc.controldegarantiasyensambles.main.view.cardstages.cardfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.intesc.controldegarantiasyensambles.databinding.FragmentGeneralInfoBinding
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.User
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModel

@SuppressLint("SetTextI18n")
class GeneralInfoFragment : Fragment() {

    private lateinit var binding: FragmentGeneralInfoBinding
    private lateinit var sharedTabViewModel: SharedTabViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGeneralInfoBinding.inflate(inflater, container, false)
        sharedTabViewModel = ViewModelProvider(requireActivity())[SharedTabViewModel::class.java]

        setupObservers()

        return binding.root
    }

    private fun setupObservers() {
        sharedTabViewModel.card.observe(viewLifecycleOwner) { card ->
            if (card != null) {
                displayCardData(card)
            }
        }

        sharedTabViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                displayUserData(user)
                binding.piLoadingCustomer.visibility = View.GONE
            } else {
                displayNoUserData()
                binding.piLoadingCustomer.visibility = View.GONE
            }
        }
    }

    private fun displayCardData(card: Card) {
        binding.apply {
            tvSerialNumber.text = "No. de serie: ${card.serialNumber}"
            tvCategory.text = "Categoría: ${card.category}"
            tvCreationDate.text = "Fecha de creación: ${card.creationDate}"
            tvUpdateDate.text = "Fecha de actualización: ${card.updateDate}"

            if (card.userId != null) {
                piLoadingCustomer.visibility = View.VISIBLE
            }
        }
    }

    private fun displayUserData(user: User) {
        binding.apply {
            tvTittleCustomer.text = "Información del cliente"
            tvCustomerName.text = "Cliente: ${user.name}"
            tvPhoneNumber.text = "Teléfono: ${user.phoneNumber}"
            tvEmail.text = "Correo: ${user.email}"
            tvZipCode.text = "Código postal: ${user.zipCode}"
            tvAddress.text = "Dirección: ${user.address}"
        }
    }

    private fun displayNoUserData() {
        binding.apply {
            tvTittleCustomer.text = "Tarjeta sin cliente asignado"
            tvCustomerName.text = ""
            tvPhoneNumber.text = ""
            tvEmail.text = ""
            tvZipCode.text = ""
            tvAddress.text = ""
        }
    }
}


