package io.r03el.photograbber.config


data class TelegramConfig(
    val botToken: String,
    val enableFilter: Boolean,
    val groupsToMonitor: List<Long>,
)