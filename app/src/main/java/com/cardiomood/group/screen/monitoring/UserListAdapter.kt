package com.cardiomood.group.screen.monitoring

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cardiomood.group.R
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxrelay.PublishRelay
import rx.Observable

class UserListAdapter : RecyclerView.Adapter<UserViewHolder>() {

    private var items: List<MonitoredUser> = emptyList()
    private val itemClicks = PublishRelay.create<MonitoredUser>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        items[position].let { item ->
            holder.userName.text = listOf(item.user.lastName, item.user.firstName)
                    .filterNotNull()
                    .joinToString(" ")
                    .trim()
            when (item.status) {
                DeviceStatus.NONE -> {
                    holder.status.visibility = View.GONE
                }
                DeviceStatus.CONNECTED -> {
                    holder.status.visibility = View.VISIBLE
                    holder.status.text = item.heartRate.toString()
                }
                else -> {
                    holder.status.visibility = View.VISIBLE
                    holder.status.text = item.status.name
                }
            }
            holder.itemView.clicks()
                    .map { item }
                    .subscribe(itemClicks)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateContent(items: List<MonitoredUser>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun itemClicks(): Observable<MonitoredUser> = itemClicks

}