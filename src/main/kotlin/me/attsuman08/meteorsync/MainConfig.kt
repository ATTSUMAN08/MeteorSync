package me.attsuman08.meteorsync

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.event.config.ModConfigEvent

object MainConfig {
    private val builder = ForgeConfigSpec.Builder()

    private val redisIdentifierConfig: ForgeConfigSpec.ConfigValue<String> = builder
        .comment("識別子")
        .define("redisIdentifier", "CH1")

    private val redisHostConfig: ForgeConfigSpec.ConfigValue<String> = builder
        .comment("Redisのホスト")
        .define("redisHost", "127.0.0.1")

    private val redisPortConfig: ForgeConfigSpec.ConfigValue<Int> = builder
        .comment("Redisのポート")
        .define("redisPort", 6379)

    private val redisUsernameConfig: ForgeConfigSpec.ConfigValue<String> = builder
        .comment("Redisのユーザー名")
        .define("redisUsername", "null")

    private val redisPasswordConfig: ForgeConfigSpec.ConfigValue<String> = builder
        .comment("Redisのパスワード")
        .define("redisPassword", "null")

    private val redisSSLConfig: ForgeConfigSpec.ConfigValue<Boolean> = builder
        .comment("RedisのSSL")
        .define("redisSSL", false)

    val spec: ForgeConfigSpec = builder.build()

    var redisIdentifier: String = "CH1"
    var redisHost: String = "127.0.0.1"
    var redisPort: Int = 6379
    var redisUsername: String = "null"
    var redisPassword: String = "null"
    var redisSSL: Boolean = false

    fun onConfigLoad(event: ModConfigEvent) {
        redisIdentifier = redisIdentifierConfig.get()
        redisHost = redisHostConfig.get()
        redisPort = redisPortConfig.get()
        redisUsername = redisUsernameConfig.get()
        redisPassword = redisPasswordConfig.get()
        redisSSL = redisSSLConfig.get()
    }
}
