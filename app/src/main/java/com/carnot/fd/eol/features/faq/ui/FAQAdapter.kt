package com.carnot.fd.eol.features.faq.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.carnot.fd.eol.R
import com.carnot.fd.eol.databinding.ItemFaqBinding


class FAQAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var questionsList = mutableListOf<String>()
    private var questionsMap = mutableMapOf<String, List<String>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return FAQViewHolder(
            ItemFaqBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val channel = questionsList[position]
        val faqViewHolder = holder as FAQViewHolder
        faqViewHolder.bind(channel)
    }

    override fun getItemCount(): Int {
        return questionsList.size
    }

    inner class FAQViewHolder(val binding: ItemFaqBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.tvHeader.text = item

            var answer = ""

            for ((i, ans) in questionsMap[item]!!.withIndex()) {
                answer = answer + "" + (i + 1) + ". " + ans + "\n"
            }

            binding.tvAnswers.text = answer

            if (binding.layoutAnswers.isVisible)
                binding.ivIcon.setBackgroundResource(com.google.android.material.R.drawable.mtrl_ic_cancel)
            else
                binding.ivIcon.setBackgroundResource(R.drawable.baseline_arrow_forward_ios_24)

            binding.layoutHeader.setOnClickListener {
                if (binding.layoutAnswers.isVisible) {
                    binding.ivIcon.setBackgroundResource(R.drawable.baseline_arrow_forward_ios_24)
                    binding.layoutAnswers.visibility = View.GONE
                } else {
                    binding.ivIcon.setBackgroundResource(com.chuckerteam.chucker.R.drawable.mtrl_ic_cancel)
                    binding.layoutAnswers.visibility = View.VISIBLE
                }

            }
        }
    }

    fun submitList(list: MutableList<String>, responseData: Map<String, List<String>>?) {
        val diffCallback = FAQDiffCallback(questionsList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        questionsList.clear()
        questionsList.addAll(list)

        questionsMap.clear()
        questionsMap = responseData as MutableMap<String, List<String>>

        diffResult.dispatchUpdatesTo(this)
        notifyDataSetChanged()
    }

    internal class FAQDiffCallback(
        private val oldList: List<String>,
        private val newList: List<String>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}