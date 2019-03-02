package com.nordef.snake

import android.app.Fragment
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.nordef.snake.fragment.Game
import com.nordef.snake.fragment.Lose


class MainActivity : AppCompatActivity() {

    internal var fragment: Fragment? = null
    internal var fragmentManager: FragmentManager? = null
    internal var fragmentTransaction: FragmentTransaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("high_score", Context.MODE_PRIVATE)
        hightScore = sharedPref.getInt("highScore", 0)
        duration = sharedPref.getString("duration", "00:00")
    }

    fun moveToFragmentGame() {
        fragment = Game()
        fragmentManager = getFragmentManager()
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction!!.replace(R.id.fragment, fragment)
        fragmentTransaction!!.commit()
    }

    fun moveToFragmentLose(score: Int, duration: String) {
        fragment = Lose()
        fragmentManager = getFragmentManager()
        fragmentTransaction = fragmentManager!!.beginTransaction()
        val bundle = Bundle()
        bundle.putInt("score", score)
        bundle.putString("duration", duration)
        fragment!!.arguments = bundle
        fragmentTransaction!!.replace(R.id.fragment, fragment)
        fragmentTransaction!!.commit()
    }

    companion object {
        var hightScore = 0
        var duration = ""
    }

    data class coordinate(var x: Float, var y: Float)
}
