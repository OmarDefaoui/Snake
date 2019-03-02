package com.nordef.snake.fragment

import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import com.nordef.snake.MainActivity
import com.nordef.snake.MainActivity.coordinate
import com.nordef.snake.R
import com.nordef.snake.custom.OnSwipeTouchListener
import java.util.Random
import kotlin.collections.ArrayList


class Game : Fragment() {

    internal lateinit var view: View
    internal lateinit var context: Context

    internal lateinit var relativeLayout: RelativeLayout
    internal lateinit var main_rl: RelativeLayout
    internal lateinit var iv_pause: ImageView
    internal lateinit var tv_score: TextView
    internal lateinit var chronometer: Chronometer

    var arrayCoordinate = ArrayList<coordinate>()
    var swipeDirection = "R"
    var squareSize = 40

    var timeWhenStopped = 0L
    var isResume = false
    var isPause = false
    var isLose = false

    var width = 0
    var height = 0
    var positionX = 0f
    var positionY = 0f
    var foodPositionX = 0f
    var foodPositionY = 0f
    var difWidth = 0
    var difHeight = 0
    var scorePoint = -1
    var delay = 200L //milliseconds
    var num = ArrayList<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        container?.removeAllViews()
        view = inflater.inflate(R.layout.fragment_game, container, false)
        context = inflater.context

        relativeLayout = view.findViewById(R.id.relativeLayout)
        main_rl = view.findViewById(R.id.main_rl)
        iv_pause = view.findViewById(R.id.iv_pause)
        tv_score = view.findViewById(R.id.tv_score)
        chronometer = view.findViewById(R.id.chronometer)

        //to get the saved delay (speed)
        delay = context.getSharedPreferences("high_score", Context.MODE_PRIVATE).getLong("delay", 200)
        load_num() //num to diplay in spinner

        iv_pause.setOnClickListener {
            pauseChrono()
            isPause = true

            val builder = AlertDialog.Builder(context)
            builder.setCancelable(false)
            val builderView = (context as MainActivity).layoutInflater.inflate(R.layout.custom_alert_dialog, null)
            builder.setView(builderView)
            val dialog = builder.create()
            dialog.show()

            builderView.findViewById<TextView>(R.id.tv_score).text = "${getString(R.string.your_current_score_is)} $scorePoint"
            val tv_current_speed = builderView.findViewById<TextView>(R.id.tv_current_speed)
            tv_current_speed.text = "${getString(R.string.current_speed_is)} $delay"
            val spinner = builderView.findViewById<Spinner>(R.id.spinner)

            val adapter = ArrayAdapter<Int>(context, android.R.layout.simple_spinner_dropdown_item, num)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.setSelection(adapter.getPosition(delay.toInt()))

            builderView.findViewById<Button>(R.id.btn_resume).setOnClickListener {
                delay = spinner.selectedItem.toString().toLong()
                val editor = context.getSharedPreferences("high_score", Context.MODE_PRIVATE).edit()
                editor.putLong("delay", delay)
                editor.commit()

                dialog.dismiss()
                isResume = true
                isPause = false
                createSnake()
            }
            builderView.findViewById<Button>(R.id.btn_main).setOnClickListener {
                dialog.dismiss()
                pauseChrono()
                isLose = true
                (context as MainActivity).moveToFragmentLose(scorePoint, chronometer.text.toString())
            }
            builderView.findViewById<Button>(R.id.btn_change_speed).setOnClickListener {
                delay = spinner.selectedItem.toString().toLong()
                tv_current_speed.text = "${getString(R.string.current_speed_is)} $delay"
                val editor = context.getSharedPreferences("high_score", Context.MODE_PRIVATE).edit()
                editor.putLong("delay", delay)
                editor.commit()

                dialog.dismiss()
                isResume = true
                isPause = false
                createSnake()
            }
        }

        main_rl.setOnTouchListener(object : OnSwipeTouchListener() {
            override fun onSwipeTop(): Boolean {
                swipeTop()
                return true
            }

            override fun onSwipeRight(): Boolean {
                swipeRight()
                return true
            }

            override fun onSwipeLeft(): Boolean {
                swipeLeft()
                return true
            }

            override fun onSwipeBottom(): Boolean {
                swipeBottom()
                return true
            }
        })

        main_rl.setOnClickListener { }

        getScreenSize()

