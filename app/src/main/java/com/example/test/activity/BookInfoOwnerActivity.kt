package com.example.test.activity

import BorrowerListAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager

import com.android.volley.toolbox.JsonArrayRequest
import com.bumptech.glide.Glide
import com.example.test.databinding.ActivityBookInfoOwnerBinding
import com.example.test.entity.BorrowerEntity
import com.example.test.globalContexts.Constants
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.example.test.R
import com.example.test.databinding.FragmentTabYourbooksBinding
import com.facebook.shimmer.ShimmerFrameLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class BookInfoOwnerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookInfoOwnerBinding
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var borrowerAdapter: BorrowerListAdapter
    private lateinit var mainLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(this.getResources().getColor(R.color.zolo_bg_main));
        }

        binding = ActivityBookInfoOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shimmerFrameLayout = binding.shimmerInfoView
        mainLayout = binding.topPart

        mainLayout.visibility = View.GONE
        shimmerFrameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.startShimmer()


        // Retrieve the book information from the intent
        val bookId = intent.getIntExtra("bookId", 0)
        val bookName = intent.getStringExtra("bookName")
        val bookStatus = intent.getStringExtra("bookStatus")
        val transStatus = intent.getStringExtra("transStatus")
        val bookThumbnail = intent.getStringExtra("bookThumbnail")
        val bookAuthor = intent.getStringExtra("bookAuthor")
        val bookOwner = intent.getStringExtra("bookOwner")


        // Display the book information in the views
        binding.tvBookName.text = bookName
        binding.tvBookOwner.text = bookOwner
        binding.textAuthor.text = bookAuthor
        Glide.with(this)
            .load(bookThumbnail)
            .into(binding.coverSmol)
        Glide.with(this)
            .load(bookThumbnail)
            .into(binding.coverBig)






///////for Borrower recycler view

        val recyclerView = binding.borrowerRecyclerView
        var queue = Volley.newRequestQueue(this)
        val url = "${Constants.BASE_URL}/v0/appeals?book=${bookId}"

        var count = 0;




        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                val borrowers = ArrayList<BorrowerEntity>()
                for (i in 0 until response.length()) {
                    val appealObject = response.getJSONObject(i)
                    val trans_status = appealObject.getString("trans_status")
                    val status = appealObject.getJSONObject("bookId").getString("status")

                    if (transStatus == "PENDING") {


                        val borrowerObject = appealObject.getJSONObject("borrowerId")
                        val borrowerId = borrowerObject.getInt("id")
                        val borrowerName = borrowerObject.getString("name")
                        val inDate = appealObject.getString("initiation_date")
                        val retDate = appealObject.getString("expected_completion_date")
                        val transId = appealObject.getInt("trans_id")

                        val borrower = BorrowerEntity(
                            id = borrowerId,
                            name = borrowerName,
                            initiationDate = inDate,
                            completionDate = retDate,
                            bkStatus = status,
                            bkTransStatus = trans_status,
                            trans_id = transId
                        )
                        count++
                        borrowers.add(borrower)


                        borrowers.sortBy { it.name }
                    }
                }

                if (count ==0 && bookStatus == "AVAILABLE"){
                    recyclerView.visibility = View.GONE
                    binding.textView2.text = "No Requests on this Book"
                }
                if (bookStatus=="DELISTED"){
                    recyclerView.visibility = View.GONE
                    binding.textView2.text = "This Book is Currently Delisted"
                }


                val adapter = BorrowerListAdapter(this,borrowers)
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter=adapter

                shimmerFrameLayout.visibility = View.GONE
                shimmerFrameLayout.stopShimmer()
                mainLayout.visibility = View.VISIBLE

                Log.d("borrowers data", "onCreate: $borrowers")
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonArrayRequest)


    }


    fun formatDate(inputDate: String): String {
        // Parse the input date string
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val dateTime = LocalDateTime.parse(inputDate, formatter)

        // Extract day, month, and year from the parsed date
        val day = dateTime.dayOfMonth
        val month = dateTime.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val year = dateTime.year

        // Create the formatted string
        val formattedDate = "${ordinal(day)} $month, $year"

        return formattedDate
    }

    fun ordinal(number: Int): String {
        return when {
            number in 11..13 -> "${number}th"
            number % 10 == 1 -> "${number}st"
            number % 10 == 2 -> "${number}nd"
            number % 10 == 3 -> "${number}rd"
            else -> "${number}th"
        }
    }


}