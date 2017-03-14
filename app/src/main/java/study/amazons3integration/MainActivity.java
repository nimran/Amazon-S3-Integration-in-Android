package study.amazons3integration;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.InputStream;

import study.amazons3integration.aws.S3Uploader;
import study.amazons3integration.aws.S3Utils;

public class MainActivity extends AppCompatActivity {

    ImageView ivSelectedImage;
    TextView tvStatus;

    private final int SELECT_PICTURE = 2;
    Uri imageUri;
    S3Uploader s3uploaderObj;
    String urlFromS3 = null;
    ProgressDialog progressDialog;
    private String TAG = MainActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        s3uploaderObj = new S3Uploader(this);
        progressDialog = new ProgressDialog(this);

    }

    private void bindViews() {
        ivSelectedImage = (ImageView)findViewById(R.id.image_selected);
        tvStatus = (TextView)findViewById(R.id.status);
        findViewById(R.id.choose_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStoragePermissionGranted();
            }
        });
        findViewById(R.id.upload_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageUri != null) {
                    uploadImageTos3(imageUri);
                }
            }
        });

    }


    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                chooseImage();
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            chooseImage();
            return true;
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseImage();
            Log.e(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
        } else {
            Log.e(TAG, "Please click again and select allow to choose profile picture");
        }
    }

    public void OnPictureSelect(Intent data) {
        imageUri = data.getData();
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (imageStream != null) {
            ivSelectedImage.setImageBitmap(BitmapFactory.decodeStream(imageStream));
        }
    }

    private String getFilePathfromURI(Uri selectedImageUri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(selectedImageUri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;

    }

    private void uploadImageTos3(Uri imageUri) {
        final String path = getFilePathfromURI(imageUri);
        if (path != null) {
            showLoading("Uploading details !!");
            s3uploaderObj.initUpload(path);
            s3uploaderObj.setOns3UploadDone(new S3Uploader.S3UploadInterface() {
                @Override
                public void onUploadSuccess(String response) {
                    if (response.equalsIgnoreCase("Success")) {
                        hideLoading();
                        urlFromS3 = S3Utils.generates3ShareUrl(getApplicationContext(), path);
                        if(!TextUtils.isEmpty(urlFromS3)) {
                            tvStatus.setText("Uploaded : "+urlFromS3);
                        }
                    }
                }

                @Override
                public void onUploadError(String response) {
                    hideLoading();
                    tvStatus.setText("Error : "+response);
                    Log.e(TAG, "Error Uploading");

                }
            });
        }
    }

    private void showLoading(String message) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK) {
                OnPictureSelect(data);
            }
        }
    }

}
