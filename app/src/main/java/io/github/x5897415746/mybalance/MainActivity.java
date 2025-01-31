package io.github.x5897415746.mybalance;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.x5897415746.mybalance.databinding.ActivityMainBinding;
import io.github.x5897415746.mybalance.util.NfcTechFilterXmlParser;
import io.github.x5897415746.mybalance.util.StringUtil;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications).build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (getIntent() != null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableNfcReading();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableNfcReading();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@NonNull Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Log.w(LOG_TAG, intent.getAction());
            Tag nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] tagId = nfcTag.getId();
            Log.w(LOG_TAG, "Found a tag. ID: " + (tagId == null ? "(null)" : StringUtil.bytesToHex(tagId)));
            Log.w(LOG_TAG, nfcTag.toString());

            if (StringUtil.strArrayContains(nfcTag.getTechList(), NfcF.class.getName())) {
                Log.w(LOG_TAG, "NfcF");
                readOctopusCard(nfcTag);
            }
        }
    }

    private boolean readOctopusCard(Tag tag) {
        final byte CMD_POLLING = 0x00;
        final byte CMD_READ_WO_ENCRYPTION = 0x06;
        final byte CMD_REQUEST_SYSCODE = 0x0C;

        final char SYSCODE_WILDCARD = 0xFFFF;
        final char SYSCODE_OCTOPUS = 0x8008;

        final char SERVICECODE_OCTOPUS_BALANCE = 0x1701;

        NfcF nfcFTag = NfcF.get(tag);
        try {
            int frameLength = 0;
            ByteBuffer frame;
            byte[] idm = null;
            byte[] pmm = null;
            byte[] response;
            ByteBuffer responseBuffer;
            nfcFTag.connect();
            // Polling
            frameLength = 6;
            frame = ByteBuffer.allocate(frameLength).order(ByteOrder.BIG_ENDIAN);
            frame.put((byte) frameLength).put(CMD_POLLING).putChar(SYSCODE_WILDCARD).put(new byte[]{0x01, 0x00});
            response = nfcFTag.transceive(frame.array());
            Log.w(LOG_TAG, StringUtil.bytesToHex(response));
            idm = Arrays.copyOfRange(response, 2, 10);
            pmm = Arrays.copyOfRange(response, 10, 18);
            // Request System Code
            frameLength = 10;
            frame = ByteBuffer.allocate(frameLength);
            frame.put((byte) frameLength).put(CMD_REQUEST_SYSCODE).put(idm);
            response = nfcFTag.transceive(frame.array());
            Log.w(LOG_TAG, StringUtil.bytesToHex(response));
            int systemCodeCount = Byte.toUnsignedInt(response[10]);
            List<Character> systemCodeList = new ArrayList<Character>();
            responseBuffer = ByteBuffer.wrap(response, 11, systemCodeCount * 2);
            while(systemCodeCount-- > 0) {
                systemCodeList.add(responseBuffer.getChar());
            }
            Log.w(LOG_TAG, systemCodeList.toString());
            if (!systemCodeList.contains(SYSCODE_OCTOPUS)) {
                Log.e(LOG_TAG, "Not a valid Octopus card: ");
                return false;
            }
            // Read without Authentication
            frameLength = 16;
            frame = ByteBuffer.allocate(frameLength);
            frame.put((byte) frameLength).put(CMD_READ_WO_ENCRYPTION).put(idm).put((byte) 0x01).putChar(SERVICECODE_OCTOPUS_BALANCE).put(new byte[]{0x01,(byte)0x80,0x00});
            response = nfcFTag.transceive(frame.array());
            Log.w(LOG_TAG, StringUtil.bytesToHex(response));
            responseBuffer = ByteBuffer.wrap(response, 10, response.length - 10);
            char readStatusFlag = responseBuffer.getChar();
            if (readStatusFlag != 0x0000) {
                Log.e(LOG_TAG, "Failed to read card. Status Flags: " + (int) readStatusFlag);
                return false;
            }
            int blockCount = Byte.toUnsignedInt(responseBuffer.get());
            int balanceValue = responseBuffer.getInt();
            double balance = (balanceValue - 500) / 10.0;
            Log.w(LOG_TAG, String.valueOf(balance));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to read card: " + e.getMessage());
        }
        return true;
    }

    private boolean enableNfcReading() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.w(LOG_TAG, "NFC Adapter is not available");
            return false;
        }
        if (!nfcAdapter.isEnabled()) {
            Log.w(LOG_TAG, "NFC is off");
            return false;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);
        String[][] techLists = NfcTechFilterXmlParser.parse(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{intentFilter}, techLists);
        return true;
    }

    private void disableNfcReading() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }



}
