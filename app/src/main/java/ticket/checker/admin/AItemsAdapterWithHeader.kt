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
    protected var headerHolder : RecyclerView.ViewHolder? = null

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
        return super.getItemCount() + ADAPTER_COUNT_HEADER
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
            notifyItemChanged(position-1)
        }
    }

    override fun itemRemoved(position : Int) {
        if(isItemPosition(position)) {
            items.removeAt(position-1)
            notifyItemRemoved(position-1)
        }
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