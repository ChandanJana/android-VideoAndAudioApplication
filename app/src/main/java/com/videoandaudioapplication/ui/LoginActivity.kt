package com.videoandaudioapplication.ui;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.videoandaudioapplication.R
import com.videoandaudioapplication.databinding.ActivityLoginBinding
import com.videoandaudioapplication.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var views: ActivityLoginBinding

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        views.apply {
            btn.setOnClickListener {
                Log.d("TAGG", "init: click")
                mainRepository.login(
                    usernameEt.text.toString(), passwordEt.text.toString()
                ) { isDone, reason ->
                    Log.d("TAGG", "init: isDone $isDone")
                    if (!isDone) {
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    } else {
                        //start moving to our main activity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("username", usernameEt.text.toString())
                        })
                    }
                }
            }
        }
    }
}