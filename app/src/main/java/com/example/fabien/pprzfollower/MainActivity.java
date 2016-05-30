package com.example.fabien.pprzfollower;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private Button okButton = null;
    private LocationManager locMan = null;
    private TextView gpsTextView = null;
    private EditText ipEdit = null;
    private EditText portEdit = null;
    private String ipAddr = "";
    private int port = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        okButton = (Button) findViewById(R.id.okButton);
        gpsTextView = (TextView) findViewById(R.id.gpsText);
        locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ipEdit = (EditText) findViewById(R.id.ipEdit);
        portEdit = (EditText) findViewById(R.id.portEdit);
        //byte [] msg = createPPRZMsg(43.4622, 1.2729, 185);
        //new testUdp().execute(msg);

        try {
            locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, new
                    LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            String txt = location.getLatitude() + " " + location.getLongitude() + " " + location.getAltitude() + " "  + location.getSpeed() + "\n"+ location.getTime() + " " + location.getAccuracy() + " " + location.getBearing() + "\n";
                            byte [] msg = createPPRZMsg(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed());

                            new testUdp().execute(msg);
                            gpsTextView.setText(txt);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }
                    });
        } catch (SecurityException e) {
            Log.d("MYAPP", "Avez vous les droits d'accès au GPS ?");
        }

        okButton.setOnClickListener(okListener);

    }

    private View.OnClickListener okListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ipAddr = ipEdit.getText().toString();
            port = Integer.parseInt(portEdit.getText().toString());
        }
    };

    public byte[] createPPRZMsg(double dlat, double dlon, double dalt, float dspeed) {

        long lat = (long) (dlat * Math.pow(10, 7));
        long lon = (long) (dlon * Math.pow(10, 7));
        long alt = (long) (dalt * Math.pow(10, 3));
        int speed = (int) dspeed * 100;

        byte[] bs = new byte[30];
        int nb = 0;

        bs[nb++] = (byte) 0x99;      //PPRZ_STX
        bs[nb++] = 0;                //length set at the end
        bs[nb++] = 0;                //sender id
        bs[nb++] = 56;               //message id


        //transform lat to 4 bytes : 32 bits
        for (int i = nb; i < nb + 4; i++) {
            bs[i] = (byte) (lat & 0xFF);
            lat >>>= 8;
        }
        nb += 4;

        //transform lon to 4 bytes : 32 bits
        for (int i = nb; i < nb + 4; i++) {
            bs[i] = (byte) (lon & 0xFF);
            lon >>>= 8;
        }
        nb += 4;

        //transform alt to 4 bytes : 32 bits
        for (int i = nb; i < nb + 4; i++) {
            bs[i] = (byte) (alt & 0xFF);
            alt >>>= 8;
        }
        nb += 4;

        //transform speed to 2 bytes : 16 bits
        for (int i = nb; i < nb + 2; i++) {
            bs[i] = (byte) (speed & 0xFF);
            speed >>>= 8;
        }
        nb += 2;

        bs[1] = (byte) (nb+2); //la longueur !

        //calc checksum
        byte ck_A = 0;
        byte ck_B = 0;
        for (int i = 1; i < nb; i++) {
            ck_A += bs[i];
            ck_B += ck_A;
        }

        bs[nb++] = ck_A;
        bs[nb++] = ck_B;

        bs[1] = (byte) nb; //la longueur !
        byte[] msg = Arrays.copyOfRange(bs, 0, nb);

        return msg;
    }


    class testUdp extends AsyncTask<byte[], Integer, Long> {
        protected Long doInBackground(byte[]... msgs) {
            try {

                InetAddress serveur = InetAddress.getByName(ipAddr);
                int length = msgs[0].length;
                DatagramSocket socket = new DatagramSocket();
                DatagramPacket donneesEmises = new DatagramPacket(msgs[0], length, serveur, port);


                socket.setSoTimeout(3000);
                socket.send(donneesEmises);
            } catch (SocketTimeoutException ste) {
                Log.d("MYAPP", "Le delai pour la reponse a expire");
            } catch (Exception e) {
                Log.d("MYAPP", "échec !");
                e.printStackTrace();
            }
            return new Long(0);
        }
    }


}
