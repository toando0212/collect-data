package com.example.tool;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.IOException;
import java.util.List;

import android.net.wifi.ScanResult;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileWriter;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private ListView listView;
    private WifiListAdapter wifiListAdapter;

    private EditText editTextX, editTextY;
    private Button buttonRescan, buttonSave, buttonOpenCSV, buttonClearCSV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        editTextX = findViewById(R.id.editTextX);
        editTextY = findViewById(R.id.editTextY);

        buttonRescan = findViewById(R.id.buttonRescan);
        buttonSave = findViewById(R.id.buttonSave);
        buttonOpenCSV = findViewById(R.id.buttonOpenCSV);
        buttonClearCSV = findViewById(R.id.buttonClearCSV);




        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Check if permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission granted, proceed with scanning Wi-Fi networks
            scanWifiNetworks();
        }


        buttonRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanWifiNetworks();
                Toast.makeText(MainActivity.this, "Scan completed", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Save button click
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanWifiNetworks();
                saveWifiDataToCSV();

            }
        });

        // Handle Open CSV button click
        buttonOpenCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCSVFile();
            }
        });

        //handle clear csv
        buttonClearCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hiển thị hộp thoại xác nhận
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có chắc chắn muốn xóa toàn bộ nội dung file CSV không?")
                        .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nếu người dùng chọn "Xóa"
                                clearCSVFile();
                            }
                        })
                        .setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nếu người dùng chọn "Hủy", chỉ đóng dialog
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
    }

    // Method to scan and display Wi-Fi networks
//    private void scanWifiNetworks() {
//        // Ensure Wi-Fi is enabled
//        if (!wifiManager.isWifiEnabled()) {
//            wifiManager.setWifiEnabled(true);
//        }
//
//        boolean isScanStarted = wifiManager.startScan();
//        if (!isScanStarted) {
//            Toast.makeText(this, "Wi-Fi scan could not start. Try again.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//
//        // Start scanning for available networks
//        List<ScanResult> scanResults = wifiManager.getScanResults();
//
//        if (scanResults != null && !scanResults.isEmpty()) {
//            wifiListAdapter = new WifiListAdapter(this, scanResults);
//            listView.setAdapter(wifiListAdapter);
//        } else {
//            Toast.makeText(this, "No Wi-Fi networks found", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void scanWifiNetworks() {
        // Ensure Wi-Fi is enabled
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        // Start scanning for available networks
        boolean isScanStarted = wifiManager.startScan();
        if (!isScanStarted) {
            Toast.makeText(this, "Wi-Fi scan could not start. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register BroadcastReceiver to handle scan results
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                if (scanResults != null && !scanResults.isEmpty()) {
                    // Update the ListView with new scan results
                    wifiListAdapter = new WifiListAdapter(MainActivity.this, scanResults);
                    listView.setAdapter(wifiListAdapter);
                    wifiListAdapter.notifyDataSetChanged();

                    Toast.makeText(MainActivity.this, "Wi-Fi list updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "No Wi-Fi networks found.", Toast.LENGTH_SHORT).show();
                }

                // Unregister receiver after processing results
                unregisterReceiver(this);
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }


    // Method to save Wi-Fi data to CSV
    private void saveWifiDataToCSV() {
        // Get position from EditText fields
        String xPosition = editTextX.getText().toString().trim();
        String yPosition = editTextY.getText().toString().trim();

        // Validate position inputs
        if (xPosition.isEmpty() || yPosition.isEmpty()) {
            Toast.makeText(this, "Please enter both X and Y positions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a CSV file to save the data
        String fileName = "wifi_data.csv";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try (FileWriter writer = new FileWriter(file, true)) {
            // Write the header if the file is empty
            if (file.length() == 0) {
                writer.append("SSID, Signal Strength (dBm),MAC address, X Position, Y Position\n");
            }

            // Get the list of Wi-Fi networks and write to the file
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                String ssid = scanResult.SSID;
                int signalStrength = scanResult.level; // dBm value
                String macAddress = scanResult.BSSID; //MAC address
                // Write Wi-Fi data to CSV
                writer.append(ssid)
                        .append(",")
                        .append(String.valueOf(signalStrength))
                        .append(",")
                        .append(macAddress)
                        .append(",")
                        .append(xPosition)
                        .append(",")
                        .append(yPosition)
                        .append("\n");
            }

            // Notify the user
            Toast.makeText(this, "Wi-Fi data saved successfully!", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving data to CSV", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to open the CSV file
    private void openCSVFile() {
        // Get the file from the directory
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "wifi_data.csv");

        // Check if the file exists
        if (file.exists()) {
            // Create an Intent to view the CSV file
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            Toast.makeText(this, "CSV file not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearCSVFile() {
        // Xác định file CSV
        String fileName = "wifi_data.csv";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            // Ghi đè file với nội dung trống
            FileWriter writer = new FileWriter(file, false); // `false` để ghi đè
            writer.close();

            // Thông báo cho người dùng
            Toast.makeText(this, "CSV file cleared successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error clearing CSV file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifiNetworks();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}