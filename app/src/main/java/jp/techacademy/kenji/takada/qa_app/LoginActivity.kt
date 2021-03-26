package jp.techacademy.kenji.takada.qa_app

import android.content.Context
import android.icu.text.CaseMap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.DropBoxManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*
import java.lang.reflect.Method

class LoginActivity : AppCompatActivity() {

//  各種Property値の設定　Field変数定義
//  FirebaseAuth  import 要
//  Firebase関連　　処理の完了を受け取るLitener(Account作成処理　LogIn処理用)  DBの読み書き
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDataBaseReference: DatabaseReference

    // Account作成時にFlagを立て、LogIn処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // DBへのRefarence取得
        // FirebaseDatabase　import
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuthのObjectを取得する
        mAuth = FirebaseAuth.getInstance()



        //Account 作成処理の Listener

        // FirebaseのAccount作成処理はOnCompleteListenerClassで受け取り
        // onCompleteMethodをOverrideする
        mCreateAccountListener = OnCompleteListener { task ->
            // 引数に入ってきた　isSuccessfulMethodを使って、成功したかどうかを確認
            if (task.isSuccessful) {

                // 成功した場合
                // LogInを行う
                // emailText import要
                // login Method を使用している
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)
            } else {

                // 失敗した場合
                // Errorを表示する
                // View　import要　androi.viewの方
                val view = findViewById<View>(android.R.id.content)

                // Snackbar　import要
                // Snackbarで　Errorの旨を表示
                Snackbar.make(
                    view,
                    getString(R.string.create_account_failure_message),
                    Snackbar.LENGTH_LONG
                ).show()

                // ProgresBarを非表示にする
                progressBar.visibility = View.GONE
            }
       }
       //Account 作成処理の Listener END


        //LogIn 処理のListener
        //FirebaseのLogIn処理もOnCompleteListenerClassで受け取り
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                val user = mAuth.currentUser
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)

//                mIsCreateAccount を使って
//                Account作成Buttonを押してからのLogIn処理か、
//                LogInButton Tap の場合かで処理
                if (mIsCreateAccount) {
                    // Account作成の時は表示名をFirebaseに保存する
                    // Account作成Buttonを押した場合は表示名をFirebaseとPreferenceに保存

                    // Firebaseは、DataをKeyとValueの組み合わせで保存
                    val name = nameText.text.toString()
                    val data = HashMap<String, String>()
                    data["name"] = name
                    // Valueを保存するには setValue Methodを使用
                    userRef.setValue(data)

                    // 表示名をPreferenceに保存する
                    // Firebaseから表示名を取得してPreferenceに保存
                    saveName(name)
                } else {
                    // ValueEventListener　DataSnapshot　と　DatabaseError　を　importして初めて使用可能

                    // FirebaseからDataを一度だけ取得する場合
                    // DatabaseReferenceClassが実装している
                    // QueryClassのaddListenerForSingleValueEvent Methodを使用する
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        // DataSnapshot　import要
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            saveName(data!!["name"] as String)
                        }

                        // DatabaseError　import要
                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // ProgresBarを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                // finish() Methodで LoginActivity を閉じる
                finish()

            } else {
                // 失敗した場合
                // Errorを表示する
                // LogINに失敗した場合は、SnackbarでErrorの旨を表示し、
                // 処理中に表示していたDiaLogを非表示に
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG)
                    .show()

                // Progeress Bar を非表示にする
                progressBar.visibility = View.GONE
            }
        }
        // Log In 処理のListener END



        // TitleのTitleを変更
        // Titleの設定
        title = getString(R.string.login_title)



        // Acount作成Buttonの　OnClickListenerを設定
        // InputMethodManager の hideSoftInputFromWindow Method　を呼び出して
        // KeyBoardを閉じ、LogIn時に表示名を保存するように
        // createAccountMethodを呼び出してAccount作成処理を開始
        // mIsCreateAccountにtrueを設定
        createButton.setOnClickListener { v ->
            // KeyBoardが出てたら閉じる
            //  Context　import要　android.content.Content の方
            //  InputMethodManager　import要
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // LogIn時に表示名を保存するようにFlagを立てる
                mIsCreateAccount = true

                createAccount(email, password)
            } else {
                // Errorを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG)
                    .show()
            }
        }
        // createButton.setOnClickListener　END


        // LogIn Buttonの　OnClickListenerを設定
        //LogInButtonをTapした時には同様にKeyBoardを閉じ
        //loginMethodを呼び出してLogIn処理を開始
        loginButton.setOnClickListener { v ->
            // KeyBoardが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // Flagを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // Errorを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG)
                    .show()
            }
        }
        // loginButton.setOnClickListener　END

    }

    //Acount作成を行うcreateAccountMethod　ProgresBarを表示させ
    //Firebase AuthClass　の　createUserWithEmailAndPassword Method　で　Account作成

    //createUserWithEmailAndPasswordMethodの引数にはmailAdrass、passwordを与え、
    //更にaddOnCompleteListenerMethodを呼び出してListenerを設定
    private fun createAccount(email: String, password: String) {
        // ProgresBarを表示する
        progressBar.visibility = View.VISIBLE

        // Accountを作成する
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }


    // LogIn処理を行うloginMethod　ProgressBarを表示させ
    // Firebase　AuthClassのsignInWithEmailAndPasswordMethodでLogIn処理

    // signInWithEmailAndPasswordMethodの引数にはmailAdrass、passwordを与え、
    // 更にaddOnCompleteListenerMethodを呼び出してListenerを設定
    private fun login(email: String, password: String) {
        // ProgresBarを表示する
        progressBar.visibility = View.VISIBLE
        // LogInする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    //saveNameMethod 引数で受け取った表示名をPreferenceに保存
    // 忘れずにcommitMethodを呼び出して保存処理を反映
    private fun saveName(name: String) {
        // Preferenceに保存する
        // PreferenceManager　import要　　
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()
    }




}
//Class END
