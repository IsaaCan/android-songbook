package mwongela.songbook.room

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import mwongela.songbook.R
import mwongela.songbook.layout.list.GenericListView

class JoinRoomListView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listViewStyle,
) : GenericListView<Room>(
    context, attrs, defStyleAttr,
    itemViewRes = R.layout.list_item_generic_string,
) {

    var onClickCallback: (item: Room) -> Unit = {}

    constructor(context: Context) : this(context, null, android.R.attr.listViewStyle)

    constructor(context: Context, attrs: AttributeSet) : this(
        context,
        attrs,
        android.R.attr.listViewStyle
    )

    @SuppressLint("SetTextI18n")
    override fun buildView(view: View, item: Room) {
        view.findViewById<TextView>(R.id.itemLabel)?.text = "${item.name} (${item.hostAddress})"
    }

    override fun onClick(item: Room) {
        onClickCallback(item)
    }
}
