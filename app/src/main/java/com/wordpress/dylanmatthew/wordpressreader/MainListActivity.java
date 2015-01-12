package com.wordpress.dylanmatthew.wordpressreader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;


public class MainListActivity extends ActionBarActivity {


    public static final String TAG = MainListActivity.class.getSimpleName();        //TAG Variable

    protected JSONObject mBlogData;         //JSONObject with Blog Data

    public ListView listView;               //List View to display titles and dates

    public EditText editText;               //Edit Text for blog name

    public Button button;                   //Button to search blogs

    protected ProgressBar mProgressBar;         //Progress bar visible while posts load

    public String nameParam;                //Blog name entered by user

    public TextView textView;               //Instructions

    private final String KEY_TITLE = "title";
    private final String KEY_DATE = "date";         //Keys to use with Hash Map


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        listView = (ListView) findViewById(R.id.listView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        editText = (EditText) findViewById(R.id.editText);
        button = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.textView);              //Creating items


        listView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);                 //Hides ListView and ProgressBar for now

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(editText.getText().toString().equals(null)) {
                    Toast.makeText(MainListActivity.this, "Enter a blog name!", Toast.LENGTH_LONG).show();
                }else {

                    nameParam = editText.getText().toString();      //When button clicked, get parameter

                    //Check to see if there is network

                    if(isNetworkAvail()) {

                        //Show Progress bar to spin while Async Task executes
                        mProgressBar.setVisibility(View.VISIBLE);

                        //Async Task
                        GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
                        getBlogPostsTask.execute(nameParam);

                    }
                    else {
                        Toast.makeText(MainListActivity.this, "Network is unavailable!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //Set Listener for List View
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                try {

                    //Get the array of posts
                    JSONArray jsonPosts = mBlogData.getJSONArray("posts");

                    //Get individual post at position clicked
                    JSONObject jsonPost = jsonPosts.getJSONObject(position);

                    //Grab the Url
                    String blogUrl = jsonPost.getString("URL");

                    //Create Intent with Url and open it in WebView Activity
                    Intent intent = new Intent(MainListActivity.this, BlogWebViewActivity.class);
                    intent.setData(Uri.parse(blogUrl));

                    startActivity(intent);

                } catch (JSONException e) {
                    //JSONException
                }
            }
        });




    }






    //Method used to check network
    private boolean isNetworkAvail() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        boolean isAvail = false;

        if(networkInfo != null && networkInfo.isConnected()) {
            isAvail = true;
        }

        return isAvail;

    }




    //Handles info gathered from Async Task
    public void handleBlogResponse() {

        //Hides Button, EditText and TextView

        textView.setVisibility(View.INVISIBLE);
        button.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.INVISIBLE);

        //Show ListView

        listView.setVisibility(View.VISIBLE);

        //Hides Progress Bar
        mProgressBar.setVisibility(View.INVISIBLE);


        if(mBlogData == null) {

            updateDisplayForError();        //If no data, show error popup

            textView.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
            editText.setVisibility(View.VISIBLE);

        }
        else {
            try {

                //Grabs array of posts from data
                JSONArray jsonPosts = mBlogData.getJSONArray("posts");

                ArrayList<HashMap<String, String>> blogPosts = new ArrayList<HashMap<String, String>>();

                //Cycle through array of posts
                for(int i =0; i < jsonPosts.length(); i++) {

                    //Grab individual post
                    JSONObject post = jsonPosts.getJSONObject(i);

                    //Grab title and format it
                    String title = post.getString(KEY_TITLE);
                    title = Html.fromHtml(title).toString();

                    //Grab date
                    String date = post.getString(KEY_DATE);
                    char[] dateArray = date.toCharArray();
                    String fDate = "";

                    for(int j = 0; j < 10; j++){
                        fDate += dateArray[j];          //Format date to YYYY-MM-DD (2014-09-11)
                    }

                    //Add individual titles and dates to Hash Map
                    HashMap<String, String> blogPost = new HashMap<String, String>();
                    blogPost.put(KEY_TITLE, title);
                    blogPost.put(KEY_DATE, fDate);

                    //Add Hash Map to Array List
                    blogPosts.add(blogPost);
                }

                //Arrays for Simple Adapter
                String[] keys = {KEY_TITLE, KEY_DATE};
                int[] ids = {android.R.id.text1, android.R.id.text2};

                //Creates and sets Simple Adapter
                SimpleAdapter adapter =
                        new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);

                listView.setAdapter(adapter);

            }catch(Exception e){

            }
        }




    }

    //Builds and shows Alert Dialog when Blog Data is empty
    private void updateDisplayForError() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage("There was an error getting Blog Data");
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    //Async Task
    private class GetBlogPostsTask extends AsyncTask<String, Void, JSONObject> {


        @Override
        protected JSONObject doInBackground(String... params) {

            int responseCode = -1;
            JSONObject jsonResponse = null;

            try {

                //Url for API with parameter as site
                URL blogFeedUrl = new URL("https://public-api.wordpress.com/rest/v1.1/sites/" + params[0] + ".wordpress.com/posts/?fields=date,title,URL");

                //Connect to Url
                HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
                connection.connect();
                responseCode = connection.getResponseCode();


                //if response code is 200, continue
                if(responseCode == HttpsURLConnection.HTTP_OK) {

                    //Sets up Input Stream and Reader
                    InputStream inputStream = connection.getInputStream();

                    Reader reader = new InputStreamReader(inputStream);

                    int nextCharacter; // read() returns an int, we cast it to char later
                    String responseData = "";
                    while(true){ // Infinite loop, can only be stopped by a "break" statement
                        nextCharacter = reader.read(); // read() without parameters returns one character
                        if(nextCharacter == -1) // A return value of -1 means that we reached the end
                            break;
                        responseData += (char) nextCharacter; // The += operator appends the character to the end of the string
                    }




                    //set variable jsonResponse to the Data
                    jsonResponse = new JSONObject(responseData);

                }
                else {

                }


            }catch(MalformedURLException e){

            }
            catch(IOException e) {

            }
            catch(Exception e){

            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject results) {

            //Sets member variable to JSON data received in Async Task
            mBlogData = results;

            //Updates View
            handleBlogResponse();


        }

    }


}