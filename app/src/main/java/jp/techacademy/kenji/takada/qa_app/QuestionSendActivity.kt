package jp.techacademy.kenji.takada.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {

//    PermissionのDaialogとIntent連携からActivityに戻ってきた時に識別するための定数、

//  他の数値でも問題　無し
//    それぞれこのActivity内で複数のPermitionの許可のDualogを出すことがある場合、
//    複数のActivityから戻ってくることがある場合に識別するための値だが、
//    質問投稿画面ではそれぞれ1種類だけで本来識別する必要もないので何かしらの値が入っていれば問題
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    // Genruを保持するPeroperty、
    private var mGenre: Int = 0
    // Cameraで撮影した画像を保存するURIを保持するPropertyを定義
    private var mPictureUri: Uri? = null


//    onCreateMethodではIntentで渡ってきたGenru番号を取り出してmGenreで保持
//    そしてTitleの設定と、Listenerの設定を行う。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // 渡ってきたGenruの番号を保持する
        val extras = intent.extras
        mGenre = extras!!.getInt("genre")

        // UIの準備
        title = getString(R.string.question_send_title)

        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }


    //    Intent連携から戻ってきた時に画像を取得し、ImageViewに設定する
    //    dataがnullかdata.getData()の場合はCameraで撮影したときなので画像の取得にmPictureUriを使用する

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmapを取得する
            // ContentResolverClass、InputStreamClass、BitmapFactoryClassを使ってURIからBitmapを作成
            // こういう作成手法と捉えておく
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) {
                return
            }

            // 取得したBimapの長辺を500pxにResizeする
            // 取得したBitmapからResizeして新たなBitmapを作成し、ImageViewに設定
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) // (1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }


    // onClickMethodでは添付画像を選択・表示するImageViewをTapした時と、
    // 投稿ButtonをTapした時の処理

    // Android6.0以降か否かで処理を分ける。
    //6.0以降であればcheckSelfPermissionMethodで
    // 外部Strageへの書き込みが許可されているか確認
    override fun onClick(v: View) {
        if (v === imageView) {
            // パーミッションの許可状態を確認する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている
                    showChooser()
                } else {
                    // 許可されていないので許可Dialogを表示する
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)

                    return
                }
            } else {
                showChooser()
            }
        } else if (v === sendButton) {

            // 投稿ボタンがタップされた時はKeyBoradを閉じ、
            // Titleと本文が入力されていることを確認した上で、投稿するDataを用意してFirebaseに保存

            // KeyBoradが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())

            val data = HashMap<String, String>()

            // UID
            data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文を取得する
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            if (title.isEmpty()) {
                // タイトルが入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_title), Snackbar.LENGTH_LONG).show()
                return
            }

            if (body.isEmpty()) {
                // 質問が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.question_message), Snackbar.LENGTH_LONG).show()
                return
            }

            // Preferenceから名前を取る
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY, "")

            data["title"] = title
            data["body"] = body
            data["name"] = name!!

            // 添付画像を取得する
            // 画像を取得する部分で as?
            // 安全なCast演算子 というもので、Castに失敗したら null を返す。
            // 画像が設定されていない場合にCastしようとするとApplicationが落ちるため、
            //  as? を使って、画像がないときは null を返すようにしている。
            val drawable = imageView.drawable as? BitmapDrawable

            // 画像はBASE64EncodeというDataを文字列に変換する仕組みを使って文字列に.
            // Firebaseは文字列や数字しか保存出来ないが、こうすることで画像をFirebaseに保存可能となる！！

            // 添付画像が設定されていれば画像を取り出してBASE64エンコードする
            if (drawable != null) {
                val bitmap = drawable.bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                data["image"] = bitmapString
            }

            // 保存する際はDatabaseReferenceClassのsetValueを使用している
            // 今回は第2引数も指定。第2引数にはCompletionListenerClassを指定
            //（今回はActivityがCompletionListenerClassを実装している）。
            // 保存するのに時間を要することが予想されるのでCompletionListenerClassで完了を受け取るように
            // this　がそれ
            genreRef.push().setValue(data, this)
            progressBar.visibility = View.VISIBLE
        }
    }


    // onRequestPermissionsResult MEthos は
    // 許可DiaLigでUserが選択した結果を受け取る

    // if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
    // とすることで 許可したかどうかを判断。
    // 許可された場合はshowChooserMethosを使用する。
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Userが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    // showChooser　Methos　Garellyから選択するIntentとCameraで撮影するIntentを作成して、
    // 更にそれらを選択するIntentを作成してDiaLogを表示
    private fun showChooser() {
        // Garellyから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        //　Cameraで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // Garelly選択のIntentを与えてcreateChooserMethodを使用する
        // IntentClassのcreateChooserMethodの第1引数に1つ目のIntentを指定し、
        // 第2引数にはDuaLogへ表示するTitleを指定
        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        // EXTRA_INITIAL_INTENTSにCamera撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        // そのIntentに対し、2つのIntent
        // chooserIntent
        // と
        // chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        // を渡す

        // ！！！！！！！！　これで　画像の遷移がなされる　2つ目がIntent
        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    //    Firebaseへの保存が完了したらfinishMethodを呼び出してActivityを閉じます。
    //    もし失敗した場合はSnackbarでErrorの旨を表示
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.question_send_error_message), Snackbar.LENGTH_LONG).show()
        }
    }
}