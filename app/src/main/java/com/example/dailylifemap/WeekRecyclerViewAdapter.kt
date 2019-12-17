package com.example.dailylifemap

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_weekday.view.*

class WeekRecyclerViewAdapter(private val mapActivity: MapActivity)
    : RecyclerView.Adapter<WeekRecyclerViewAdapter.WeekItemViewHolder>(){
    private var weekdayTitles: List<String> = WeekdayDataMaker.getWeekdayTitleLatestOrder()
    private val placesLongTimeInWeek: PlaceData
    private lateinit var recyclerView: ViewGroup
    private var nowSelectedPosition = 0

    init{
        val weekdayDataMaker = WeekdayDataMaker(mapActivity)
        placesLongTimeInWeek = weekdayDataMaker.getLongTimePlacesOnThisWeek()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekItemViewHolder {
        recyclerView = parent
        val inflater = LayoutInflater.from(parent.context)
        val viewWeekdayItemView = inflater.inflate(R.layout.item_weekday, parent, false)

        Log.d("weekdayTitle", weekdayTitles.toString() + " ${weekdayTitles.count()}")
        Log.d("weekdayCount", itemCount.toString())

        return WeekItemViewHolder(viewWeekdayItemView)
    }

    override fun getItemCount() = weekdayTitles.count()

    override fun onBindViewHolder(holder: WeekItemViewHolder, position: Int) {
        if(position == nowSelectedPosition){
            holder.weekdayItemView.setBackgroundColor(Color.WHITE)
            holder.weekdayTitleView.setTextColor(Color.BLACK)
        }

        if(position < weekdayTitles.count()) {
            holder.weekdayTitleView.text = weekdayTitles[position]
            holder.weekdayItemView.setOnClickListener {
                nowSelectedPosition = position
                mapActivity.setNewWeekday(position)
                recyclerView.children.forEach {
                    it.background = mapActivity.getDrawable(R.color.colorHalfTransparentBlack)
                    it.titleWeekday.setTextColor(Color.WHITE)
                }

                holder.weekdayItemView.setBackgroundColor(Color.WHITE)
                holder.weekdayTitleView.setTextColor(Color.BLACK)
            }
        }

        Log.d("bind", "${holder.weekdayTitleView.text} - $position")
    }

    class WeekItemViewHolder(val weekdayItemView: View)
        : RecyclerView.ViewHolder(weekdayItemView){
        val weekdayTitleView = weekdayItemView.titleWeekday
    }
}