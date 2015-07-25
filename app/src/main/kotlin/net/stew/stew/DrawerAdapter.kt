package net.stew.stew

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DrawerAdapter(context: Context) : BaseAdapter() {

    private val listItems = context.getResources().getStringArray(R.array.post_collections) + context.getString(R.string.log_out)
    private var activeItemPosition = 0

    override fun getItem(position: Int) = listItems[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val context = parent.getContext()
        val isActive = position == activeItemPosition
        val textView = when (convertView) {
            null -> {
                LayoutInflater.from(context).inflate(R.layout.drawer_list_item, parent, false) as TextView
            }
            else -> convertView as TextView
        }

        textView.setText(getItem(position))
        textView.setTextColor(context.getResources().getColor(if (isActive) R.color.text_active else android.R.color.primary_text_light))

        return textView
    }

    override fun getCount() = listItems.size()

    fun setActiveItemPosition(position: Int) {
        activeItemPosition = position
        notifyDataSetChanged()
    }
}

