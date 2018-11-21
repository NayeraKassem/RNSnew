package activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.pc.rnsmodel.R;
import com.google.android.gms.location.FusedLocationProviderClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import app.AppConfig;
import app.AppController;
import helper.SQLiteHandler;

public class DetailsActivity extends AppCompatActivity {
    private TextView setName;
    private TextView setRating;
    private EditText userReview;
    private Button submitReview;
    private ImageView imageView;
    JSONObject JOBJ;
    private RatingBar ratingBar;
    String obj;
    Double average;
    Double Latitude;
    Double Longitude;
    Double Rating;
    String Name;
    String PhoneNo;
    String review;
    String place_name;
    String place_ID;
    String user_name;
    String email;
    String imagePath;
   FloatingActionButton callfloat;
   FloatingActionButton locationfloat;
   FloatingActionButton info;
   LinearLayout callLayout;
   LinearLayout locationLayout;
    private ListView listReview;
    int id = 0;
    private static final String TAG = "DetailsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ProgressDialog pDialog;
    String tt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        setName = (TextView) findViewById(R.id.setName);
        setRating = (TextView) findViewById(R.id.setRating);
        userReview = (EditText) findViewById(R.id.userReview);
        submitReview = (Button) findViewById(R.id.submitReview);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        listReview = (ListView) findViewById(R.id.reviewList);
    // imageView = (ImageView) findViewById(R.id.imageView);
        callfloat=(FloatingActionButton) findViewById(R.id.fab) ;
        locationfloat= (FloatingActionButton) findViewById(R.id.fablocation);
        info=(FloatingActionButton) findViewById(R.id.fabinfo);
        callLayout=(LinearLayout)findViewById(R.id.callLayout);
        locationLayout=(LinearLayout)findViewById(R.id.locationLayout);
        obj = getIntent().getStringExtra("object");
        Log.d(TAG, "OBJECT " + obj);
        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);


        Latitude = getIntent().getDoubleExtra("Latitude", 0.00);
        Longitude = getIntent().getDoubleExtra("Longitude", 0.00);
        Rating = getIntent().getDoubleExtra("rating", 0.00);
        Name = getIntent().getStringExtra("Name");
        PhoneNo = getIntent().getStringExtra("PhoneNo");
        id = getIntent().getIntExtra("ID", 0);
        email = getIntent().getStringExtra("email");
        imagePath = getIntent().getStringExtra("image path");

        setName.setText(Name);
        setRating.setText(Rating.toString());

        place_name = setName.getText().toString().trim();
        tt = place_name;
//        place_ID = String.valueOf(id);
        user_name = getIntent().getStringExtra("username");

        getReviews();
       info.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               if(callLayout.getVisibility()==View.VISIBLE && locationLayout.getVisibility()==View.VISIBLE){
                   callLayout.setVisibility(View.GONE);
                   locationLayout.setVisibility(View.GONE);
               }
               else{
                   callLayout.setVisibility(View.VISIBLE);
                   locationLayout.setVisibility(View.VISIBLE);
               }
           }
       });
        locationfloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(DetailsActivity.this, MapsActivity.class);
                intent.putExtra("Longitude", Longitude);
                intent.putExtra("Latitude", Latitude);
                startActivity(intent);
            }
        });
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar rtBar, float rating, boolean fromUser) {
                float rat = (float) rating;
                Log.d(TAG, "RATING" + rat);
                average = (Rating + rat) / 2;
                Log.d(TAG, "average" + average);
                jobj();
                updateRate(average, id);
            }
        });

        submitReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                review = userReview.getText().toString().trim();
                Log.d(TAG, "bagarab" + review + ";" + place_name + id + user_name + email);
                SendReview(email, id, place_name, user_name, review);

            }
        });
      callfloat.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              dialContactPhone(PhoneNo);
          }
      });

    }

    private void dialContactPhone(final String phoneNumber) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
    }


    public void jobj() {
        try {
            JOBJ = new JSONObject(obj);
//            JOBJ.remove("Distance");
            JOBJ.put("rating", average);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "el klaam 3ala eh " + JOBJ);
    }

    private void updateRate(final Double rate, final int ID) {
        // Tag used to cancel the request
        String tag_string_req = "req_register";

        pDialog.setMessage("Registering ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LANDMARKS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("rating", rate.toString());
                params.put("ID", String.valueOf(ID));

                Log.d(TAG, "Reham rating:" + params);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    private void SendReview(final String useremail, final int place_id, final String placeName, final String userName, final String Review) {
        // Tag used to cancel the request
        String tag_string_req = "req_register";

        pDialog.setMessage("Registering ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_REVIEW, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", useremail);
                params.put("places_ID", String.valueOf(place_id));
                params.put("places_name", placeName);
                params.put("users_name", userName);
                params.put("reviews", Review);

                Log.d(TAG, "Nayera Reviw: " + params);
                return params;

            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void getReviews() {
        // Tag used to cancel the request
        String tag_string_req = "req_register";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_SENDREVIEW, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());

                try {
                    JSONArray jObj = new JSONArray(response);
                    Log.d(TAG, "nayera");
                    String error = jObj.toString();
                    if (error != null) {
                        //    String name = jObj.getJSONObject(0).getString("Name");

                        loadIntoListView(jObj.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void loadIntoListView(String json) throws JSONException {
        Log.d(TAG, "reham test" + json);
        JSONArray jsonArray = new JSONArray(json);
        ArrayList<String> placeReviews= new ArrayList<>();
        //String[] btengan = new String[jsonArray.length()];

        int i = 0;
        int j = 0;

        while (i < jsonArray.length()) {
            JSONObject obj = jsonArray.getJSONObject(i);
            //  Log.d(TAG, "el object geh: " + " " + obj);
            //    String s = obj.getString("places_name");
            int places_id = obj.getInt("places_ID");
            Log.d(TAG, "test_string: " + places_id);

            String places_id_string = (String) obj.get("places_ID");
            Log.d(TAG, "test_string: " + places_id_string);
            Log.d(TAG, "id: " + id);

            //  Log.d(TAG, "islam test: " + s.equals(Name) );

                if (places_id_string.equals("" + id)) {
                    Log.d(TAG, "test el i" + i);
                    placeReviews.add( obj.getString("users_name") + '\n'
                            + obj.getString("reviews"));

                }

            i++;
            //Log.d(TAG, "test el i" + i);
        }

        //Log.d(TAG, "test el length" + jsonArray.length());
        //Log.d(TAG, "loadIntoListView: " + Name);

        String[] list = new String[placeReviews.size()];
        for (int k=0;k<placeReviews.size();k++){
            list[k]=placeReviews.get(k);
        }
        ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<Object>(getApplicationContext(), android.R.layout.simple_list_item_1, list);
        listReview.setAdapter(arrayAdapter);
        /*ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<Object>(this, android.R.layout.simple_list_item_1, btengan);
        listReview.setAdapter(arrayAdapter);*/
        //Log.d(TAG, "test ana 5alast" + btengan[0]);
    }


}
