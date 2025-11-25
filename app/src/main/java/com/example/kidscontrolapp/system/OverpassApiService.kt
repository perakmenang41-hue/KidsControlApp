package com.example.kidscontrolapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class StorePOI(val name: String, val lat: Double, val lon: Double)

object OverpassApiService {

    // Fetch nearby shops (amenity=shop) within 500m radius
    suspend fun fetchNearbyStores(lat: Double, lon: Double, radius: Int = 500): List<StorePOI> =
        withContext(Dispatchers.IO) {
            val overpassQuery = """
                [out:json];
                (
                  node["shop"](around:$radius,$lat,$lon);
                  way["shop"](around:$radius,$lat,$lon);
                  relation["shop"](around:$radius,$lat,$lon);
                );
                out center;
            """.trimIndent()

            val url = "https://overpass-api.de/api/interpreter?data=${overpassQuery.replace("\n","")}"

            return@withContext try {
                val response = URL(url).readText()
                val json = JSONObject(response)
                val elements = json.getJSONArray("elements")
                val stores = mutableListOf<StorePOI>()

                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    val tags = elem.optJSONObject("tags")
                    val name = tags?.optString("name") ?: "Unnamed Shop"

                    val (latElem, lonElem) = when {
                        elem.has("lat") && elem.has("lon") -> elem.getDouble("lat") to elem.getDouble("lon")
                        elem.has("center") -> {
                            val center = elem.getJSONObject("center")
                            center.getDouble("lat") to center.getDouble("lon")
                        }
                        else -> continue
                    }

                    stores.add(StorePOI(name, latElem, lonElem))
                }

                stores
            } catch (e: Exception) {
                Log.e("OverpassApi", "Error fetching stores: ${e.message}")
                emptyList()
            }
        }
}
