package mwongela.songbook.admin


import mwongela.songbook.info.logger.LoggerFactory.logger
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.general.model.Song
import kotlinx.coroutines.Deferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

class SongRankService(
    adminService: LazyInject<AdminService> = appFactory.adminService,
) {
    private val adminService by LazyExtractor(adminService)

    companion object {
        private const val chordsApiBase = "https://chords.mwongela.dev/api/v5"

        private val updatePublicSongIdUrl = { id: String -> "$chordsApiBase/songs/$id" }

        private const val authTokenHeader = "X-Auth-Token"
    }

    private val httpRequester = HttpRequester()
    private val jsonType = MediaType.parse("application/json; charset=utf-8")
    private val jsonSerializer = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
        allowStructuredMapKeys = true
        prettyPrint = false
        useArrayPolymorphism = false
    }

    fun updateRankAsync(song: Song, rank: Double?): Deferred<Result<Unit>> {
        song.rank = rank
        logger.info("Updating song rank: $song")
        val dto = SongRankUpdateDto(rank = song.rank)
        val json = jsonSerializer.encodeToString(SongRankUpdateDto.serializer(), dto)
        val request: Request = Request.Builder()
            .url(updatePublicSongIdUrl(song.id))
            .put(RequestBody.create(jsonType, json))
            .addHeader(authTokenHeader, adminService.userAuthToken)
            .build()
        return httpRequester.httpRequestAsync(request) { response: Response ->
            logger.debug("Update rank response", response.body()?.string())
        }
    }

}

@Serializable
data class SongRankUpdateDto(
    var rank: Double? = null,
)
