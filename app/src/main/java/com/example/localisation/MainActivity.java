package com.example.localisation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private RequestQueue requestQueue;
    private TextView tvInfo;

    private String insertUrl = "http://10.0.2.2/localisation/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, 1);
            return;
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000,
                150,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        altitude = location.getAltitude();
                        accuracy = location.getAccuracy();

                        String msg = "Latitude : " + latitude
                                + "\nLongitude : " + longitude
                                + "\nAltitude : " + altitude
                                + "\nPrécision : " + accuracy + " m";

                        tvInfo.setText(msg);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                        addPosition(latitude, longitude);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        String newStatus = "";
                        switch (status) {
                            case LocationProvider.OUT_OF_SERVICE:
                                newStatus = "OUT_OF_SERVICE";
                                break;
                            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                                newStatus = "TEMPORARILY_UNAVAILABLE";
                                break;
                            case LocationProvider.AVAILABLE:
                                newStatus = "AVAILABLE";
                                break;
                        }

                        Toast.makeText(getApplicationContext(),
                                "Statut du provider " + provider + " : " + newStatus,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        Toast.makeText(getApplicationContext(),
                                "Provider activé : " + provider,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        Toast.makeText(getApplicationContext(),
                                "Provider désactivé : " + provider,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                insertUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), "Réponse : " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Log plus précis pour debugger
                        String errorMsg = error.getMessage();
                        if (error.networkResponse == null) {
                            errorMsg = "Impossible de contacter le serveur (Vérifiez l'IP 10.0.2.2)";
                        }
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date_position", sdf.format(new Date()));

                // GESTION DE L'IDENTIFIANT (IMEI ou ANDROID_ID)
                String finalID = "unknown";
                try {
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    // Si on a la permission et qu'on est sur une version compatible
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        if (android.os.Build.VERSION.SDK_INT < 29) {
                            finalID = tm.getDeviceId();
                        }
                    }
                } catch (Exception e) {
                    // En cas d'erreur (Android 10+), on laisse finalID à "unknown"
                }

                // Si l'IMEI n'est pas récupérable (cas de ton erreur actuelle), on prend l'Android ID
                if (finalID == null || finalID.equals("unknown") || finalID.isEmpty()) {
                    finalID = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                }

                params.put("imei", finalID);
                return params;
            }
        };

        requestQueue.add(request);
    }
}