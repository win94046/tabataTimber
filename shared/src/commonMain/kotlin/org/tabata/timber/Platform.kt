package org.tabata.timber

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform