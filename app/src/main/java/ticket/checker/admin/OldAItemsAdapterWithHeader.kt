package ticket.checker.admin

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import ticket.checker.R

/**
 * Created by Dani on 09.02.2018.
 */
@Deprecated(message = "Use AItemsAdapterWithHeader instead")
abstract class OldAItemsAdapterWithHeader<T, Y>(context : Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM = 0
        private const val HEADER = 1
        private const val COUNT_HEADER = 1
        private const val FOOTER = 2
        private const val COUNT_FOOTER = 1
    }

    protected val inflater: LayoutInflater = LayoutInflater.from(context)

    private var headerHolder : RecyclerView.ViewHolder? = null
    private var footerHolder : RecyclerView.ViewHolder? = null

    protected var filterType : String? = null
    protected var filterValue : String = ""

    protected var items: MutableList<T> = mutableListOf()
    protected var itemStats : Y? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> {
                headerHolder = inflateHeaderHolder(parent)
                headerHolder as RecyclerView.ViewHolder
            }
            ITEM -> {
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
            ITEM -> {
                val item = items[position - COUNT_HEADER]
                updateItemInfo(holder, item)
            }
            HEADER -> {
                headerHolder = holder
                updateHeaderInfo(holder, filterType, filterValue, itemStats)
            }
            FOOTER -> {
                footerHolder = holder
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + COUNT_HEADER + COUNT_FOOTER
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> HEADER
            position < items.size + COUNT_HEADER -> ITEM
            else -> FOOTER
        }
    }

    fun setLoading(isLoading : Boolean) {
        if(footerHolder != null) {
            (footerHolder as FooterHolder).showLoadingSpinner(isLoading)
        }
    }

    fun updateItemsList(updatedItems: List<T>) {
        val startItemsIndex = items.size + COUNT_HEADER
        val endItemsIndex = startItemsIndex + updatedItems.size - COUNT_HEADER
        items.addAll(updatedItems)
        notifyItemRemoved(startItemsIndex) // footer is removed
        notifyItemRangeInserted(startItemsIndex, endItemsIndex)
    }

    fun resetItemsList() {
        val endItemsIndex = items.size + COUNT_HEADER + COUNT_FOOTER
        items = mutableListOf()
        setHeaderVisibility(headerHolder as RecyclerView.ViewHolder, false)
        notifyItemRangeRemoved(COUNT_HEADER, endItemsIndex)
    }

    fun updateHeaderInfo(filterT: String?, filterV : String, itemStats: Y) {
        this.filterType = filterT
        this.filterValue = filterV
        this.itemStats = itemStats

        if(headerHolder != null) {
            updateHeaderInfo(headerHolder as RecyclerView.ViewHolder, filterType, filterValue, itemStats)
        }
    }

    protected fun isItemPosition(position : Int) : Boolean {
        if(getItemViewType(position) == ITEM) {
            return true
        }
        return false
    }

    abstract fun itemAdded(addedItem : T)

    abstract fun itemEdited(editedItem : T, position : Int)

    abstract fun itemRemoved(position : Int)

    abstract fun launchInfoActivity(view : View, position : Int)

    protected abstract fun inflateItemHolder(parent: ViewGroup) : RecyclerView.ViewHolder

    protected abstract fun inflateHeaderHolder(parent: ViewGroup) : RecyclerView.ViewHolder

    protected abstract fun updateItemInfo(holder : RecyclerView.ViewHolder, item : T)

    protected abstract fun setHeaderVisibility(holder : RecyclerView.ViewHolder, isVisible : Boolean)

    protected abstract fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filterType : String?, filterValue : String, itemStats : Y?)

    private class FooterHolder(viewItem : View) : RecyclerView.ViewHolder(viewItem) {
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