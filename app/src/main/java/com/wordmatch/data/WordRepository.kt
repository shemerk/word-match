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
    /** Loads the dictionary for [binId] (the active child's bin). Blank = bundled words only. */
    suspend fun loadWords(binId: String): List<WordItem>
}

/**
 * Loads words from a public JSONBin.io bin when [binId] is set, otherwise (or on any network
 * failure) falls back to the bundled assets/words.json so the app always has words to show.
 * The bin is passed per call (not baked in) so switching child switches dictionary at runtime.
 */
class WordRepositoryImpl(
    private val context: Context,
    private val api: JsonBinApi = Retrofit.Builder()
        .baseUrl(GameConfig.JSONBIN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(JsonBinApi::class.java)
) : WordRepository {

    override suspend fun loadWords(binId: String): List<WordItem> = withContext(Dispatchers.IO) {
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
