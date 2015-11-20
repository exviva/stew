package net.stew.stew

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DrawerAdapter(context: Context) : BaseAdapter() {

    private val listItems = context.resources.getStringArray(R.array.post_collections) +
        context.getString(R.string.log_out) +
        context.getString(R.string.about)
    private var activeItemPosition = 0

    override fun getItem(position: Int) = listItems[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val context = parent.context
        val isActive = position == activeItemPosition
        val textView = when (convertView) {
            null -> LayoutInflater.from(context).inflate(R.layout.drawer_list_item, parent, false)
            else -> convertView
        } as TextView

        textView.text = getItem(position)
        textView.setTextColor(ContextCompat.getColor(context, if (isActive) R.color.primary else android.R.color.primary_text_light))

        return textView
    }

    override fun getCount() = listItems.size

    fun setActiveItemPosition(position: Int) {
        activeItemPosition = position
        notifyDataSetChanged()
    }
}

