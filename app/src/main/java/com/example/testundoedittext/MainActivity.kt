package com.example.testundoedittext

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var mEditText: UndoRedoEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mEditText = findViewById(R.id.editor)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_undo -> {
                mEditText.undo()
                true
            }
            R.id.action_redo -> {
                mEditText.redo()
                true
            }
            R.id.action_clear -> {
                mEditText.clearHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
