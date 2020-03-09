package com.example.bhoomi.mapnavigationdemo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,OnMapReadyCallback{

    Context ctx=this;
    GoogleMap mMap;
    GPSTracker gpsTracker;
    AlertDialog.Builder alertDialog;
    double currentLat;
    double currentLong;
    LatLng pickUp;
    LatLng destination,between,endLatLong;
    ArrayList<LatLng> MarkerPoints=new ArrayList<>();
    ArrayList<LatLng> points;
    String distance;
    String duration;
    Marker marker,currentMarker;
    Button button;
    int index=0,current=0;
    PolylineOptions lineOptions = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final String provider = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!provider.contains("gps")) {
            Log.d("mytag" ,"gps is off");
//            if (alertDialog != null) {
//            }
            alertDialog = new android.support.v7.app.AlertDialog.Builder(this);
            alertDialog.setTitle("DISCOVER");
            alertDialog.setMessage("Hello.!! DISCOVER wants to access your location. Please turn on GPS.");
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton("SETTING",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
                            Intent gpsOptionsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(gpsOptionsIntent);
                        }
                    });
            alertDialog.show();
        }

        getPermission();

        allocateMemory();

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapone);
        supportMapFragment.getMapAsync((OnMapReadyCallback) this);

        setListener();

    }

    private void setListener() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("urtag","index--"+index);
                Log.d("urtag","points.size()--"+points.size());


