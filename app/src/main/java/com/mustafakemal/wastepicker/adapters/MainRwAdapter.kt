package com.mustafakemal.wastepicker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mustafakemal.wastepicker.R
import com.mustafakemal.wastepicker.retrofit.ContainerModel
import kotlinx.android.synthetic.main.container_list_item.view.*

class MainRwAdapter(private val data: List<ContainerModel>): RecyclerView.Adapter<MainRwAdapter.MainRwViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRwViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.container_list_item, parent, false)
        return MainRwViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MainRwViewHolder, position: Int) {
        holder.containerId.text = data[position].containerId.toString()
        holder.connectionCount.text = data[position].distance.size.toString()
        holder.latitude.text = data[position].latitude.toString()
        holder.longitude.text = data[position].longitude.toString()
    }

    class MainRwViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val containerId: TextView = view.findViewById(R.id.tx_container_id)
        val connectionCount: TextView = view.findViewById(R.id.active_connection_count)
        val latitude: TextView = view.findViewById(R.id.tx_container_lat)
        val longitude: TextView = view.findViewById(R.id.tx_container_lng)


    }
}