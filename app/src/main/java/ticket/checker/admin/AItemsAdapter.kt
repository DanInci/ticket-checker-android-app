package ticket.checker.admin

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import ticket.checker.R

abstract class AItemsAdapter<T>(context : Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ADAPTER_ITEM_ID = 0
        const val ADAPTER_FOOTER_ID = 2
        const val ADAPTER_COUNT_FOOTER = 1
    }

    protected val inflater: LayoutInflater = LayoutInflater.from(context)

    protected var items: MutableList<T> = mutableListOf()
    protected var footerHolder : RecyclerView.ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
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
            ADAPTER_ITEM_ID -> {
                val item = items[position]
                updateItemInfo(holder, item)
            }
            ADAPTER_FOOTER_ID -> {
                footerHolder = holder
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + ADAPTER_COUNT_FOOTER
    }

    open fun getRealItemsCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < items.size -> ADAPTER_ITEM_ID
            else -> ADAPTER_FOOTER_ID
        }
    }

    fun getPositionByItem(item: T): Int? {
        val foundItem = items.withIndex().find { (_, t) -> t == item }
        if(foundItem != null) {
            return foundItem.index
        }
        return null
    }

    open fun getItemByPosition(position: Int): T? {
        if(isItemPosition(position)) {
            return items[position]
        }
        return null
    }

    fun setLoading(isLoading : Boolean) {
        if(footerHolder != null) {
            (footerHolder as FooterHolder).showLoadingSpinner(isLoading)
        }
    }

    open fun updateItemsList(updatedItems: List<T>) {
        val startItemsIndex = items.size
        val endItemsIndex = startItemsIndex + updatedItems.size
        items.addAll(updatedItems)
        notifyItemRemoved(startItemsIndex) // footer is removed
        notifyItemRangeInserted(startItemsIndex, endItemsIndex)
    }

    open fun resetItemsList() {
        val endItemsIndex = items.size + ADAPTER_COUNT_FOOTER
        items = mutableListOf()
        notifyItemRangeRemoved(0, endItemsIndex)
    }

    open fun itemAdded(addedItem : T) {
        items.add(0, addedItem)
        notifyItemInserted(0)
    }

    open fun itemEdited(editedItem : T, position : Int) {
        if(isItemPosition(position)) {
            items.removeAt(position)
            items.add(position, editedItem)
            notifyItemChanged(position)
        }
    }

    open fun itemRemoved(position : Int) {
        if(isItemPosition(position)) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemId(position: Int): Long {
        return when(position) {
            items.size -> RecyclerView.NO_ID
            else -> getItemId(items[position])
        }
    }

    protected fun isItemPosition(position : Int) : Boolean {
        if(getItemViewType(position) == ADAPTER_ITEM_ID) {
            return true
        }
        return false
    }

    protected abstract fun inflateItemHolder(parent: ViewGroup) : RecyclerView.ViewHolder

    protected abstract fun updateItemInfo(holder : RecyclerView.ViewHolder, item : T)

    protected abstract fun getItemId(item: T): Long

    protected class FooterHolder(viewItem : View) : RecyclerView.ViewHolder(viewItem) {
        private val loadingSpinner : ProgressBar = viewItem.findViewById(R.id.loadingSpinner)

        init {
            showLoadingSpinner(false)
        }

        fun showLoadingSpinner(isVisible : Boolean) {
            if(isVisible) {
                loadingSpinner.visibility = View.VISIBLE
            }
            else {
                loadingSpinner.visibility = View.GONE
            }
        }
    }
}