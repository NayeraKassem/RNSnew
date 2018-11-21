package activity;

import app.AppConfig;
import app.AppController;
import helper.SQLiteHandler;
import helper.SessionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.pc.rnsmodel.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    private TextView txtName;
    private TextView txtEmail;
    private Button btnLogout;
    private Button btnLocation;
    private SQLiteHandler db;
    private SessionManager session;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final String TAG = "MainActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    String name;
    String email;
    String User_id;
    ListView listView;
    Button SortRate;
    List<JSONObject> myJsonArrayAsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkLoc();
        txtName = (TextView) findViewById(R.id.name);
        txtEmail = (TextView) findViewById(R.id.email);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLocation = (Button) findViewById(R.id.btnLoc);

        SortRate = (Button) findViewById(R.id.sortRate);
        listView = (ListView) findViewById(R.id.listView2);
        listView.setOnItemClickListener(MainActivity.this);
        getLandmarks();

        SortRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Collections.sort(myJsonArrayAsList, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject jsonObjectA, JSONObject jsonObjectB) {
                        int compare = 0;
                        try {
                            Double keyA = jsonObjectA.getDouble("rating");
                            Double keyB = jsonObjectB.getDouble("rating");
                            compare = Double.compare(keyA, keyB);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return compare;
                    }
                });
                try {
                    Sorting();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        db = new SQLiteHandler(getApplicationContext());


        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }


        HashMap<String, String> user = db.getUserDetails();

         name = user.get("name");
         email = user.get("email");

        txtName.setText(name);
        txtEmail.setText(email);


        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,
                        MapsActivity.class);
                startActivity(intent);
            }
        });
    }

    public void onItemClick(AdapterView<?> l, View v, int position, long id) {

        // Then you start a new Activity via Intent
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, DetailsActivity.class);
        intent.putExtra("position", position);
        // Or / And
        intent.putExtra("id", id);
        try {
            intent.putExtra("Latitude", myJsonArrayAsList.get(position).getDouble("Latitude"));
            intent.putExtra("Longitude", myJsonArrayAsList.get(position).getDouble("Longitude"));
            intent.putExtra("Name",myJsonArrayAsList.get(position).getString("Name"));
            intent.putExtra("rating",myJsonArrayAsList.get(position).getDouble("rating"));
            intent.putExtra("PhoneNo",myJsonArrayAsList.get(position).getString("Phone Number"));
            intent.putExtra("ID",myJsonArrayAsList.get(position).getInt("ID"));
            intent.putExtra("object",myJsonArrayAsList.get(position).toString());
            intent.putExtra("username",name);
            intent.putExtra("email", email);
            intent.putExtra("image path", myJsonArrayAsList.get(position).getString("image path"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        startActivity(intent);
    }


    private void getLandmarks() {
        // Tag used to cancel the request
        String tag_string_req = "req_register";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LANDMARKS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());

                try {
                    JSONArray jObj = new JSONArray(response);
                    Log.d(TAG, "nayera");
                    String error = jObj.toString();
                    if (error != null) {
                        String name = jObj.getJSONObject(0).getString("Name");

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
        Object[] heroes = new Object[jsonArray.length()];

        myJsonArrayAsList = new ArrayList<JSONObject>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            heroes[i] = obj.getString("Name") + '\n'
                    + obj.getString("rating");


            myJsonArrayAsList.add(obj);
        }

        Log.d(TAG, "before" + myJsonArrayAsList.toString());
        getDeviceLocation();
        Log.d(TAG, "after" + myJsonArrayAsList.toString());
        ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<Object>(this, android.R.layout.simple_list_item_1, heroes);
        listView.setAdapter(arrayAdapter);
    }

    private void Sorting() throws JSONException {
        Object test[] = new Object[myJsonArrayAsList.size()];
        for (int i = 0; i < myJsonArrayAsList.size(); i++) {
            JSONObject obj = myJsonArrayAsList.get(i);
            test[i] = obj.getString("Name") + '\n'
                    + obj.getString("rating") + '\n'
                    + obj.getString("Distance");


        }

        ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<Object>(this, android.R.layout.simple_list_item_1, test);
        listView.setAdapter(arrayAdapter);
    }

    private void checkLoc() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;

            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");

                            Location currentLocation = (Location) task.getResult();

                            float[] results = new float[myJsonArrayAsList.size()];
                            Object[] updated = new Object[myJsonArrayAsList.size()];
                            try {
                                for (int i = 0; i < myJsonArrayAsList.size(); i++) {
                                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                            myJsonArrayAsList.get(i).getDouble("Latitude"), myJsonArrayAsList.get(i).getDouble("Longitude"), results);
                                    myJsonArrayAsList.get(i).put("Distance", results[0]);

//                                    String ditTest = myJsonArrayAsList.get(i).getString("Distance");
//                                    Double.parseDouble(ditTest);
//
                                    updated[i] = myJsonArrayAsList.get(i).getString("Name") + '\n'
                                            + myJsonArrayAsList.get(i).getString("rating") + '\n'
                                            + myJsonArrayAsList.get(i).getString("Distance");

                                    Log.d(TAG, "ski dist" + myJsonArrayAsList.get(i).getString("Distance"));

                                }
                                Collections.sort(myJsonArrayAsList, new Comparator<JSONObject>() {
                                    @Override
                                    public int compare(JSONObject jsonObjectA, JSONObject jsonObjectB) {
                                        int compare = 0;
                                        try {
                                            Double key1 = jsonObjectA.getDouble("Distance");
                                            Double key2 = jsonObjectB.getDouble("Distance");
                                            compare = Double.compare(key1, key2);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        return compare;
                                    }
                                });
                                 Sorting();
//                                ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<Object>(MainActivity.this, android.R.layout.simple_list_item_1, updated);
//                                listView.setAdapter(arrayAdapter);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(getApplicationContext(), "unable to get current location", Toast.LENGTH_SHORT).show();
                        }

                    }
//                    private void SortByDistance() throws JSONException {
//                        Object test[] = new Object[myJsonArrayAsList.size()];
//                        for (int i = 0; i < myJsonArrayAsList.size(); i++) {
//                            JSONObject obj = myJsonArrayAsList.get(i);
//                            test[i] = obj.getString("Name") + '\n'
//                                    + obj.getString("rating")+'\n'
//                            +obj.getString("Distance");
//
//
//                        }
//
//                        ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<Object>(MainActivity.this, android.R.layout.simple_list_item_1, test);
//                        listView.setAdapter(arrayAdapter);
//                    }
                });

            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }

    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}