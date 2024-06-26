package com.example.test.activity

import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

import android.widget.ScrollView

import android.widget.RelativeLayout

import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.databinding.ActivityBookInfoBinding
import com.example.test.databinding.ActivityMainBinding
import com.example.test.entity.BooksDetailsEntity
import com.example.test.globalContexts.Constants
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import org.json.JSONObject
import java.time.LocalDate
import java.util.Date

class BookInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookInfoBinding


    private lateinit var shimmerFrameLayout: ShimmerFrameLayout

    private lateinit var mainLayout: ScrollView

    private var book: BooksDetailsEntity = BooksDetailsEntity(
        id = -1,
        name = "",
        description = "",
        owner = "",
        thumbnail = ""
    )
    private var id: Int? = null
    private var name: String? = ""
    private var description: String? = ""
    private var thumbnail: String? = ""
    private var author: String? = ""
    private var owner_id: Int? = 0
    private var owner: String? = ""
    private var book_available_till: Date? = null
    var maxBorrow = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(this.getResources().getColor(R.color.zolo_bg_main));
        }

        binding = ActivityBookInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backButton = binding.tvbackToHome
        backButton.setOnClickListener {
            finish()
        }

       shimmerFrameLayout = binding.shimmerInfoView
        mainLayout = binding.scrollView2

        val bookId = intent.getIntExtra("bookId", 0)
        if (bookId == 0) {
            finish()
            return
        } else {
            fetchBookDetails(bookId )
        }

        id = intent.getIntExtra("id", 3)
        name = intent.getStringExtra("name")
        description = intent.getStringExtra("description")
        thumbnail = intent.getStringExtra("thumbnail")
        author = intent.getStringExtra("author")
        owner = intent.getStringExtra("owner")

//        if()

        with(binding) {
            tvBookName.text = name
            tvDescription.text = description
            textAuthor.text = author
            tvBookOwner.text = owner
            binding.bBorrowBook.setOnClickListener {
                showCustomDialog(bookId)
            }
        }



    }

    private fun showCustomDialog(bookId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.bottomsheet_datepicker, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        val tvBorrowDateText: TextView = dialogView.findViewById(R.id.tvBorrowDateText)
        val btnAdd: TextView = dialogView.findViewById(R.id.btnAdd)
        val btnSub: TextView = dialogView.findViewById(R.id.btnSub)

        val btnConfirm: MaterialButton = dialogView.findViewById(R.id.btnConfirmRequest)

        getMaxNumberOfDays(bookId, tvBorrowDateText, btnAdd)

        btnSub.setOnClickListener {
            decrementDays(tvBorrowDateText)
        }

        btnConfirm.setOnClickListener {
            borrowDataToDatabase(bookId, tvBorrowDateText.text.toString().toInt())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getMaxNumberOfDays(bookId: Int, tvBorrowDateText: TextView, btnAdd: TextView) {
        val url = "${Constants.BASE_URL}/v0/books/$bookId"
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("BookInfoActivity", "Response: $response")
                maxBorrow = response.getInt("maxBorrow")
                Log.d("BookInfoActivity", "Max borrow days for book $bookId: $maxBorrow")
                btnAdd.setOnClickListener {
                    incrementDays(tvBorrowDateText, maxBorrow)
                }
            },
            { error ->
                Log.e("BookInfoActivity", "Error fetching book details: $error")
            }
        )
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
    private fun incrementDays(tvBorrowDateText: TextView,max:Int) {
        var currentDays = tvBorrowDateText.text.toString().toInt()
        if(currentDays<max){
            tvBorrowDateText.text = (currentDays + 1).toString()
        }
        Log.d("noofgdaysTAG", "incrementDays: "+tvBorrowDateText.text)
    }
    private fun decrementDays(tvBorrowDateText: TextView) {
        val currentDays = tvBorrowDateText.text.toString().toInt()
        if (currentDays > 1) {
            tvBorrowDateText.text = (currentDays - 1).toString()
        }
    }



    private fun borrowDataToDatabase(bookId: Int, count: Int) {
        val url = "https://api-zolo.onrender.com/v0/appeals"
        val localDate = LocalDate.now()
        val addedDate = localDate.plusDays(count.toLong())
        val completionDate = addedDate.toString()
        val jsonBody = JSONObject().apply {
            put("bookId", bookId)
            put("borrowerId", Constants.USER_ID)
            put("initiation_date", localDate.toString())
            put("expected_completion_date", completionDate)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                // Handle successful response
                Toast.makeText(this, "Book requested from owner", Toast.LENGTH_SHORT).show()
                Log.d("BookInfoActivity", "Book borrowed successfully. Response: $response")
                // Update UI or perform further actions if needed
                finish()
            },
            { error ->
                // Handle errors
                Toast.makeText(this, "Book requested from owner", Toast.LENGTH_SHORT).show()
                Log.e("BookInfoActivity", "Error borrowing book: $error")
                Toast.makeText(this, "Book requested from owner", Toast.LENGTH_SHORT).show()
                
                // Update UI or perform further actions if needed
                finish()
                // Display error message or perform other error handling steps
                finish()
            }
        )

        // Adding the request to the RequestQueue
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }



    private fun fetchBookDetails(bookId: Int ) {
        mainLayout.visibility = View.GONE
        shimmerFrameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.startShimmer()

        val url = "${Constants.BASE_URL}/v0/books/$bookId"
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("BookInfoActivity", "API Response: $response")

                //
                // Using optString instead of getString
                val name = response.getString("name")
                val description = response.getString("description")
                val author = response.optString("author")
                val owner = response.getJSONObject("owner")
                val thumbnail = response.getString("thumbnail")

                val coverBig = findViewById<ImageView>(R.id.cover_big)
                val coverSmall = findViewById<ImageView>(R.id.cover_smol)

                val requestBookBtn: MaterialButton = findViewById(R.id.bBorrowBook)

                if (owner.getInt("id") == Constants.USER_ID) {
                    requestBookBtn.visibility = View.GONE
                }

                Glide.with(this)
                    .load(thumbnail)
                    .into(coverBig)

                Glide.with(this)
                    .load(thumbnail)
                    .into(coverSmall)

                shimmerFrameLayout.stopShimmer()
                binding.tvBookName.text = name
                binding.tvDescription.text = description
                binding.textAuthor.text = author
                binding.tvBookOwner.text = owner.getString("name")
                checkPreviousRequest(bookId);
                shimmerFrameLayout.visibility = View.GONE
                shimmerFrameLayout.stopShimmer()
                mainLayout.visibility = View.VISIBLE
            },
            { error ->
                Log.e("BookInfoActivity", "Error fetching book details: $error")
            })
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
    fun checkPreviousRequest(bookId: Int) {
        val url = "${Constants.BASE_URL}/v0/appeals"
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("BookInfoActivity", "Response: $response")
                for (j in 0 until response.length()){
                    val appealObject = response.getJSONObject(j)
                    val book = appealObject.getJSONObject("bookId")
                    val bookIdFromAppeal = book.getInt("id")
                    if (bookIdFromAppeal == bookId) {
                        val borrower = appealObject.getJSONObject("borrowerId")
                        val borrowerId = borrower.getInt("id")
                        if (borrowerId == Constants.USER_ID) {
                            Log.d("BookInfoActivity", "Setting visibility to GONE")
                            binding.bBorrowBook.visibility = View.GONE
                        }
                    }
                }
            },
            { error ->
                Log.e("BookInfoActivity", "Error fetching appeals: $error")
            }
        )
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

}
