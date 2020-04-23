package ticket.checker.admin

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import ticket.checker.R

abstract class AItemsAdapterWithHeader<T, Y>(context : Context) : AItemsAdapter<T>(context) {

    companion object {
        private const val ADAPTER_HEADER_ID = 1
        private const val ADAPTER_COUNT_HEADER = 1
    }

    protected var headerItem : Y? = null
    private var headerHolder : RecyclerView.ViewHolder? = null

    protected var filterType : String? = null
    protected var filterValue : String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ADAPTER_HEADER_ID -> {
                headerHolder = inflateHeaderHolder(parent)
                headerHolder as RecyclerView.ViewHolder
            }
            ADAPTER_ITEM_ID -> {
                inflateItemHolder(parent)
            }
            else -> {
                val view = inflater.inflate(R.layout.footer_loading,parent, false)
                footerHolder = FooterHolder(view)
                footerHolder as RecyclerView.ViewHolder
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            ADAPTER_HEADER_ID -> {
                headerHolder = holder
                updateHeaderInfo(holder, filterType, filterValue, headerItem)
            }
            ADAPTER_ITEM_ID -> {
                val item = items[position - ADAPTER_COUNT_HEADER]
                updateItemInfo(holder, item)
            }
            ADAPTER_FOOTER_ID -> {
                footerHolder = holder
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + ADAPTER_COUNT_HEADER + ADAPTER_COUNT_FOOTER
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> ADAPTER_HEADER_ID
            position < items.size + ADAPTER_COUNT_HEADER -> ADAPTER_ITEM_ID
            else -> ADAPTER_FOOTER_ID
        }
    }

    override fun itemAdded(addedItem : T) {
        items.add(0, addedItem)
        notifyItemInserted(1)
    }

    override fun itemEdited(editedItem : T, position : Int) {
        if(isItemPosition(position)) {
            items.removeAt(position-1)
            items.add(position-1, editedItem)
            notifyItemChanged(position)
        }
    }

    override fun itemRemoved(position : Int) {
        if(isItemPosition(position)) {
            items.removeAt(position-1)
            notifyItemRemoved(position)
        }
    }

    override fun getItemId(position: Int): Long {
        return when(position) {
            0, items.size + 1 -> RecyclerView.NO_ID
            else -> getItemId(items[position - 1])
        }
    }

    override fun getItemByPosition(position: Int): T? {
        if(isItemPosition(position)) {
            return items[position-1]
        }
        return null
    }

    override fun updateItemsList(updatedItems: List<T>) {
        val startItemsIndex = items.size + ADAPTER_COUNT_HEADER
        val endItemsIndex = startItemsIndex + updatedItems.size - ADAPTER_COUNT_HEADER
        items.addAll(updatedItems)
        notifyItemRemoved(startItemsIndex) // footer is removed
        notifyItemRangeInserted(startItemsIndex, endItemsIndex)
    }

    override fun resetItemsList() {
        val endItemsIndex = items.size + ADAPTER_COUNT_HEADER + ADAPTER_COUNT_FOOTER
        items = mutableListOf()
        setHeaderVisibility(headerHolder as RecyclerView.ViewHolder, false)
        notifyItemRangeRemoved(ADAPTER_COUNT_HEADER, endItemsIndex)
    }


    fun updateHeaderInfo(filterT: String?, filterV : String, headerItem: Y) {
        this.filterType = filterT
        this.filterValue = filterV
        this.headerItem = headerItem

        if(headerHolder != null) {
            updateHeaderInfo(headerHolder as RecyclerView.ViewHolder, filterType, filterValue, headerItem)
        }
    }

    protected abstract fun inflateHeaderHolder(parent: ViewGroup) : RecyclerView.ViewHolder

    protected abstract fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filterType : String?, filterValue : String, itemStats : Y?)

    protected abstract fun setHeaderVisibility(holder : RecyclerView.ViewHolder, isVisible : Boolean)

}