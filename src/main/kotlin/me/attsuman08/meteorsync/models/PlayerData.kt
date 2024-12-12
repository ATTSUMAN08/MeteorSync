package me.attsuman08.meteorsync.models

import com.google.gson.JsonElement

class PlayerData(
    val serverId: String,
    val curiosData: Map<String, JsonElement>
)