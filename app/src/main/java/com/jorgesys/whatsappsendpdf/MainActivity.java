package com.jorgesys.whatsappsendpdf;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WhatsAppSendPDF";
    private static final String DIRECTORY = "Download";
    private static final int REQUEST_CODE  = 112;
    private static String fileName = "";
    private static String urlFile = "";
    private static String phoneNumber  = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnSendPDF).setOnClickListener(view -> {

            EditText editTextUrlPdf = findViewById(R.id.editTextUrlPdf);
            urlFile = editTextUrlPdf.getText().toString();
            EditText editTextPhone = findViewById(R.id.editTextPhone);
            phoneNumber = editTextPhone.getText().toString();

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE);
                try {
                    URL url = new URL(urlFile);
                    String urlPath = url.getPath();
                    fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
                    //Log.i(TAG, "Download file un path: " + Environment.getExternalStorageDirectory() + File.separator + fileName);
                    Log.i(TAG, "Download file un path: " + getExternalFilesDir(null) + File.separator + fileName);
                    new DownloadFileFromURL().execute(urlFile);
                } catch (MalformedURLException e) {
                 Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    private void sendPDFtoWhatsApp() {
        try {
            //Deprecated use of Environment.getExternalStorageDirectory().
            //File file = new File(Environment.getExternalStorageDirectory() +  File.separator +  DIRECTORY+ File.separator  + fileName);
            File file = new File(getExternalFilesDir(null), File.separator + fileName);  //add <external-path name="files" path="files"/>
            if (fileExists(file)) {
                Uri uriFile = FileProvider.getUriForFile(this, getPackageName(), file); //add <external-path name="files" path="files"/>
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_STREAM, uriFile);
                //intent.setComponent(new ComponentName("com.whatsapp","com.whatsapp.Conversation"));//Not necessary
                //Phone number must contain area code without  sign.
                intent.putExtra("jid", phoneNumber.replace("+","") + "@s.whatsapp.net"); //numero telefonico sin prefijo "+"!
                intent.setPackage("com.whatsapp");
                startActivity(intent);
            }else{
                Toast.makeText(getApplicationContext(), "File doesn´t exist!", Toast.LENGTH_LONG).show();
            }
            } catch(android.content.ActivityNotFoundException ex){
                Log.e(TAG, "Whatsapp is not installed." + ex.getMessage());
                Toast.makeText(getApplicationContext(), "Whatsapp is not installed.", Toast.LENGTH_LONG).show();
            }

    }

    private boolean fileExists(File file){
        if(file.exists()){
            Log.i(TAG, "* File exists! " + file.getAbsolutePath());
            return true;
        }else {
            Log.e(TAG, "* File doesn´t exists! " + file.getAbsolutePath());
            return false;
        }
    }

    private ProgressDialog pDialog;
    public static final int progress_bar_type = 0;


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        //Before starting background thread Show Progress Bar Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        //Downloading file in background thread
        @Override
        protected String doInBackground(String... urls) {
            int count;
            try {
                URL url = new URL(urls[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // this will be useful so that you can show a tipical 0-100% progress bar.
                int lenghtOfFile = connection.getContentLength();
                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),8192);
                // Output stream
                //Deprecated use of Environment.getExternalStorageDirectory()
                //OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() +  File.separator +  DIRECTORY+ File.separator  + fileName);
                OutputStream output = new FileOutputStream(getExternalFilesDir(null) + File.separator + fileName);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    //Publishing the progress....
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                    //Writing data to file
                    output.write(data, 0, count);
                }
                //Flushing output
                output.flush();
                //Closing streams
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e(TAG,"DownloadFileFromURL doInBackground(): "+ e.getMessage());
            }
            return null;
        }

        //Updating progress bar
        protected void onProgressUpdate(String... progress) {
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        //After completing background task Dismiss the progress dialog
        @Override
        protected void onPostExecute(String file_url) {
            //Dismiss the dialog after the file was completely downloaded
            dismissDialog(progress_bar_type);
            //Send file
            sendPDFtoWhatsApp();
        }

    }


}
