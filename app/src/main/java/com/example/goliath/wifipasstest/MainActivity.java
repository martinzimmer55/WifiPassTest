package com.example.goliath.wifipasstest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    //private String ssid = "";
    //private String password = "";
    //private String security = "";
    private WifiConfiguration wifiConfiguration;
    private static final String TAG = "WifiPassTest";
    private int oldNetworkId = -1;
    private int newNetworkId;
    private String oldNetworkSssid = "";
    private String newNetworkSsid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //inicializo variables
        wifiConfiguration = new WifiConfiguration();
        //seteo el layout
        setContentView(R.layout.activity_main);
        //obtengo los datos de red original por si falla la conexion, en ese caso tengo que volver atras y conectar a la red original
        //obtenerDatosWifiAnterior();
    }

    public void cambiarConfigWifi(View v) {
        /*metodo para crear la nueva configuracion, activarla y conectar a la red del pastillero
        se usan los parametros de ssid  y password obtenidos del codigo QR*/
        //Los datos los saco del intent
        // Para este caso de prueba saco los datos ingresados de la pantalla
        TextView txtSSID = (TextView) findViewById(R.id.txtSsid);
        TextView txtPassword = (TextView) findViewById(R.id.txtPassword);
        TextView txtSecurity = (TextView) findViewById(R.id.txtSecurity);
        String newSsid = txtSSID.getText().toString();
        String newPassword = txtPassword.getText().toString();
        String newSecurity = txtSecurity.getText().toString();
        obtenerDatosWifiAnterior();
        cambiarRed(newSsid, newPassword, newSecurity);
    }

    public void cambiarRed(String ssid, String password, String security) {
        //creo la nueva configuracion de red nueva
        crearConfig(ssid, password, security);
        //agrego el wifi manager
        agregarWifiManagerNuevo(ssid);
        obtenerNetworkIdNuevo(ssid);
        //conecto a la red nueva
        conectarWifiNueva();
        //conectarAWifi(ssid);
        //verifico si la conexion fue exitosa
        new verificarConexionExitosa().execute();
    }

    public void crearConfig(String ssid, String password, String security) {
        //metodo para crear la configuracion de wifi para conectarse al pastillero
        //luego de escanear el codigo qr obtengo ssid y password, que son los parametros que recibe el metodo
        wifiConfiguration.SSID = "\"" + ssid + "\"";
        newNetworkSsid = ssid;
        //segun el tipo de seguridad que soporte la red se debe cambiar la configuracion
        if (security.contains("WPA")) { //WPA
            Log.d(TAG, "Creando configuracion WPA");
            wifiConfiguration.preSharedKey = "\"" + password + "\"";
            //wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            //wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        } else {
            if (security.contains("WEP")) { //WEP
                Log.d(TAG, "Creando configuracion WEP");
                wifiConfiguration.wepKeys[0] = "\"" + password + "\"";
                wifiConfiguration.wepTxKeyIndex = 0;
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            } else { //Libre
                Log.d(TAG, "Creando configuracion Libre");
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
        }
    }

    public void agregarWifiManagerNuevo(String ssid) {
        //metodo para agregar la configuracion del wifi del pastillero al telefono
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(wifiConfiguration);
        Log.d(TAG, "SSID nuevo: " + ssid);
    }

    public void obtenerNetworkIdNuevo(String ssid) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                if (i.networkId != oldNetworkId) {
                    newNetworkId = i.networkId;
                    Log.d(TAG, "ID de red nuevo: " + String.valueOf(newNetworkId));
                    break;
                }
            }
        }
    }

    public void obtenerDatosWifiAnterior() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo != null) {
            oldNetworkId = connectionInfo.getNetworkId();
            oldNetworkSssid = connectionInfo.getSSID();
            Log.d(TAG, "SSID anterior; " + oldNetworkSssid);
            Log.d(TAG, "ID de red anterior: " + String.valueOf(oldNetworkId));
        } else {
            Log.d(TAG, "ConnectionInfo null, no se obtuvieron datos" + oldNetworkSssid);
        }
    }

    public void conectarWifiAnterior() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiMgr.disconnect();
        wifiMgr.enableNetwork(oldNetworkId, true);
        wifiMgr.reconnect();
        Log.d(TAG, "Conectando a la red anterior con id: " + oldNetworkId);
    }

/*    public void conectarWifiNueva() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiMgr.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                if (i.networkId != oldNetworkId) {
                    wifiMgr.disconnect();
                    Log.d(TAG, "Desconectado");
                    wifiMgr.enableNetwork(i.networkId, true);
                    newNetworkId = i.networkId;
                    Log.d(TAG, "Habilitado");
                    wifiMgr.reconnect();
                    Log.d(TAG, "Reconectando");
                    break;
                }
            }
        }
    }*/

    public void conectarWifiNueva() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG, "Conectando a la red nueva con id: " + newNetworkId);
        wifiMgr.disconnect();
        wifiMgr.enableNetwork(newNetworkId, true);
        wifiMgr.reconnect();
    }

    public void eliminarWifiManagerAnterior() {
        //metodo para eliminar la configuracion del wifi anterior
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.removeNetwork(oldNetworkId);
    }

    public void eliminarWifiManagerNuevo() {
        //metodo para eliminar la configuracion del wifi anterior
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.removeNetwork(newNetworkId);
    }

    public class verificarConexionExitosa extends AsyncTask {
        /* Para verificar si la conexion fue exitosa. Como SSID y security son parametros que saco del escaneo de red, seguro son correctos
        * entonces en caso de que la conexion falle, seguro es porque el usuario ingreso un password incorrecto*/

        boolean result = false;

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                //espero 3 segundos para darle tiempo al telefono para que se conecte a la red (siempre demora unos segundos
                Log.d(TAG, "Verificando si la conexion fue exitosa...");
                sleep(3000);
            /* pasados los 3 segundos obtengo la info de la conexion. Si esta conectada, la conexion fue exitosa, de lo contrario
            * hay que volver a pedir la password */
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                //NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo == null) {
                    Log.d(TAG, "Network info null");
                    eliminarWifiManagerNuevo();
                    conectarWifiAnterior();
                    result = false;
                } else {
                    if (networkInfo.isConnected()) {
                        // si la red a la que me conecte es igual a la red a la que estaba conectado, tengo que borrar el wifimanager anterior
                        //if (oldNetworkSssid.equals(newNetworkSsid)) {
                            eliminarWifiManagerAnterior();
                        //}
                        Log.d(TAG, "Conexion exitosa");
                        result = true;
                    } else {
                        Log.d(TAG, "Error en la conexion");
                        eliminarWifiManagerNuevo();
                        conectarWifiAnterior();
                        result = false;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return result;
        }
        protected void onPostExecute(boolean result){
            if(result == false) {
                createPasswordDialog();
            }
        }
    }

    public void createPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText txtPassword = new EditText(this);
        builder.setTitle("Password incorrecto, vuelva a ingresarlo por favor: ");   // Título
        builder.setView(txtPassword);
        builder.setPositiveButton("Conectar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("Password: ", txtPassword.getText().toString());
                //password = txtPassword.getText().toString();
                //cambiarRed(ssid, password, security);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                conectarWifiAnterior();
                Log.i("Cancelado: ", "cancelado por el usuario");
            }
        });
        Dialog dialog = builder.create();
        dialog.show();
    }
}
