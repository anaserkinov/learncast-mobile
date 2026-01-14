package me.anasmusa.learncast.data.model

import me.anasmusa.learncast.Strings

enum class Filters(
    val title: Int,
) {
    Latest(Strings.LATEST),
    InProgress(Strings.IN_PROGRESS),
    Downloads(Strings.DOWNLOADS),
    MostSnipped(Strings.MOST_SNIPPED),
    Favourite(Strings.FAVOURITE_LESSONS),
}
