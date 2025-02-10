package com.intesc.controldegarantiasyensambles.main.view.customer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.intesc.controldegarantiasyensambles.databinding.CustomerItemListBinding
import com.intesc.controldegarantiasyensambles.main.model.User

class CustomerAdapter(
    private var customerList: MutableList<User>,
    private val onEditClick: (User) -> Unit,
    private val onSelectClick: (User) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    fun addCustomer(newCustomer: User) {
        customerList.add(newCustomer)
        notifyItemInserted(customerList.size - 1)
    }

    fun updateCustomer(updatedCustomer: User) {
        val position = customerList.indexOfFirst { it.id == updatedCustomer.id }
        if (position != -1) {
            customerList[position] = updatedCustomer
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newCustomerList: MutableList<User>) {
        customerList.clear()
        customerList.addAll(newCustomerList)
        notifyDataSetChanged()
    }

    inner class CustomerViewHolder(private val binding: CustomerItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: User) {
            binding.tvCustomerName.text = customer.name
            binding.tvPhoneNumber.text = customer.phoneNumber
            binding.btnEdit.setOnClickListener { onEditClick(customer) }
            binding.btnSelect.setOnClickListener { onSelectClick(customer) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = CustomerItemListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun getItemCount(): Int = customerList.size

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customerList[position])
    }
}
