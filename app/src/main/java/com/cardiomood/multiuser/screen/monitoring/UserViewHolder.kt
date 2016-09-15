package com.cardiomood.multiuser.screen.monitoring

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.cardiomood.multiuser.R

class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val userName = view.findViewById(R.id.full_name) as TextView
    val status = view.findViewById(R.id.status) as TextView

}