package com.example.compose_recyclerview.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Abstract class for handling infinite scrolling events in a RecyclerView.
 */
abstract class InfiniteScrollListener : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dy > 0 || dx > 0) {
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is LinearLayoutManager) {
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                    onScrollEnd()
                }
            }
        }
    }

    /**
     * Callback triggered when the user reaches the end of the list during scrolling.
     */
    protected abstract fun onScrollEnd()
}
