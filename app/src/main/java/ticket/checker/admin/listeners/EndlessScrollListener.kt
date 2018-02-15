package ticket.checker.admin.listeners

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

/**
 * Created by Dani on 08.02.2018.
 */
abstract class EndlessScrollListener(private val layoutManager : LinearLayoutManager) : RecyclerView.OnScrollListener() {
    var currentPage = 0
    var previousTotalItemCount = 0
    var loading = true
    var enabled = true

    private val startingPageIndex = 0
    private val visibleThreshold = 5


    override fun onScrolled(recyclerView : RecyclerView, dx: Int, dy: Int) {
        if(enabled) {
            val totalItemsCount = layoutManager.itemCount
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

            if (totalItemsCount < previousTotalItemCount) {
                currentPage = startingPageIndex
                previousTotalItemCount = totalItemsCount
                if (totalItemsCount == 0) {
                    loading = true
                }
            }

            if (loading && (totalItemsCount > previousTotalItemCount)) {
                loading = false
                previousTotalItemCount = totalItemsCount
            }

            if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemsCount) {
                currentPage++
                onLoadMore(currentPage, totalItemsCount, recyclerView)
                loading = true
            }
        }
    }

    fun resetState() {
        currentPage = startingPageIndex
        previousTotalItemCount = 0
        loading = true
    }

    abstract fun onLoadMore(page : Int, totalItemsCount : Int, recyclerView: RecyclerView)

}