//                index=0;
                if(points.size()==1){
//                    getNewPath(points.get(0),destination);
                    showCurvedPolyline(points.get(0),destination,0.2);
                }
                else if(index<=points.size() && points.size()!=1){

                    getRoute(points.get(1),destination);
                    showCurvedPolyline(points.get(index-1),destination,0.2);

                }
            }
        });
    }

    private void allocateMemory() {
        gpsTracker = new GPSTracker(ctx);
        button=findViewById(R.id.button);
    }

    private void getPermission() {
        Log.d("mytag","getPermission called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d("mytage","inside if getPermission");
                String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

                requestPermissions(permission, 12);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==12 && grantResults.length==2){
            SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapone);
            supportMapFragment.getMapAsync((OnMapReadyCallback) this);
        }
        else {
            Toast.makeText(MainActivity.this,"Permission not granted",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap=googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            mMap.setMyLocationEnabled(true);
            return;
        }
        //Initialize Google Play Services
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.ACCESS_FINE_LOCATION)
//                    == PackageManager.PERMISSION_GRANTED) {
//                buildGoogleApiClient();
//                mMap.setMyLocationEnabled(true);
//            }
//        } else {
//            buildGoogleApiClient();
//            mMap.setMyLocationEnabled(true);
//        }
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
//        mMap.setMyLocationEnabled(true);
        mMap.setMapType(googleMap.MAP_TYPE_NORMAL);
        //mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CoustamMapTilePrivider(getResources().getAssets())));
        mMap.setBuildingsEnabled(false);

        mMap.getUiSettings().setZoomControlsEnabled(false);
        // mMap.setMinZoomPreference(11);
        mMap.setTrafficEnabled(false);

        currentLat = gpsTracker.getLatitude();
        currentLong = gpsTracker.getLongitude();

        pickUp = new LatLng(currentLat, currentLong);
        destination=new LatLng(23.0333,72.5552);
        between=new LatLng(23.0330,72.5579);


        MarkerOptions options = new MarkerOptions();
        options.title("current location");
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        options.title("driver");
        options.position(new LatLng(currentLat, currentLong));

        currentMarker=mMap.addMarker(options);

         marker=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).position(destination));

        if (currentLat != 0.0 && currentLong != 0.0) {
            Log.d("mytag","cureentlat-"+currentLat+"currentlong-"+currentLong);
                getRoute(pickUp,destination);
//            getAddress(currentLat, currentLong, txt_auto_pickup);
//            getNearByCar(currentLat,currentLong,mMap);
        }

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                // mMap.setMinZoomPreference(15);
                return false;
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

            }
        });
        mMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                // mMap.setMinZoomPreference(12);
                MarkerOptions options = new MarkerOptions();
                options.title("current location");
                options.position(new LatLng(location.getLatitude(), location.getLongitude()));
                /*CircleOptions circleOptions = new CircleOptions();
                circleOptions.center(new LatLng(location.getLatitude(),
                        location.getLongitude()));

                circleOptions.radius(200);
                circleOptions.fillColor(Color.RED);
                circleOptions.strokeWidth(6);

                mMap.addCircle(circleOptions);*/
                mMap.addMarker(options);

                // Already two locations

            }
        });
        LatLng location = new LatLng(currentLat, currentLong);
        CameraPosition current = new CameraPosition.Builder().target(location).zoom(15.5F).bearing(300F) // orientation
                .tilt(50F) // viewing angle
                .build();

        // use map to move camera into position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(current));
    }

    public void getRoute(LatLng pickUp, LatLng destination) {

//        index++;

        if (MarkerPoints.size() > 1) {
            MarkerPoints.clear();
            mMap.clear();
        }

        // Adding new item to the ArrayList
        MarkerPoints.add(pickUp);
        MarkerPoints.add(destination);

//         Creating MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions();

//         Setting the position of the marker
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerOptions.position(destination);

        /**
         * For the start location, the color of marker is GREEN and
         * for the end location, the color of marker is RED.
         */
//        if (MarkerPoints.size() == 1) {
//            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        } else if (MarkerPoints.size() == 2) {
//            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//        }


        // Add new marker to the Google Map Android API V2
        mMap.addMarker(markerOptions);
        currentMarker=mMap.addMarker(new MarkerOptions().position(pickUp).title("driver").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Checks, whether start and end locations are captured
        if (MarkerPoints.size() >= 2) {
            LatLng origin = pickUp;
            LatLng dest = destination;

            // Getting URL to the Google Directions API
            String url = getUrl(origin, dest);
            Log.d("onMapClick", url.toString());
            FetchUrl fetchUrl = new FetchUrl();

            // Start downloading json data from Google Directions API
            fetchUrl.execute(url);
            //move map camera
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
//            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        }
        CameraPosition current = new CameraPosition.Builder().target(pickUp).zoom(15.5F).bearing(300F) // orientation
                .tilt(50F) // viewing angle
                .build();

        // use map to move camera into position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(current));
//        CameraUpdateFactory.zoomTo(11f);
//        CameraUpdateFactory.scrollBy(0, 30);

//        Projection projection = mMap.getProjection();
//        LatLng markerLocation = markerOptions.getPosition();
//        Point screenPosition = projection.toScreenLocation(markerLocation);
//        Point mappoint = mMap.getProjection().toScreenLocation(new LatLng(pickUp.latitude, pickUp.longitude));
//        mappoint.set(mappoint.x+70, mappoint.y+200);
//        mMap.moveCamera(CameraUpdateFactory.zoomTo(11f));
//        mMap.animateCamera(CameraUpdateFactory.newLatLng(mMap.getProjection().fromScreenLocation(mappoint)));

    }
    public String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + "AIzaSyBoEjKK2DGakqVE_9n0JT83oEjbTm-Ui1Y";


        return url;
    }
    public class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }


    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("mytag","downloadUrl"+ data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("mytag","Exception"+ e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("mytag","ParserTask"+jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("mytag","ParserTask parser"+ parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("mytag","ParserTask route"+routes.toString());

            } catch (Exception e) {
                Log.d("mytag","ParserTask exception"+e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {



            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {

                    HashMap<String, String> point = path.get(j);

                    if (j == 0) {    // Get distance from the list
                        distance = (String) point.get("distance");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = (String) point.get("duration");
                        continue;
                    }
                    else if(j==2){

                        double endLat = Double.parseDouble(point.get("endLat"));
                        double endLong = Double.parseDouble(point.get("endLong"));
                        endLatLong=new LatLng(endLat,endLong);

                        Log.d("urtag","parser End latlong"+endLatLong);
                        Log.d("urtag","destination latlong"+destination);

                        continue;

                    }
//                    if(j==0){ // Get distance from the list
//                        distance = point.get("distance");
//                        duration = (String)point.get("duration");
//                        continue;
//                    }
// else if(j==1){ // Get duration from the list
//                        duration = (String)point.get("duration");
//                        continue;
//                    }

                    Log.d("mytag","distance--"+distance);
                    Log.d("mytag","duration--"+duration);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat, lng);


                    points.add(position);
                    Log.d("mytag","parser latlong"+position);


                }

                index=points.size();
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);


                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    // Use default InfoWindow frame
                    @Override
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    // Defines the contents of the InfoWindow
                    @Override
                    public View getInfoContents(Marker arg0) {

                        View v=null;

                        if(arg0.getTitle().equals("driver")){

                            Log.d("mytag","infowindow---");
                             v = getLayoutInflater().inflate(R.layout.info_window, null);

                            // Getting the position from the marker
                            LatLng latLng = arg0.getPosition();

                            // Getting reference to the TextView to set latitude
                            TextView tvLat = (TextView) v.findViewById(R.id.txtDis);

                            // Getting reference to the TextView to set longitude
//                        TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);

                            // Setting the latitude
                            tvLat.setText("Distance:" + distance+"   Duration:"+duration);

                            // Setting the longitude
//                        tvLng.setText("Longitude:"+ latLng.longitude);

                            // Returning the view containing InfoWindow contents
//                            return v;
                        }
                        else {

                        }
//                        // Getting view from the layout file info_window_layout
//
                        return v;
                    }
                });
                currentMarker.showInfoWindow();
                Log.d("mytag","parser points"+points);
                Log.d("mytag","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("mytag","without Polylines drawn");
            }
        }
    }
    public void  getNewPath(LatLng pickUp,LatLng destination){

        ArrayList<LatLng> lastPoint = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            lineOptions = new PolylineOptions();


            // Fetching all the points in i-th route




//                    if(j==0){ // Get distance from the list
//                        distance = point.get("distance");
//                        duration = (String)point.get("duration");
//                        continue;
//                    }
// else if(j==1){ // Get duration from the list
//                        duration = (String)point.get("duration");
//                        continue;
//                    }




                lastPoint.add(pickUp);
                lastPoint.add(destination);
                Log.d("urtag","last point"+points.get(0));
                Log.d("urtag","destination"+destination);
            }

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(lastPoint);
            lineOptions.width(10);
            lineOptions.color(Color.BLUE);

        if(lineOptions != null) {
            mMap.addPolyline(lineOptions);
        }
        else {
            Log.d("mytag","without Polylines drawn");
        }
    }
    private void showCurvedPolyline (LatLng p1, LatLng p2, double k) {
        //Calculate distance and heading between two points
        double d = SphericalUtil.computeDistanceBetween(p1,p2);
        double h = SphericalUtil.computeHeading(p1, p2);

        //Midpoint position
        LatLng p = SphericalUtil.computeOffset(p1, d*0.5, h);

        //Apply some mathematics to calculate position of the circle center
        double x = (1-k*k)*d*0.5/(2*k);
        double r = (1+k*k)*d*0.5/(2*k);

        LatLng c = SphericalUtil.computeOffset(p, x, h + 90.0);

        //Polyline options
        PolylineOptions options = new PolylineOptions();
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(30), new Gap(20));

        //Calculate heading between circle center and two points
        double h1 = SphericalUtil.computeHeading(c, p1);
        double h2 = SphericalUtil.computeHeading(c, p2);

        //Calculate positions of points on circle border and add them to polyline options
        int numpoints = 100;
        double step = (h2 -h1) / numpoints;

        for (int i=0; i < numpoints; i++) {
            LatLng pi = SphericalUtil.computeOffset(c, r, h1 + i * step);
            options.add(pi);
        }

        //Draw polyline
        mMap.addPolyline(options.width(10).color(Color.MAGENTA).geodesic(false).pattern(pattern));

        if(points.size()==1){

            CameraPosition current = new CameraPosition.Builder().target(destination).zoom(20F)
                    .bearing(300F) // orientation
                    .tilt(0F) // viewing angle
                    .build();

            // use map to move camera into position
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(current));
        }
    }
}