        return view
    }

    private fun swipeTop() {
        if (swipeDirection.equals("B") || swipeDirection.equals("T"))
            return
        swipeDirection = "T"
    }

    private fun swipeBottom() {
        if (swipeDirection.equals("T") || swipeDirection.equals("B"))
            return
        swipeDirection = "B"
    }

    private fun swipeLeft() {
        if (swipeDirection.equals("R") || swipeDirection.equals("L"))
            return
        swipeDirection = "L"
    }

    private fun swipeRight() {
        if (swipeDirection.equals("L") || swipeDirection.equals("R"))
            return
        swipeDirection = "R"
    }

    private fun createSnake() {

        if (!isResume) {
            //start with position in center
            positionX = (width / 2).toFloat() - squareSize
            positionY = (height / 2).toFloat()

            //check if can eat the food
            if (positionX.toInt() % squareSize != 0) {
                while ((positionX.toInt() % squareSize) != 0) {

                    val reste = positionX % squareSize
                    positionX = positionX - reste
                }
            }

            if (positionY.toInt() % squareSize != 0) {
                while ((positionY.toInt() % squareSize) != 0) {

                    val reste = positionY % squareSize
                    positionY = positionY - reste
                }
            }

            for (i in 0..2) {
                // create iv programmmatically
                val imageView = ImageView(context)
                imageView.setBackgroundColor(resources.getColor(R.color.colorBody))
                imageView.layoutParams = RelativeLayout.LayoutParams(squareSize, squareSize)

                if (i == 2)
                    imageView.setBackgroundColor(resources.getColor(R.color.colorHead))

                positionX += squareSize
                positionY += 0
                imageView.x = positionX
                imageView.y = positionY

                // Adds the view to the layout
                arrayCoordinate.add(coordinate(positionX, positionY))
                relativeLayout.addView(imageView)
            }

            //display food
            displayFood()
        }

        resumeChrono()
        val handler = Handler()

        // to repeat code every 1 milliseconde
        handler.postDelayed(object : Runnable {
            override fun run() {

                if (isPause)
                    return

                //number of iv
                var i = arrayCoordinate.size - 1
                val posX = relativeLayout.getChildAt(i).x
                val posY = relativeLayout.getChildAt(i).y

                Log.d("tage", "$posX, $posY")

                arrayCoordinate.set(i, coordinate(relativeLayout.getChildAt(i).x, relativeLayout.getChildAt(i).y))
                //check if crash with his body
                if (checkCrash(i)) {
                    loadLoseScreen()
                    return
                }

                when (swipeDirection) {
                    "R" -> {
                        //check if capted food
                        if ((posX + squareSize == foodPositionX) && (posY == foodPositionY)) {
                            Log.d("tage", "right")
                            i += 1
                            foodIsCapted(i)
                        }
                        relativeLayout.getChildAt(i).x += squareSize
                    }
                    "L" -> {
                        if ((posX - squareSize == foodPositionX) && (posY == foodPositionY)) {
                            Log.d("tage", "left")
                            i += 1
                            foodIsCapted(i)
                        }
                        relativeLayout.getChildAt(i).x -= squareSize
                    }
                    "T" -> {
                        if ((posY - squareSize == foodPositionY) && (posX == foodPositionX)) {
                            Log.d("tage", "top")
                            i += 1
                            foodIsCapted(i)
                        }
                        relativeLayout.getChildAt(i).y -= squareSize
                    }
                    "B" -> {
                        if ((posY + squareSize == foodPositionY) && (posX == foodPositionX)) {
                            Log.d("tage", "bottom")
                            i += 1
                            foodIsCapted(i)
                        }
                        relativeLayout.getChildAt(i).y += squareSize
                    }
                }

                //make element follow the next one
                for (i in 0..(arrayCoordinate.size - 2)) {
                    arrayCoordinate.set(i, coordinate(arrayCoordinate[i + 1].x, arrayCoordinate[i + 1].y))
                    relativeLayout.getChildAt(i).x = arrayCoordinate[i].x
                    relativeLayout.getChildAt(i).y = arrayCoordinate[i].y
                }

                //if the head iv arrived to the x screen border
                checkBorder(i)

                handler.postDelayed(this, delay)
            }
        }, delay)


    }

    private fun foodIsCapted(i: Int) {
        relativeLayout.getChildAt(i).setBackgroundColor(resources.getColor(R.color.colorHead))
        relativeLayout.getChildAt(i - 1).setBackgroundColor(resources.getColor(R.color.colorBody))

        arrayCoordinate.set(i - 1, coordinate(relativeLayout.getChildAt(i - 1).x,
                relativeLayout.getChildAt(i - 1).y))

        arrayCoordinate.add(coordinate(foodPositionX, foodPositionY))
        displayFood()
        arrayCoordinate.set(i, coordinate(relativeLayout.getChildAt(i).x, relativeLayout.getChildAt(i).y))
    }

    private fun loadLoseScreen() {
        isLose = true
        pauseChrono()
        Thread.sleep(1000)
        (context as MainActivity).moveToFragmentLose(scorePoint, chronometer.text.toString())
    }

    private fun checkCrash(i: Int): Boolean {
        for (x in 0..(i - 4)) {
            if (arrayCoordinate[i].equals(arrayCoordinate[x]))
                return true
        }
        return false
    }

    private fun checkBorder(i: Int) {

        //if the last iv appear i the new place replace the group with the old one to kep the ids
        if (relativeLayout.getChildAt(i).x >= width) {
            arrayCoordinate.set(i, coordinate(0f, relativeLayout.getChildAt(i).y))
            relativeLayout.getChildAt(i).x = 0f

        } else if (relativeLayout.getChildAt(i).x <= -squareSize) {
            arrayCoordinate.set(i, coordinate(width - squareSize.toFloat(), relativeLayout.getChildAt(i).y))
            relativeLayout.getChildAt(i).x = width.toFloat() - squareSize.toFloat()

        } else if (relativeLayout.getChildAt(i).y >= height) {
            arrayCoordinate.set(i, coordinate(relativeLayout.getChildAt(i).x, 0f))
            relativeLayout.getChildAt(i).y = 0f

        } else if (relativeLayout.getChildAt(i).y <= -squareSize) {
            arrayCoordinate.set(i, coordinate(relativeLayout.getChildAt(i).x, height - squareSize.toFloat()))
            relativeLayout.getChildAt(i).y = height.toFloat() - squareSize.toFloat()

        }
    }

    private fun displayFood() {
        scorePoint++
        tv_score.text = "${getString(R.string.score)} $scorePoint"

        //to create a random food
        foodPositionX = (0..width).random().toFloat()
        foodPositionY = (0..height).random().toFloat()

        //check if we can eat the food
        if (foodPositionX.toInt() % squareSize != 0) {
            while ((foodPositionX.toInt() % squareSize) != 0) {

                val reste = foodPositionX % squareSize
                foodPositionX = foodPositionX - reste
            }
        }

        if (foodPositionY.toInt() % squareSize != 0) {
            while ((foodPositionY.toInt() % squareSize) != 0) {

                val reste = foodPositionY % squareSize
                foodPositionY = foodPositionY - reste
            }
        }

        //here add while to repeat get random if pos is full
        if (arrayCoordinate.contains(coordinate(foodPositionX, foodPositionY))) {
            while (arrayCoordinate.contains(coordinate(foodPositionX, foodPositionY))) {

                //to create a random food
                foodPositionX = (0..width).random().toFloat()
                foodPositionY = (0..height).random().toFloat()

                //check if we can eat the food
                if (foodPositionX.toInt() % squareSize != 0) {
                    while ((foodPositionX.toInt() % squareSize) != 0) {

                        val reste = foodPositionX % squareSize
                        foodPositionX = foodPositionX - reste
                    }
                }

                if (foodPositionY.toInt() % squareSize != 0) {
                    while ((foodPositionY.toInt() % squareSize) != 0) {

                        val reste = foodPositionY % squareSize
                        foodPositionY = foodPositionY - reste
                    }
                }

            }
        }

        //create the food image
        val imageView = ImageView(context)
        imageView.setBackgroundColor(resources.getColor(R.color.colorFood))
        imageView.layoutParams = RelativeLayout.LayoutParams(squareSize, squareSize)

        imageView.x = foodPositionX
        imageView.y = foodPositionY
        Log.e("tage", "food x : $foodPositionX, y: $foodPositionY")

        // Adds the food to the layout
        relativeLayout.addView(imageView)
    }

    // will return an `Int` between 0 and 10 (incl.) //ex: (0..10).random()
    fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) + start

    private fun getScreenSize() {
        relativeLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val availableHeight = relativeLayout.measuredHeight
                if (availableHeight > 0) {
                    relativeLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)

                    //save height here and do whatever you want with it
                    width = relativeLayout.width
                    height = relativeLayout.height

                    squareSize = height / squareSize
                    Log.d("tage", "snake size: $squareSize")
                    height -= dpToPx(squareSize)

                    if (width % squareSize != 0) {
                        Log.d("tage", "wifth before: $width")
                        while ((width % squareSize) != 0) {

                            val reste = width % squareSize
                            width = width - reste
                            Log.d("tage", "after: $width")
                        }
                    }

                    if (height % squareSize != 0) {
                        Log.d("tage", "height before: $height")
                        while ((height % squareSize) != 0) {

                            val reste = height % squareSize
                            height = height - reste
                            Log.d("tage", "after: $height")
                        }
                    }

                    difWidth = relativeLayout.width - width
                    difHeight = relativeLayout.height - height
                    Log.d("tage", "dx: $difWidth, dy: $difHeight")

                    val params = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                    params.setMargins(0, difHeight, difWidth, 0)
                    relativeLayout.setLayoutParams(params)

                    createSnake()
                }
            }
        })
    }

    fun dpToPx(dp: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    private fun pauseChrono() {
        timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
        chronometer.stop()
    }

    private fun resumeChrono() {
        chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
        chronometer.start()
    }

    override fun onPause() {
        super.onPause()
        if (!isLose)
            iv_pause.callOnClick()
    }

    private fun load_num() {
        for (i in 1..9) {
            num.add(i)

            if (i == 9) {
                for (i in 1..9)
                    num.add(i * 10)
                for (i in 1..10)
                    num.add(i * 100)
            }
        }
    }

}