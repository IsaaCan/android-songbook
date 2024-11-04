package mwongela.songbook.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.custom.share.parseSongFromUri
import mwongela.songbook.info.logger.LoggerFactory


class SharedSongOpenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val encodedSong = parseSongFromUri(intent)
        LoggerFactory.logger.info("opening encoded shared song: $encodedSong")

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra("encodedSong", encodedSong)
        startActivity(intent)
        finish()
    }

}
