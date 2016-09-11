package com.cardiomood.group.screen.monitoring

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.cardiomood.group.R

class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val userName = view.findViewById(R.id.full_name) as TextView

}