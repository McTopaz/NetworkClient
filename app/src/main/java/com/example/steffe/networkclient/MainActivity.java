package com.example.steffe.networkclient;

import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.io.OutputStream;


public class MainActivity extends ActionBarActivity {

    EditText txtIp;
    EditText txtPort;
    EditText txtTimeout;
    EditText txtRequest;
    RadioButton rbUdp;
    RadioButton rbTcp;
    Button btnSendButton;
    TextView lblresponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtIp = (EditText)findViewById(R.id.txtIP);
        txtPort = (EditText)findViewById(R.id.txtPort);
        txtTimeout = (EditText)findViewById(R.id.txtTimeout);
        txtRequest = (EditText)findViewById(R.id.txtRequest);
        rbUdp = (RadioButton)findViewById(R.id.rbUdp);
        rbTcp = (RadioButton)findViewById(R.id.rbTcp);
        btnSendButton = (Button)findViewById(R.id.btnSendRequest);
        lblresponse = (TextView)findViewById(R.id.lblResponse);

        ConnectSendButton();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ConnectSendButton() {
        Button button = (Button)findViewById(R.id.btnSendRequest);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SendReceive();
            }
        });
    }

    private void SendReceive() {
        String ip = txtIp.getText().toString();
        int port = Integer.parseInt(txtPort.getText().toString());
        int timeout = Integer.parseInt(txtTimeout.getText().toString());
        Boolean udp = rbUdp.isChecked();
        Boolean tcp = rbTcp.isChecked();
        String request = txtRequest.getText().toString();
        String response = "";

        if (udp) {
            response = SendReceiveUdp(ip, port, timeout, request);
        }
        if (tcp){
            response = SendReceiveTcp(ip, port, timeout, request);
        }

        lblresponse.setText(response);
    }

    private String SendReceiveUdp(String ip, int port, int timeout, String request) {

        // networkonmainthreadexception

        HashMap<String,Object> params = new HashMap<String, Object>();
        params.put("ip", ip);
        params.put("port", port);
        params.put("timeout", timeout);
        params.put("data", HexStringToByteArray(request));

        byte[] response = null;

        UdpClient client = new UdpClient();

        try {
            client.execute(params);
            response = client.get();
        }
        catch (Exception e) {
            return "Failed UDP";
        }

        try {
            return ByteArrayToHexString(response);
        }
        catch (Exception e) {
            return "Invalid parsing of UDP-packet.";
        }
    }

    private String SendReceiveTcp(String ip, int port, int timeout, String request) {

        HashMap<String,Object> params = new HashMap<String, Object>();
        params.put("ip", ip);
        params.put("port", port);
        params.put("timeout", timeout);
        params.put("data", HexStringToByteArray(request));

        byte[] response = null;

        TcpClient client = new TcpClient();

        try {
            client.execute(params);
            response = client.get();
        }
        catch (Exception e) {
            return "Failed TCP";
        }

        try {
            return ByteArrayToHexString(response);
        }
        catch (Exception e) {
            return "Invalid parsing of TCP-packet.";
        }
    }

    public byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public String ByteArrayToHexString(byte[] data) {

        String hex = "";

        for (int i = 0; i < data.length; i++) {
            if (((i % 12) == 0) && i != 0) {
                hex += System.lineSeparator();
            }
            hex += String.format("%02X ", data[i]);
        }

        return hex;
    }
}

class UdpClient extends AsyncTask<HashMap, Object, byte[]> {

    @Override
    protected byte[] doInBackground(HashMap... params) {

        HashMap<String, Object> p = params[0];
        String ip = (String)p.get("ip");
        int port = (int)p.get("port");
        int timeout = (int)p.get("timeout");
        byte[] data = (byte[])p.get("data");

        InetAddress serverAddress = null;

        try {
            serverAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return new byte[0];
        }

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            return new byte[0];
        }

        DatagramPacket sendPackage = new DatagramPacket(data, data.length, serverAddress, port);

        try {
            socket.send(sendPackage);
        }
        catch (IOException e) {
            return new byte[0];
        }

        byte[] buffer = new byte[1024];
        DatagramPacket receivePackage = new DatagramPacket(buffer, buffer.length);

        try {
            socket.receive(receivePackage);
        }
        catch (Exception e) {
            return new byte[0];
        }

        socket.close();
        return  receivePackage.getData();
    }
}

class TcpClient extends AsyncTask<HashMap, Object, byte[]> {

    @Override
    protected byte[] doInBackground(HashMap... params) {

        HashMap<String, Object> p = params[0];
        String ip = (String)p.get("ip");
        int port = (int)p.get("port");
        int timeout = (int)p.get("timeout");
        byte[] data = (byte[])p.get("data");

        Socket socket = null;

        try {
            socket = new Socket(ip, port);
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            return new byte[0];
        } catch (UnknownHostException e) {
            return new byte[0];
        } catch (IOException e) {
            return new byte[0];
        }

        OutputStream out;

        try {
            out = socket.getOutputStream();
            out.write(data);
        } catch (IOException e) {
            return new byte[0];
        }

        InputStream in;
        byte[] buffer = new byte[1024];
        int readBytes = 0;

        // if(inputStream.available() > 0) while((aByte = read()) > -1)

        try {
            in = socket.getInputStream();
            readBytes = in.read(buffer, 0, buffer.length);
        } catch (IOException e) {
            return new byte[0];
        }

        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            return new byte[0];
        }

        return Arrays.copyOfRange(buffer, 0, readBytes);
    }
}

// http://shabbynote.blogspot.se/2012/12/Stream-Socket-using-TCP-Socket-Programming-in-Java.html
