package com.namgs.okhttp_test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody


// Model data class (json 형식)
data class OilPriceResponse( //최상단
    @SerializedName("RESULT") val result: OilResult
)
data class OilResult( // oil정보단 모든 리스트는 여기서 관리
    @SerializedName("OIL") val oil: List<OilPrice>
)
data class OilPrice(
    @SerializedName("TRADE_DT") val tradeDate: String,
    @SerializedName("PRODCD") val productCode: String,
    @SerializedName("PRODNM") val productName: String,
    @SerializedName("PRICE") val price: String,
    @SerializedName("DIFF") val diff: String
)








// ViewModel
class OilPriceViewModel : ViewModel() {

    //Intent 정보 상태 정보
    sealed class State {
        object Loading : State()
        data class Success(val oilPrices: List<OilPrice>) : State()
        data class Error(val message: String) : State()
    }



    //viewmodel 정보 flow로 정보 전달 및 상태정보 전달
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> get() = _state

    //okhttp 정보
    private val client = OkHttpClient()

    // 오피넷 정보 가져오는 동작
    fun fetchOilPrices() {
        viewModelScope.launch(Dispatchers.IO) {

            _state.value = State.Loading // intent에 현재 상태 정보 전달

            try {
                delay(1000) // 로딩 화면

                val api_key = BuildConfig.API_KEY  // apikey hide


                //post 방식으로 전달
                val formBody = FormBody.Builder()
                    .add("code", api_key) // 여기에 실제 API 키를 입력하세요.
                    .add("out", "json")
                    .build()
                                /*
                //get 방식으로 데이터 전달
                val request = Request.Builder()
                    .url("https://www.opinet.co.kr/api/avgAllPrice.do?code="+api_key+"&out=json")
                    .get()
                    .build()
                    */


                val request = Request.Builder()  //post 방식으로 데이터 전달
                    .url("https://www.opinet.co.kr/api/avgAllPrice.do?")
                    .post(formBody)
                    .build()


//정상적으로 수신시
                client.newCall(request).execute().use { response ->

                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val responseBody = response.body?.string()
                   // Log.d("test","responsebody ${responseBody}")  /// 정상적으로 수신

                    val oilPriceResponse = Gson().fromJson(responseBody, OilPriceResponse::class.java)
                   // Log.d("test","oilPriceResponse ${oilPriceResponse}")


                    //정상 수신시 INTENT 값이 성공인 경우
                    _state.value = State.Success(oilPriceResponse.result.oil)
                   // Log.d("test","_state.value ${_state.value}")
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Unknown error")
                //Log.d("test","_state.value err ${_state.value}")
            }
        }
    }
}

// RecyclerView Adapter
class OilPriceAdapter(private val oilPrices: List<OilPrice>) :
    RecyclerView.Adapter<OilPriceAdapter.OilPriceViewHolder>() {

    class OilPriceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tradeDate: TextView = view.findViewById(R.id.tradeDate)
        val productCode: TextView = view.findViewById(R.id.productCode)
        val price: TextView = view.findViewById(R.id.price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OilPriceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_oil_price, parent, false)
        return OilPriceViewHolder(view)
    }

    override fun onBindViewHolder(holder: OilPriceViewHolder, position: Int) {
        val oilPrice = oilPrices[position]
        holder.tradeDate.text = oilPrice.tradeDate
        holder.productCode.text = oilPrice.productCode
        holder.price.text = oilPrice.price
    }

    override fun getItemCount() = oilPrices.size
}




// Main Activity
class MainActivity : AppCompatActivity() {

    private val viewModel: OilPriceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progr :ProgressBar =  findViewById(R.id.progressBar)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is OilPriceViewModel.State.Loading -> {
                        recyclerView.visibility =View.GONE
                        progr.visibility = View.VISIBLE
                        // Show loading state (e.g., ProgressBar visibility)
                    }
                    is OilPriceViewModel.State.Success -> {
                        recyclerView.visibility =View.VISIBLE
                        progr.visibility = View.GONE
                        recyclerView.adapter = OilPriceAdapter(state.oilPrices)
                    }
                    is OilPriceViewModel.State.Error -> {
                        // Show error message (e.g., Toast or TextView)
                    }
                }
            }
        }

        viewModel.fetchOilPrices()
/*        viewModel.state.observe(this) { state ->
            when (state) {
                is OilPriceViewModel.State.Loading -> {
                    // Show loading state
                }
                is OilPriceViewModel.State.Success -> {
                    recyclerView.adapter = OilPriceAdapter(state.oilPrices)
                }
                is OilPriceViewModel.State.Error -> {
                    // Show error message
                }
            }
        }

        viewModel.fetchOilPrices()*/
    }
}
