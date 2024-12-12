package me.attsuman08.meteorsync

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import me.attsuman08.meteorsync.models.PlayerData
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.registerConfig
import top.theillusivec4.curios.api.*

@Mod(MeteorSync.ID)
object MeteorSync {
    const val ID = "meteorsync"
    val LOGGER: Logger = LogManager.getLogger(ID)
    private lateinit var jedisPool: JedisPool
    val gson = Gson()

    init {
        // Listener登録
        MOD_BUS.addListener(::onServerSetup)
        MOD_BUS.addListener(MainConfig::onConfigLoad)
        FORGE_BUS.addListener(::onJoin)
        FORGE_BUS.addListener(::onQuit)

        // Configを登録
        registerConfig(ModConfig.Type.COMMON, MainConfig.spec)
    }

    private fun connectRedis(hostname: String, port: Int, usernameTemp: String?, passwordTemp: String?, ssl: Boolean) {
        LOGGER.info("Redisに接続しています...")
        val username: String? = if (usernameTemp == "null") null else usernameTemp
        val password: String? = if (passwordTemp == "null") null else passwordTemp

        jedisPool = if (username != null) {
            JedisPool(JedisPoolConfig(), hostname, port, Protocol.DEFAULT_TIMEOUT, username, password, ssl)
        } else {
            JedisPool(JedisPoolConfig(), hostname, port, Protocol.DEFAULT_TIMEOUT, password, ssl)
        }
        try {
            getJedis().use { jedis ->
                jedis.ping()
            }
        } catch (e: Exception) {
            LOGGER.error("Redisの接続に失敗しました")
            LOGGER.error(e)
            return
        }
        LOGGER.info("Redisに接続しました！ [${MainConfig.redisIdentifier}]")
    }

    /**
     * JedisPoolを取得します。必ずuseを使ってください。
     *
     * @return Jedis
     */
    fun getJedis(): Jedis {
        return jedisPool.resource
    }

    private fun onServerSetup(e: FMLDedicatedServerSetupEvent) {
        LOGGER.info("MeteorSyncを読み込んでいます... ${e.result.name}")
        connectRedis(MainConfig.redisHost, MainConfig.redisPort, MainConfig.redisUsername, MainConfig.redisPassword, MainConfig.redisSSL)
    }

    private fun onJoin(e: PlayerEvent.PlayerLoggedInEvent) {
        getJedis().use { jedis ->
            if (!jedis.exists("MeteorSync:${e.entity.uuid}")) {
                // 初参加の処理
                LOGGER.info("${e.entity.gameProfile.name}は初参加のためデータを作成し、保存します")
                savePlayerData(e.entity)
            } else {
                // 2回目以降の処理
                LOGGER.info("${e.entity.gameProfile.name}のデータを読み込み中です")
                loadPlayerData(e.entity)
            }
        }
    }

    private fun onQuit(e: PlayerEvent.PlayerLoggedOutEvent) {
        LOGGER.info("${e.entity.gameProfile.name}のデータを保存中です")
        savePlayerData(e.entity)
    }

    private fun savePlayerData(p: Player) {
        val curiosInventory = CuriosApi.getCuriosInventory(p).orElseThrow {
            IllegalStateException("${p.gameProfile.name}のデータが見つかりませんでした")
        }
        val curiosData = mutableMapOf<String, JsonElement>()
        curiosInventory.curios.forEach { (slot, stackHandler) ->
            CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, stackHandler.serializeNBT()).resultOrPartial {
                throw IllegalStateException("ItemDataのエンコードに失敗しました")
            }.ifPresent {
                curiosData[slot] = it
            }
        }
        getJedis().use { jedis ->
            jedis.set("MeteorSync:${p.uuid}", gson.toJson(PlayerData(MainConfig.redisIdentifier, curiosData)))
            LOGGER.info("${p.gameProfile.name}のデータを保存しました")
        }
    }

    private fun loadPlayerData(p: Player) {
        getJedis().use { jedis ->
            if (jedis.exists("MeteorSync:${p.uuid}")) {
                val playerData = gson.fromJson(jedis.get("MeteorSync:${p.uuid}"), PlayerData::class.java)
                val curiosInventory = CuriosApi.getCuriosInventory(p).orElseThrow {
                    IllegalStateException("${p.gameProfile.name}のデータが見つかりませんでした")
                }
                playerData.curiosData.forEach { (slot, itemData) ->
                    println("${slot}: $itemData")
                    val compound = CompoundTag.CODEC.parse(JsonOps.INSTANCE, itemData).resultOrPartial {
                        throw IllegalStateException("ItemDataのデコードに失敗しました")
                    }.orElseThrow()
                    curiosInventory.curios[slot]?.deserializeNBT(compound)
                }
                LOGGER.info("${p.gameProfile.name}のデータを読み込みました")
            } else {
                LOGGER.info("${p.gameProfile.name}のデータが見つかりませんでした。同期をスキップします")
            }
        }
    }

}