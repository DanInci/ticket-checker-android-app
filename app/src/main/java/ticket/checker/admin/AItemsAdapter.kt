package ticket.checker.admin

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import ticket.checker.ActivityAdmin.Companion.LIST_ALL

/**
 * Created by Dani on 09.02.2018.
 */
abstract class AItemsAdapter<in T, in Y> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM = 0
        private const val HEADER = 1
    }

    private var headerHolder : RecyclerView.ViewHolder? = null

    private var filter : String = LIST_ALL
    private var items: MutableList<T> = mutableListOf()
    private var itemStats : Y? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> {
                headerHolder = inflateHeaderHolder(parent)
                return headerHolder as RecyclerView.ViewHolder
            }
            else -> {
                inflateItemHolder(parent)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            ITEM -> {
                val item = items[position - HEADER]
                updateItemInfo(holder, item)
            }
            HEADER -> {
                updateHeaderInfo(holder, filter, itemStats)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + HEADER
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            HEADER
        } else {
            ITEM
        }
    }

    fun updateItemsList(updatedItems: List<T>) {
        val startItemsIndex = items.size + HEADER
        val endItemsIndex = startItemsIndex + updatedItems.size - HEADER
        items.addAll(updatedItems)
        notifyItemRangeInserted(startItemsIndex, endItemsIndex)
    }

    fun resetItemsList() {
        val endItemsIndex = items.size + HEADER
        items = mutableListOf()
        notifyItemRangeRemoved(HEADER, endItemsIndex - HEADER)
    }

    fun updateHeaderInfo(filter: String, itemStats: Y) {
        this.filter = filter
        this.itemStats = itemStats

        if(headerHolder != null) {
            updateHeaderInfo(headerHolder as RecyclerView.ViewHolder, filter, itemStats)
        }
    }

    abstract fun inflateItemHolder(parent: ViewGroup) : RecyclerView.ViewHolder

    abstract fun inflateHeaderHolder(parent: ViewGroup) : RecyclerView.ViewHolder

    abstract fun updateItemInfo(holder : RecyclerView.ViewHolder, item : T)

    abstract fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filter : String, itemStats : Y?)
}