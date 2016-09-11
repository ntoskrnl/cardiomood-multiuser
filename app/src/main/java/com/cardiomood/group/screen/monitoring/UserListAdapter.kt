package com.cardiomood.group.screen.monitoring

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.cardiomood.group.R
import com.cardiomood.group.api.User

class UserListAdapter : RecyclerView.Adapter<UserViewHolder>() {

    private var items: List<User> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        items[position].let {
            holder.userName.text = listOf(it.lastName, it.firstName)
                    .filterNotNull()
                    .joinToString(" ")
                    .trim()
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateContent(items: List<User>) {
        this.items = items
        notifyDataSetChanged()
    }

}