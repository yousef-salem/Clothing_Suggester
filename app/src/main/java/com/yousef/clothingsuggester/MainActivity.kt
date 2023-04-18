package com.yousef.clothingsuggester


import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.yousef.clothingsuggester.data.MainData
import com.yousef.clothingsuggester.data.WeatherData
import com.yousef.clothingsuggester.databinding.ActivityMainBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()
    var degree: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_ClothingSuggester)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        makeRequestOkHttp()
        binding.retryBtn.setOnClickListener {
            makeRequestOkHttp()
        }
    }


    private fun makeRequestOkHttp(city: String = "cairo") {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.openweathermap.org")
            .addEncodedPathSegments("/data/2.5/weather")
            .addQueryParameter("q", "$city")
            .addQueryParameter("appid", "5591e9a22ae1fe0c591a03b968c6e3ff")
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.mainWeather.visibility =View.GONE
                    binding.suggestText.visibility =View.GONE
                    binding.imageViewClothes.visibility = View.GONE
                    binding.combackText.visibility =View.GONE
                    binding.retryBtn.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "Its a fail!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 404) {
                    runOnUiThread {
                        binding.mainWeather.visibility =View.GONE
                        binding.suggestText.visibility =View.GONE
                        binding.combackText.visibility =View.GONE
                        binding.imageViewClothes.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "City not found!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return
                }
                response.body?.string()?.let { jsonString ->
                    val json = JSONObject(jsonString)
                    val main = json.getJSONObject("main")
                    val weather = json.getJSONArray("weather").get(0)
                    val result = Gson().fromJson(main.toString(), MainData::class.java)
                    degree = (result.temp - 273).toInt()
                    val icon = Gson().fromJson(weather.toString(), WeatherData::class.java).icon
                    chooseClothes(degree!!, city)
                    setWeatherLayout(degree!!, city, icon)
                    runOnUiThread {
                        binding.mainWeather.visibility =View.VISIBLE
                        binding.imageViewClothes.visibility = View.VISIBLE
                        binding.suggestText.visibility =View.VISIBLE
                        binding.combackText.visibility =View.VISIBLE
                        binding.retryBtn.visibility = View.GONE
                    }
                }

            }

        })


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu?.findItem(R.id.search_item)
        val searchView = searchItem?.actionView as SearchView
        searchView.queryHint = "city"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    makeRequestOkHttp(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    fun setWeatherLayout(degree: Int, city: String, icon: String) {
        runOnUiThread {
            binding.countryTv.text = "$city"
            binding.degreeTv.text = "$degree°C"
        }
        loadImage(icon)
    }

    private fun loadImage(icon: String) {
        val url = "https://openweathermap.org/img/w/$icon.png"
        runOnUiThread {
            Glide.with(this).load(url)
                .error(com.google.android.material.R.drawable.mtrl_ic_error)
                .into(binding.icondIm)
        }
    }

    fun chooseClothes(degree: Int, city: String) {
        runOnUiThread {
            binding.imageViewClothes.setImageDrawable(getRandomDrawable(degree, city))
        }
    }

    private fun getRandomDrawable(degree: Int, city: String): Drawable {
        val drawables = listOf(
            R.drawable.t_shirt_1,
            R.drawable.t_shirt_2,
            R.drawable.t_shirt_3,
            R.drawable.t_shirt_4,
            R.drawable.t_shirt_5,
            R.drawable.shirt_1,
            R.drawable.shirt_2,
            R.drawable.shirt_3,
            R.drawable.shirt_4,
            R.drawable.shirt_5,
            R.drawable.jacket_1,
            R.drawable.jacket_2,
            R.drawable.jacket_4,
            R.drawable.jacket_5
        )
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val currentDate = LocalDate.now().toString()
        val key = "$city-$currentDate"
        var lastDrawableIndex = sharedPreferences.getInt(key, -1)

        val drawableId = if (lastDrawableIndex != -1) {
            drawables[lastDrawableIndex]
        } else {
            var randomIndex: Int?
            do {
                randomIndex = when (degree) {
                    in Int.MIN_VALUE..20 -> (10 until 14).random()
                    in 21..29 -> (5 until 10).random()
                    else -> (0 until 5).random()
                }
            } while (randomIndex == lastDrawableIndex)
            lastDrawableIndex = randomIndex!!
            sharedPreferences.edit().putInt(key, lastDrawableIndex).apply()
            drawables[randomIndex]
        }
        return ContextCompat.getDrawable(baseContext, drawableId)!!
    }
}
