package me.anasmusa.learncast.data.model

import me.anasmusa.learncast.Strings

enum class Filters(val title: Int) {
    Latest(Strings.latest),
    InProgress(Strings.in_progress),
    Downloads(Strings.downloads),
    MostSnipped(Strings.most_snipped),
    Favourite(Strings.favourite_lessons)
}