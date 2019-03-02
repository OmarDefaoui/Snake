package com.nordef.snake.fragment

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.nordef.snake.MainActivity
import com.nordef.snake.R

class Home : Fragment() {

    internal lateinit var view: View
    internal lateinit var context: Context

    internal lateinit var tv_best_score: TextView
    internal lateinit var btn_new_game: Button
    internal lateinit var btn_share: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        container?.removeAllViews()
        view = inflater.inflate(R.layout.fragment_home, container, false)
        context = inflater.context

        tv_best_score = view.findViewById(R.id.tv_best_score)
        btn_new_game = view.findViewById(R.id.btn_new_game)
        btn_share = view.findViewById(R.id.btn_share)

        val sharedPref = context.getSharedPreferences("high_score", Context.MODE_PRIVATE)
        val hightScore = sharedPref.getInt("highScore", 0)
        tv_best_score.text = "${getString(R.string.your_best_score_is)} $hightScore ${getString(R.string.`in`)} ${sharedPref.getString("duration", "00:00")}"

        btn_new_game.setOnClickListener {
            (context as MainActivity).moveToFragmentGame()
        }

        btn_share.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val shareSubText = getString(R.string.share_title)
            val shareBodyText = getString(R.string.share_link)
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareSubText + "\n\n" + shareBodyText)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
        }

        return view
    }

}