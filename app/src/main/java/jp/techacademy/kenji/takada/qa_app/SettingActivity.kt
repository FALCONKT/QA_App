package jp.techacademy.kenji.takada.qa_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {

    // DatabaseReference　import要
    private lateinit var mDataBaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)


        // Preferenceから表示名を取得してEditTextに反映させる
        // PreferenceManager import要
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        nameText.setText(name)

        // FirebaseDatabase　import要
        mDataBaseReference = FirebaseDatabase.getInstance().reference


        // UIの初期設定
        title = getString(R.string.settings_titile)

        // changeButton　import要
        // 表示名変更Button  OnClickListener

        // Loginしているかどうかを確認し、もしLogInしていなければSnackBarでその旨を表示して、
        // その後は何もしません。LogInしていればFirebaseに表示名を保存し、Preferenceに保存
        changeButton.setOnClickListener{v ->
            // KeyBoardが出ていたら閉じる
            // InputMethodManager　import要
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)


            // LogIn済みのUserを取得する
            // FirebaseAuth　import要
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // LogInしていない場合は何もしない
                // Snackbar　import要
                Snackbar.make(v, getString(R.string.no_login_user), Snackbar.LENGTH_LONG).show()
            } else {
                // 変更した表示名をFirebaseに保存する
                val name2 = nameText.text.toString()
                val userRef = mDataBaseReference.child(UsersPATH).child(user.uid)
                val data = HashMap<String, String>()
                data["name"] = name2
                userRef.setValue(data)

                // 変更した表示名をPreferenceに保存する
                val sp2 = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = sp2.edit()
                editor.putString(NameKEY, name)
                editor.commit()

                Snackbar.make(v, getString(R.string.change_disp_name), Snackbar.LENGTH_LONG).show()
            }
        }


        // LogOutButton OnClickListener
        // LogOutButton　の　OnClickListene
        // LogOutはFirebaseAuthClassのsignOutMethodを使用。
        // signOutMethodを使用したた後はPreferenceに空文字（”“)を保存し、
        // SnackbarでLogOut完了の旨を表示
        logoutButton.setOnClickListener { v ->
            FirebaseAuth.getInstance().signOut()
            nameText.setText("")
            Snackbar.make(v, getString(R.string.logout_complete_message), Snackbar.LENGTH_LONG).show()
        }


    }
    //    onCreate() END

}
//Class END