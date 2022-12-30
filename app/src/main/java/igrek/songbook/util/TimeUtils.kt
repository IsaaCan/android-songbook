package igrek.songbook.util

import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
private val iso8601Format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

fun formatTodayDate(): String {
    val date = Date()
    return dateFormat.format(date)
}

fun formatTimestampDate(timestampSeconds: Long): String {
    val date = Date(timestampSeconds * 1000)
    return dateFormat.format(date)
}

fun formatTimestampTime(timestampSeconds: Long): String {
    val date = Date(timestampSeconds * 1000)
    return iso8601Format.format(date)
}