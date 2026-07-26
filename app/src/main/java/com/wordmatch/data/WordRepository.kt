package com.wordmatch.data

import android.content.Context
import com.google.gson.Gson
import com.wordmatch.config.GameConfig
import com.wordmatch.model.JsonBinResponse
import com.wordmatch.model.WordItem
import com.wordmatch.network.JsonBinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface WordRepository {
    suspend fun loadWords(): List<WordItem>
}

/**
 * Loads words from a public JSONBin.io bin when [binId] is set, otherwise (or on any network
 * failure) falls back to the bundled assets/words.json so the app always has words to show.
 */
class WordRepositoryImpl(
    private val context: Context,
    private val binId: String,
    private val api: JsonBinApi = Retrofit.Builder()
        .baseUrl(GameConfig.JSONBIN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(JsonBinApi::class.java)
) : WordRepository {

    override suspend fun loadWords(): List<WordItem> = withContext(Dispatchers.IO) {
        if (binId.isNotBlank()) {
            runCatching { api.getWords(binId).record }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return@withContext it }
        }
        loadBundled()
    }

    private fun loadBundled(): List<WordItem> {
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        return Gson().fromJson(json, JsonBinResponse::class.java).record
    }
}
