package com.example.testundoedittext

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var mEditText: UndoRedoEditText
    val html = "<p>sasdasd</p>\n" +
            "<p>asdasdasd</p>\n" +
            "<p>asdasd</p>"

//    val html = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mEditText = findViewById(R.id.editor)
        mEditText.setIsUndo(true)
        mEditText.setText(html)
        mEditText.htmlString = html
        mEditText.setPosStartUndo(html.length)
        mEditText.setIsUndo(false)
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

    override fun onDestroy() {
        super.onDestroy()
        mEditText.clearHistory()
    }
}
