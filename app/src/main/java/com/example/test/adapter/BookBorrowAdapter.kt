package com.example.test.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.databinding.BooklistBinding
import com.example.test.entity.ListAppealEntity
import com.example.test.globalContexts.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class BookBorrowAdapter(private val context: Context, private val books: List<ListAppealEntity>) : RecyclerView.Adapter<BookBorrowAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = BooklistBinding.bind(view)
    }

    var queue = Volley.newRequestQueue(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.booklist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val book = books[position]
        holder.binding.blBkTitle.text = book.name

        holder.binding.blBkStatus.background = null
        holder.binding.blBkStatus.text = formatDate(book.expected_completion_date)



        holder.binding.returnDateTxt.visibility = View.VISIBLE

        holder.binding.tvBlAuthor.text = book.author
        holder.binding.tvBlOwner.text = book.owner

        Log.d("hi bor", "onBindViewHolder: " + book.thumbnail)
        Log.d("hi bor", "onBindViewHolder: " + book.name)
        Log.d("hi bor", "onBindViewHolder: " + book.status)

        Log.d(
            "BookBorrowAdapter",
            "Displayed book status at position $position: ${holder.binding.blBkStatus.text}"
        )

        Glide.with(context)
            .load(book.thumbnail)
            .into(holder.binding.imageView)

        if (book.status == "AVAILABLE") {
            if (book.trans_status == "PENDING") {
                holder.itemView.setOnClickListener(View.OnClickListener {
                    Toast.makeText(
                        context,
                        "Book Borrow approval by Owner Pending",
                        Toast.LENGTH_SHORT
                    ).show()
                })
                val statusTextView = holder.binding.blBkStatus
                val status = book.trans_status
                updateButtonColor(status, statusTextView)
            } else if (book.trans_status == "REJECTED") {
                holder.itemView.setOnClickListener(View.OnClickListener {
                    Toast.makeText(
                        context,
                        "Book Borrow approval by Owner Rejected",
                        Toast.LENGTH_SHORT
                    ).show()
                })
                val statusTextView = holder.binding.blBkStatus
                val status = book.trans_status
                updateButtonColor(status, statusTextView)
            } else if (book.trans_status == "COMPLETED") {
                holder.itemView.setOnClickListener(View.OnClickListener {
                    Toast.makeText(context, "Book Borrow ", Toast.LENGTH_SHORT).show()
                })
                val statusTextView = holder.binding.blBkStatus
                val status = book.trans_status
                updateButtonColor(status, statusTextView)
            }
        } else {
            holder.itemView.setOnClickListener {
                showCustomDialog(book)
            }
            val statusTextView = holder.binding.blBkStatus
            val status = book.trans_status
            updateButtonColor(status, statusTextView)
        }
    }

    private fun showCustomDialog(appeal: ListAppealEntity) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.bottomsheet_conformation, null)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(dialogView)

        val tvTitleDialogBox: TextView = dialogView.findViewById(R.id.tvTitleDialogBox)
        val tvBorrowDateText: TextView = dialogView.findViewById(R.id.tvBorrowDateText)
        val btnCancel: MaterialButton = dialogView.findViewById(R.id.btnCancel)
        val btnConfirm: MaterialButton = dialogView.findViewById(R.id.btnConfirm)

        tvTitleDialogBox.text = "Return the Borrowed Book"
        tvBorrowDateText.text = "Return the book now?"

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            returnBook(appeal)
            dialog.dismiss()
        }

        dialog.show()
    }

    public fun returnBook(appeal: ListAppealEntity) {
        val url = "${Constants.BASE_URL}/v0/appeals/${appeal.trans_id}"
        val newStatus = JSONObject().apply {
            put("trans_status", "COMPLETED")
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.PATCH, url, newStatus,
            { response ->
                Log.d("APPEAL UPDATE", "APPEAL UPDATED")
                Toast.makeText(context, "Book returned successfully", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.d("APPEAL UPDATE", "ERROR OCCURED")
            }
        )
        queue.add(jsonObjectRequest)

    }

    fun updateButtonColor(status: String, button: TextView) {
        Log.d("hi v", "updateButtonColor: " + status)

        when (status) {
            "PENDING" -> button.backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(0)),
                intArrayOf(context.resources.getColor(R.color.pending_yellow))
            )

            "REJECTED" -> button.backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(0)),
                intArrayOf(context.resources.getColor(R.color.rejected_red))
            )

            "COMPLETED" -> button.backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(0)),
                intArrayOf(context.resources.getColor(R.color.completed_green))
            )

            else -> button.backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(0)),
                intArrayOf(context.resources.getColor(R.color.zolo_bluey))
            )
        }
    }

    override fun getItemCount() = books.size



    public fun formatDate(inputDate: String): String {
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

    public  fun ordinal(number: Int): String {
        return when {
            number in 11..13 -> "${number}th"
            number % 10 == 1 -> "${number}st"
            number % 10 == 2 -> "${number}nd"
            number % 10 == 3 -> "${number}rd"
            else -> "${number}th"
        }
    }
}
