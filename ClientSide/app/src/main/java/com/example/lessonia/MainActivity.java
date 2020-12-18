package com.example.lessonia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    // Defining folder variables
    ArrayList<File> allFilePaths = new ArrayList<>();
    String folderName = "LessonAudio/";
    File folder = new File(Environment.getExternalStorageDirectory(), folderName);
    // Defining networking variables
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    String baseURL = "http://10.0.2.2:5000/";
    // Defining GUI variables
    ListView listView;

    // Defining permission codes
    private static final int RequestPermissionCode = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // If all permission is are NOT enabled
        if(!CheckingPermissionIsEnabledOrNot()){
            RequestMultiplePermission(); //Calling method to enable permission
        }else {
            //Set up data structure
            if (!folder.exists()) {
                folder.mkdirs();
            }

            //region  for manually creating files
//            try {
//                new File(Environment.getExternalStorageDirectory()+"/"+folderName, "ann.mp3").createNewFile(); // already created
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            // todo delete files before recording
//            new File(Environment.getExternalStorageDirectory()+"/"+folderName, "ann.mp3").delete(); // already created
                // endregion

            allFilePaths.addAll(findAllFiles(folder));
        }

        // Set up list view which shows all of the files in a list
        listView = findViewById(R.id.listView);
        CustomAdapter customAdapter = new CustomAdapter(); // is what adapts rows into the list view
        listView.setAdapter(customAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), AudioPlayer.class);
                intent.putExtra("titleLabel", allFilePaths.get(i).getName());
//                System.out.println(FilenameUtils.getExtension(allFilePaths.get(i).getAbsolutePath()));
                intent.putExtra("filePath", allFilePaths.get(i).getAbsolutePath());
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int buttonId = item.getItemId();

        if (buttonId == R.id.addUrl){
            customDialogAddUrl();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Sends a post request with youtube url and receives a list of file titles. Then it cycles through them calling the sendDownloadRequest
    private void sendConvertRequest(String youtubeURL){
        RequestBody formBody = new FormBody.Builder()
                .add("url", youtubeURL)
                .build();

        Request request = new Request.Builder()
                .url(baseURL+"convert")
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                System.out.println("winner winner chicken dinner");
                String responseData = response.body().string();
                if (responseData.equals("Invalid link")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"Invalid youtube video link",Toast.LENGTH_LONG).show();
                        }
                    });
                }else{
                    String[] allDownloadableFiles = responseData.split("!!!");
                    for (String fileTitle: allDownloadableFiles){
                        sendDownloadRequest(fileTitle);
                    }
                    // refreshes app once downloaded
                    finish();
                    startActivity(getIntent());
                }
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println("Connection err");
                System.out.println(e.toString());
                Log.d("myapp", Log.getStackTraceString(new Exception()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Failed to connect to server",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // Sends a get request with a single file title and store it
    private void sendDownloadRequest(final String fileTitle){
        Request request = new Request.Builder()
                .url(baseURL+"/Downloads/"+fileTitle)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Connection err when downloading",Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                File downloadedFile = new File(folder, fileTitle+".mp3");
                if (downloadedFile.exists()) {
                    boolean fileDeleted = downloadedFile.delete();
                    Log.v("fileDeleted", fileDeleted + "");
                }
                boolean fileCreated = downloadedFile.createNewFile();
                BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                sink.writeAll(response.body().source());
                sink.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,fileTitle +" has finished downloading",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // the input box for the url. It invokes the sendConvertRequest() method
    private void customDialogAddUrl(){
        final Dialog dialog = new Dialog(MainActivity.this);
        //We have added a title in the custom layout. So let's disable the default title.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //The user will be able to cancel the dialog bu clicking anywhere outside the dialog.
        dialog.setCancelable(true);
        //Mention the name of the layout of your custom dialog.
        dialog.setContentView(R.layout.custom_dialog);

        //Initializing the views of the dialog.
        final EditText urlText = dialog.findViewById(R.id.urlText);
        Button submitButton = dialog.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String youtubeURL = urlText.getText().toString();
                if (youtubeURL.isEmpty()){
                    Toast.makeText(getApplicationContext(),"Input cannot be empty",Toast.LENGTH_LONG).show();
                    try{
                        Thread.sleep(1000);
                    }catch (Exception e){
                        
                    }
                }else if(!youtubeURL.contains("https://www.youtube.com") && !youtubeURL.contains("https://youtu.be")){ // as mobile links are http://youtu.be whereas web links are http://youtube.com
                    Toast.makeText(getApplicationContext(),"Input must be a valid youtube link",Toast.LENGTH_LONG).show();
                }
                else{
                    sendConvertRequest(youtubeURL);
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    // returns an array list with all files
    private ArrayList<File> findAllFiles(File folder){
        ArrayList<File> cumulativeFiles = new ArrayList<>();
        if( folder.isDirectory() ) {
            String[] filesAndDirectories = folder.list();
            for( String fileOrDirectory : filesAndDirectories) {
                File subFolderOrSubFile = new File(folder.getAbsolutePath() + "/" + fileOrDirectory);
                cumulativeFiles.addAll(findAllFiles(subFolderOrSubFile));
            }
        } else {
            ArrayList<File> singleFile = new ArrayList<>();
            singleFile.add(folder); // in this case the folder variable is actually a file
            return singleFile; // in order to have the same return type as cumulativeFiles
        }
        return cumulativeFiles;
    }

    // region Requesting storage permission code
    private void RequestMultiplePermission() {

        // Creating String Array with Permissions.
        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                {
                        WRITE_EXTERNAL_STORAGE,
                        INTERNET,
                        ACCESS_NETWORK_STATE
                }, RequestPermissionCode);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case RequestPermissionCode:

                if (grantResults.length > 0) {

                    boolean WriteExternalStoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean InternetPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean NetworkPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (WriteExternalStoragePermission && InternetPermission && NetworkPermission) {
                        Toast.makeText(MainActivity.this, "All Permission Granted", Toast.LENGTH_LONG).show();
                        // Restarts app once permissions are granted
                        finish();
                        startActivity(getIntent());
                    }
                    else {
                        Toast.makeText(MainActivity.this,"Permission must be allowed for this app to work",Toast.LENGTH_LONG).show();
                        RequestMultiplePermission();
                    }
                }

                break;
        }
    }
    // Does a permission check intially
    public boolean CheckingPermissionIsEnabledOrNot() {

        int FirstPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int SecondPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        int ThirdPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_NETWORK_STATE);

        if( FirstPermissionResult == PackageManager.PERMISSION_GRANTED &&
                SecondPermissionResult == PackageManager.PERMISSION_GRANTED &&
                ThirdPermissionResult == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }
// endregion

    // class is part of this file as the getLayoutInflater method is used
    class CustomAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return allFilePaths.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View subView = getLayoutInflater().inflate(R.layout.row_file, null);
            TextView titleLabel = subView.findViewById(R.id.titleLabel);
            titleLabel.setText(allFilePaths.get(i).getName());

            return subView;
        }
    }
}
