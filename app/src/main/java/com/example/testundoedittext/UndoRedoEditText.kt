package com.example.testundoedittext

import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.Selection
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import java.util.*

class UndoRedoEditText(context: Context, attrs: AttributeSet) :
    AppCompatEditText(context, attrs) {

    private var mUndoRedoHelper: UndoRedoHelper

    init {
        mUndoRedoHelper = UndoRedoHelper(this)
    }

    open fun undo() {
        if (mUndoRedoHelper.canUndo) {
            try {
                mUndoRedoHelper.undo()
            } catch (e: Exception) {
            }
        }
    }

    open fun redo() {
        if (mUndoRedoHelper.canRedo) {
            try {
                mUndoRedoHelper.redo()
            } catch (e: Exception) {
            }
        }
    }

    open fun setOrderInitHtml(order: Boolean) {
        mUndoRedoHelper.setOrderInitHtml(order)
    }

    open fun clearHistory() {
        mUndoRedoHelper.clearHistory()
    }

    /*
    *  Copyright (c) 2017 Tran Le Duy
    *
    * Licensed under the Apache License, Version 2.0 (the "License");
    * you may not use this file except in compliance with the License.
    * You may obtain a copy of the License at
    *
    *     http://www.apache.org/licenses/LICENSE-2.0
    *
    * Unless required by applicable law or agreed to in writing, software
    * distributed under the License is distributed on an "AS IS" BASIS,
    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    * See the License for the specific language governing permissions and
    * limitations under the License.
    */

    /**
     * Translated to kotlin from java
     * original source from https://github.com/tranleduy2000/javaide/blob/master/app/src/main/java/com/duy/ide/editor/code/view/UndoRedoSupportEditText.java
     * @see UndoRedoHelper
     */

    class UndoRedoHelper(private val mTextView: TextView?) {
        private var mIsUndoOrRedo = false

        private val mEditHistory: EditHistory

        private val mChangeListener: EditTextChangeListener

        val canUndo: Boolean
            get() = (mEditHistory.mmPosition > 0)

        val canRedo: Boolean
            get() = (mEditHistory.mmPosition < mEditHistory.mmHistory.size)

        fun setOrderInitHtml(order: Boolean) {
            if (mEditHistory.mmHistory.size > 0) {
                mEditHistory.mmHistory[0].order = order
            }
        }

        init {
            mEditHistory = EditHistory()
            mChangeListener = EditTextChangeListener()
            mTextView!!.addTextChangedListener(mChangeListener)
        }

        fun disconnect() {
            mTextView?.removeTextChangedListener(mChangeListener)
        }

        fun setMaxHistorySize(maxHistorySize: Int) {
            mEditHistory.setMaxHistorySize(maxHistorySize)
        }

        fun clearHistory() {
            mEditHistory.clear()
        }

        private fun undoText(from: Int, to: Int, s: String) {
            val list = mutableListOf<EditItem>()
            val editOrder = mEditHistory.mmHistory[from]
            if (editOrder.after.length > editOrder.before.length) {
                editOrder.apply {
                    after = s
                    order = true
                }
                for (i in to downTo from) {
                    val edit = mEditHistory.mmHistory[i]
                    if (edit.start != editOrder.start || edit.after != editOrder.after) {
                        list.add(edit)
                    }
                }
            } else {
                editOrder.apply {
                    before = s
                    order = true
                    start -= (before.length - 1)
                }
                for (i in to downTo from) {
                    val edit = mEditHistory.mmHistory[i]
                    if (edit.start != editOrder.start || edit.before != editOrder.before) {
                        list.add(edit)
                    }
                }
            }

            mEditHistory.mmHistory.removeAll(list)
            mEditHistory.mmPosition = mEditHistory.mmHistory.size
            undo()
        }

        private fun standardizedList() {
            val list = mutableListOf<EditItem>()
            for (i in mEditHistory.mmHistory.indices) {
                val edit = mEditHistory.mmHistory[i]
                if (!edit.order) {
                    when {
                        edit.after.length > edit.before.length -> {
                            val s = edit.after.toString().substring(edit.before.length)
                            edit.apply {
                                after = s
                                start += before.length
                                before = ""
                            }
                        }
                        edit.after.length < edit.before.length -> {
                            val s = edit.before.toString().substring(edit.after.length)
                            edit.apply {
                                before = s
                                start += after.length
                                after = ""
                            }
                        }
                        else -> {
                            list.add(edit)
                        }
                    }
                }
            }

            mEditHistory.mmHistory.removeAll(list)

            standardizedList2()
        }

        private fun standardizedList2() {
            val listTemHistory = mutableListOf<EditItem>()
            val list = mutableListOf<EditItem>()
            listTemHistory.addAll(mEditHistory.mmHistory)
            var i = 0
            while (i < listTemHistory.size) {
                val edit = listTemHistory[i]
                val listFilter = mutableListOf<EditItem>()
                if (i + 1 >= listTemHistory.size) break
                var nextEdit = listTemHistory[i + 1]
                if (nextEdit.start == edit.start && !edit.order && !nextEdit.order) {
                    listFilter.add(edit)
                    listFilter.add(nextEdit)
                    i++

                    for (j in (i + 2) until listTemHistory.size) {
                        nextEdit = listTemHistory[j]
                        if (nextEdit.start == edit.start && !nextEdit.order) {
                            listFilter.add(nextEdit)
                            i = j
                        } else {
                            break
                        }
                    }
                    if (listFilter.size > 1) {
                        val firstItem = listFilter[0]
                        val lastItem = listFilter[listFilter.size - 1]
                        if (lastItem.after.length > lastItem.before.length) {
                            if (lastItem.after != firstItem.before) {
                                list.addAll(listFilter)
                            }

                        } else {
                            if (lastItem.before != firstItem.after) {
                                list.addAll(listFilter)
                            }
                        }
                    }
                    listFilter.removeAll(list)
                    mEditHistory.mmHistory.removeAll(listFilter)
                }
                i++
            }
        }

        fun undo() {
            standardizedList()
            var sTemp = ""
            for (i in (mEditHistory.mmHistory.size - 1) downTo 0) {
                val s = mEditHistory.mmHistory[i]
                if (!s.order) {
                    if (s.after.length > s.before.length) {
                        sTemp = sTemp.plus(s.after.toString())
                        when (s.after.toString()) {
                            ">" -> {
                                var sTemp1Tag = ""
                                for (j in (i - 1) downTo 0) {
                                    val s1 = mEditHistory.mmHistory[j]
                                    if (!s1.order
                                        && s1.after.length > s1.before.length
                                    ) {
                                        val s1Temp = s1.after.toString()
                                        sTemp = sTemp.plus(s1Temp)
                                        if (s1.start + s1Temp.length + sTemp1Tag.length == s.start) {
                                            sTemp1Tag = sTemp1Tag.plus(s1Temp)
                                            when (s1Temp) {
                                                ">" -> {
                                                    var sTempTag = ""
                                                    for (k in (j - 1) downTo 0) {
                                                        val s2 = mEditHistory.mmHistory[k]
                                                        val s2Temp = s2.after.toString()
                                                        if (!s2.order
                                                            && s2.after.length > s2.before.length
                                                            && s2.start + s2Temp.length + sTempTag.length == s1.start
                                                        ) {
                                                            if (s2Temp == ">") break
                                                            if (s2Temp == "<") {
                                                                sTempTag = sTempTag.reversed()
                                                                if (!TextUtils.isEmpty(sTempTag)) {
                                                                    sTemp = sTemp.reversed()
                                                                    sTemp = sTemp.substring(
                                                                        1,
                                                                        sTemp.length
                                                                    )
                                                                    undoText(
                                                                        j + 1,
                                                                        j + sTemp.length,
                                                                        sTemp
                                                                    )
                                                                    return
                                                                }
                                                            } else {
                                                                sTempTag = sTempTag.plus(s2Temp)
                                                            }
                                                        } else {
                                                            sTemp = sTemp.reversed()
                                                            undoText(j, j + sTemp.length - 1, sTemp)
                                                            return
                                                        }
                                                    }
                                                }
                                                "<" -> {
                                                    var s3Temp = ""
                                                    for (k in i - 1 downTo j + 1) {
                                                        val s3 = mEditHistory.mmHistory[k]
                                                        s3Temp = s3Temp.plus(s3.after.toString())
                                                    }
                                                    s3Temp = s3Temp.reversed()
                                                    if (s3Temp.isNotEmpty() && !s3Temp.contains("/")) {
                                                        sTemp = sTemp.reversed()
                                                        undoText(j, i, sTemp)
                                                        return
                                                    }
                                                }
                                                "\n", " " -> {
                                                    sTemp = sTemp.reversed()
                                                    undoText(j, i, sTemp)
                                                    return
                                                }
                                            }
                                        } else {
                                            sTemp = sTemp.reversed()
                                            sTemp = sTemp.substring(1, sTemp.length)
                                            undoText(j + 1, j + sTemp.length, sTemp)
                                        }
                                    } else {
                                        sTemp = sTemp.reversed()
                                        undoText(j + 1, j + sTemp.length, sTemp)
                                        return
                                    }

                                }
                            }
                            "\n", " " -> {
                                sTemp = sTemp.reversed()
                                undoText(i, i + sTemp.length - 1, sTemp)
                                return
                            }
                            else -> {
                                val s5 = mEditHistory.mmHistory[i - 1]
                                val s5Temp = s5.after.toString()
                                if (!s5.order
                                    && s5.after.length > s5.before.length
                                    && s5.start + s5Temp.length == s.start
                                ) {
                                    if (s5Temp == ">") {
                                        var sTempTag = ""
                                        for (j in (i - 2) downTo 0) {
                                            val s6 = mEditHistory.mmHistory[j]
                                            val s6Temp = s6.after.toString()
                                            if (!s6.order
                                                && s6.after.length > s6.before.length
                                                && s6.start + s6Temp.length + sTempTag.length == s5.start
                                            ) {
                                                if (s6Temp == "<") {
                                                    sTempTag = sTempTag.reversed()
                                                    if (!TextUtils.isEmpty(sTempTag)) {
                                                        sTemp = sTemp.reversed()
                                                        undoText(i, i + sTemp.length - 1, sTemp)
                                                        return
                                                    }
                                                } else {
                                                    sTempTag = sTempTag.plus(s6Temp)
                                                }
                                            } else {
                                                sTemp = sTemp.plus(s5Temp)
                                                sTemp = sTemp.reversed()
                                                undoText(i - 1, i + sTemp.length - 2, sTemp)
                                                return
                                            }
                                        }
                                    }
                                } else {
                                    sTemp = sTemp.reversed()
                                    undoText(i, i + sTemp.length - 1, sTemp)
                                    return
                                }
                            }
                        }
                    } else {
                        sTemp = sTemp.plus(s.before.toString())
                        val s5 = mEditHistory.mmHistory[i - 1]
                        if (!s5.order) {
                            if (s5.after.length > s5.before.length) {
                                undoText(i, i + sTemp.length - 1, sTemp)
                                return
                            }
                        } else {
                            undoText(i, i + sTemp.length - 1, sTemp)
                            return
                        }
                    }
                }
            }


            val edit = mEditHistory.previous ?: return

            val editable = mTextView!!.editableText
            val start = edit.start
            val end = start + if (edit!!.after != null) edit.after.length else 0

            mIsUndoOrRedo = true
            editable.replace(start, end, edit.before)
            mIsUndoOrRedo = false

            for (o in editable.getSpans(0, editable.length, UnderlineSpan::class.java)) {
                editable.removeSpan(o)
            }

            Selection.setSelection(
                editable,
                if (edit.before == null) start else start + edit.before.length
            )
        }

        fun redo() {
            val edit = mEditHistory.next ?: return

            val text = mTextView!!.editableText
            val start = edit.start
            val end = start + if (edit!!.before != null) edit.before.length else 0

            mIsUndoOrRedo = true
            text.replace(start, end, edit.after)
            mIsUndoOrRedo = false

            // This will get rid of underlines inserted when editor tries to come
            // up with a suggestion.
            for (o in text.getSpans(0, text.length, UnderlineSpan::class.java)) {
                text.removeSpan(o)
            }

            Selection.setSelection(
                text, if (edit.after == null)
                    start
                else
                    start + edit.after.length
            )
        }

        fun storePersistentState(editor: SharedPreferences.Editor, prefix: String) {
            // Store hash code of text in the editor so that we can check if the
            // editor contents has changed.
            editor.putString(prefix + ".hash", prefix.hashCode().toString())
            editor.putInt(prefix + ".maxSize", mEditHistory.mmMaxHistorySize)
            editor.putInt(prefix + ".position", mEditHistory.mmPosition)
            editor.putInt(prefix + ".size", mEditHistory.mmHistory.size)

            var i = 0
            for (ei in mEditHistory.mmHistory) {
                val pre = prefix + "." + i

                editor.putInt(pre + ".start", ei.start)
                editor.putString(pre + ".before", ei.before.toString())
                editor.putString(pre + ".after", ei.after.toString())

                i++
            }
        }

        @Throws(IllegalStateException::class)
        fun restorePersistentState(sp: SharedPreferences, prefix: String): Boolean {
            val ok = doRestorePersistentState(sp, prefix)
            if (!ok) {
                mEditHistory.clear()
            }
            return ok
        }

        private fun doRestorePersistentState(sp: SharedPreferences, prefix: String): Boolean {
            val hash = sp.getString(prefix + ".hash", null) ?: // No state to be restored.
            return true

            if (Integer.valueOf(hash) != prefix.hashCode()) {
                return false
            }

            mEditHistory.clear()
            mEditHistory.mmMaxHistorySize = sp.getInt(prefix + ".maxSize", -1)

            val count = sp.getInt(prefix + ".size", -1)
            if (count == -1) {
                return false
            }

            for (i in 0 until count) {
                val pre = prefix + "." + i

                val start = sp.getInt(pre + ".start", -1)
                val before = sp.getString(pre + ".before", null)
                val after = sp.getString(pre + ".after", null)

                if (start == -1 || before == null || after == null) {
                    return false
                }
                mEditHistory.add(EditItem(start, before, after, false))
            }

            mEditHistory.mmPosition = sp.getInt(prefix + ".position", -1)
            return mEditHistory.mmPosition != -1

        }

        // =================================================================== //

        internal enum class ActionType {
            INSERT, DELETE, PASTE, NOT_DEF
        }

        private inner class EditHistory {
            val mmHistory = LinkedList<EditItem>()
            var mmPosition = 0
            var mmMaxHistorySize = -1

            val current: EditItem?
                get() = if (mmPosition == 0) {
                    null
                } else mmHistory.get(mmPosition - 1)

            val previous: EditItem?
                get() {
                    if (mmPosition == 0) {
                        return null
                    }
                    mmPosition--
                    return mmHistory.get(mmPosition)
                }

            val next: EditItem?
                get() {
                    if (mmPosition >= mmHistory.size) {
                        return null
                    }

                    val item = mmHistory.get(mmPosition)
                    mmPosition++
                    return item
                }

            fun clear() {
                mmPosition = 0
                mmHistory.clear()
            }

            fun add(item: EditItem) {
                while (mmHistory.size > mmPosition) {
                    mmHistory.removeLast()
                }
                mmHistory.add(item)
                mmPosition++

                if (mmMaxHistorySize >= 0) {
                    trimHistory()
                }
            }

            fun setMaxHistorySize(maxHistorySize: Int) {
                mmMaxHistorySize = maxHistorySize
                if (mmMaxHistorySize >= 0) {
                    trimHistory()
                }
            }

            fun trimHistory() {
                while (mmHistory.size > mmMaxHistorySize) {
                    mmHistory.removeFirst()
                    mmPosition--
                }

                if (mmPosition < 0) {
                    mmPosition = 0
                }
            }
        }

        private data class EditItem(
            var start: Int,
            var before: CharSequence,
            var after: CharSequence,
            var order: Boolean
        )

        private inner class EditTextChangeListener : TextWatcher {
            private var mBeforeChange: CharSequence? = null
            private var mAfterChange: CharSequence? = null
            private var lastActionType = ActionType.NOT_DEF
            private var lastActionTime: Long = 0

            private val actionType: ActionType
                get() {
                    return if (!TextUtils.isEmpty(mBeforeChange) && TextUtils.isEmpty(mAfterChange)) {
                        ActionType.DELETE
                    } else if (TextUtils.isEmpty(mBeforeChange) && !TextUtils.isEmpty(mAfterChange)) {
                        ActionType.INSERT
                    } else {
                        ActionType.PASTE
                    }
                }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                if (mIsUndoOrRedo) {
                    return
                }
                mBeforeChange = s.subSequence(start, start + count)
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (mIsUndoOrRedo) {
                    return
                }
                mAfterChange = s.subSequence(start, start + count)
                makeBatch(start)
            }

            private fun makeBatch(start: Int) {
                val at = actionType
                val editItem = mEditHistory.current
                if (lastActionType != at || ActionType.PASTE == at || System.currentTimeMillis() - lastActionTime > 10 || editItem == null) {
                    Log.d("Trung", mAfterChange.toString())
                    mEditHistory.add(EditItem(start, mBeforeChange!!, mAfterChange!!, false))
                } else {
                    if (at == ActionType.DELETE) {
                        editItem.start = start
                        editItem.before = (mBeforeChange!!).toString() + editItem.before.toString()
                    } else {
                        editItem.after = (editItem.after).toString() + mAfterChange!!.toString()
                    }
                }
                lastActionType = at
                lastActionTime = System.currentTimeMillis()
            }

            override fun afterTextChanged(s: Editable) {}
        }
    }
}