package com.kaka.bluetoothble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.kaka.bluetoothble.adapter.BleAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
/**
 * @author ：ckf
 * @date ：Created in 2022/11/23 09:28
 * @function: 主页面
 * @description：
 * @modified By：LQX
 * @version: $
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG ="ble_tag" ;
    ProgressBar pbSearchBle;
    ImageView ivSerBleStatus;
    TextView tvSerBleStatus;
    TextView tvSerBindStatus;
    ListView bleListView;
    private LinearLayout operaView;
    private Button btnWrite;
    private Button btnRead,btnlingmindu,btnpid,btnyuzhi,btndianliu;
    private EditText etWriteContent,et_lingmindu,et_pid,et_yuzhi,et_dianliu;
    private TextView tvResponse;
    private List<BluetoothDevice> mDatas;
    private List<Integer> mRssis;
    private BleAdapter mAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private boolean isScaning=false;
    private boolean isConnecting=false;
    private BluetoothGatt mBluetoothGatt;

    //服务和特征值
    private UUID write_UUID_service;
    private UUID write_UUID_chara;
    private UUID read_UUID_service;
    private UUID read_UUID_chara;
    private UUID notify_UUID_service;
    private UUID notify_UUID_chara;
    private UUID indicate_UUID_service;
    private UUID indicate_UUID_chara;
    private String hex="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_device);
        initView();
        initData();
        mBluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();
        if (mBluetoothAdapter==null||!mBluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,0);
        }

    }

    private void initData() {
        mDatas=new ArrayList<>();
        mRssis=new ArrayList<>();
        mAdapter=new BleAdapter(MainActivity.this,mDatas,mRssis);
        bleListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void initView(){
        pbSearchBle=findViewById(R.id.progress_ser_bluetooth);
        ivSerBleStatus=findViewById(R.id.iv_ser_ble_status);
        tvSerBindStatus=findViewById(R.id.tv_ser_bind_status);
        tvSerBleStatus=findViewById(R.id.tv_ser_ble_status);
        bleListView=findViewById(R.id.ble_list_view);
        operaView=findViewById(R.id.opera_view);
        //btnWrite=findViewById(R.id.btnWrite);
        //btnRead=findViewById(R.id.btnRead);
        etWriteContent=findViewById(R.id.et_write);
        //tvResponse=findViewById(R.id.tv_response);

        et_lingmindu = findViewById(R.id.et_lingmindu);
        et_pid = findViewById(R.id.et_pid);
        et_yuzhi = findViewById(R.id.et_yuzhi);
        et_dianliu = findViewById(R.id.et_dianliu);

        btnlingmindu = findViewById(R.id.btnlingmindu);
        btnpid = findViewById(R.id.btnpid);
        btnyuzhi = findViewById(R.id.btnyuzhi);
        btndianliu = findViewById(R.id.btndianliu);

        btnlingmindu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //执行灵敏度写入操作
                lingmindu();
            }
        });
        btnpid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //执行pid写入操作
                pid();
            }
        });
        btnyuzhi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //执行阈值写入操作
                yuzhi();
            }
        });
        btndianliu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //执行阈值写入操作
                dianliu();
            }
        });
//        btnRead.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                readData();
//            }
//        });

//        btnWrite.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //执行写入操作
//                //writeData();
//            }
//        });


        ivSerBleStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScaning){
                    tvSerBindStatus.setText("停止搜索");
                    stopScanDevice();
                }else{
                    checkPermissions();
                }

            }
        });
        bleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isScaning){
                    stopScanDevice();
                }
                if (!isConnecting){
                    isConnecting=true;
                    BluetoothDevice bluetoothDevice= mDatas.get(position);
                    //连接设备
                    tvSerBindStatus.setText("连接中");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback, TRANSPORT_LE);
                    } else {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback);
                    }
                }

            }
        });


    }

    private void readData() {
        BluetoothGattCharacteristic characteristic=mBluetoothGatt.getService(read_UUID_service)
                .getCharacteristic(read_UUID_chara);
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    /**
     * 开始扫描 10秒后自动停止
     * */
    private void scanDevice(){
        tvSerBindStatus.setText("正在搜索");
        isScaning=true;
        pbSearchBle.setVisibility(View.VISIBLE);
        mBluetoothAdapter.startLeScan(scanCallback);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //结束扫描
                mBluetoothAdapter.stopLeScan(scanCallback);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isScaning=false;
                        pbSearchBle.setVisibility(View.GONE);
                        tvSerBindStatus.setText("搜索已结束");
                    }
                });
            }
        },10000);
    }

    /**
     * 停止扫描
     * */
    private void stopScanDevice(){
        isScaning=false;
        pbSearchBle.setVisibility(View.GONE);
        mBluetoothAdapter.stopLeScan(scanCallback);
    }


    BluetoothAdapter.LeScanCallback scanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.e(TAG, "run: scanning...");
            if (!mDatas.contains(device)){
                mDatas.add(device);
                mRssis.add(rssi);
                mAdapter.notifyDataSetChanged();
            }

        }
    };

    private BluetoothGattCallback gattCallback=new BluetoothGattCallback() {
        /**
         * 断开或连接 状态发生变化时调用
         * */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG,"onConnectionStateChange()");
            if (status==BluetoothGatt.GATT_SUCCESS){
                //连接成功
                if (newState== BluetoothGatt.STATE_CONNECTED){
                    Log.e(TAG,"连接成功");
                    //发现服务
                    gatt.discoverServices();
                }
            }else{
                //连接失败
                Log.e(TAG,"失败=="+status);
                mBluetoothGatt.close();
                isConnecting=false;
            }
        }
        /**
         * 发现设备（真正建立连接）
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //直到这里才是真正建立了可通信的连接
            isConnecting=false;
            Log.e(TAG,"onServicesDiscovered()---建立连接");
            //获取初始化服务和特征值
            initServiceAndChara();
            //订阅通知
//            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt
//                    .getService(notify_UUID_service).getCharacteristic(notify_UUID_chara),true);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleListView.setVisibility(View.GONE);
                    operaView.setVisibility(View.VISIBLE);
                    tvSerBindStatus.setText("已连接");
                }
            });
        }
        /**
         * 读操作的回调
         * */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.e(TAG,"onCharacteristicRead()");
        }
        /**
         * 写操作的回调
         * */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            Log.e(TAG,"onCharacteristicWrite()  status="+status+",value="+HexUtil.encodeHexStr(characteristic.getValue()));
        }
        /**
         * 接收到硬件返回的数据
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.e(TAG,"onCharacteristicChanged()"+characteristic.getValue());
            final byte[] data=characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addText(tvResponse,bytes2hex(data));
                }
            });

        }
    };
    /**
     * 检查权限
     */
    private void checkPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            // 用户已经同意该权限
                            scanDevice();
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            ToastUtils.showLong("用户开启权限后才能使用");
                        }
                    }
                });
    }


    private void initServiceAndChara(){
        List<BluetoothGattService> bluetoothGattServices= mBluetoothGatt.getServices();
        for (BluetoothGattService bluetoothGattService:bluetoothGattServices){
            List<BluetoothGattCharacteristic> characteristics=bluetoothGattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic:characteristics){
                int charaProp = characteristic.getProperties();
//                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                    read_UUID_chara=characteristic.getUuid();
//                    read_UUID_service=bluetoothGattService.getUuid();
//                    Log.e(TAG,"read_chara="+read_UUID_chara+"----read_service="+read_UUID_service);
//                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);
                }
//                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
//                    write_UUID_chara=characteristic.getUuid();
//                    write_UUID_service=bluetoothGattService.getUuid();
//                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);
//
//                }
//                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                    notify_UUID_chara=characteristic.getUuid();
//                    notify_UUID_service=bluetoothGattService.getUuid();
//                    Log.e(TAG,"notify_chara="+notify_UUID_chara+"----notify_service="+notify_UUID_service);
//                }
//                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
//                    indicate_UUID_chara=characteristic.getUuid();
//                    indicate_UUID_service=bluetoothGattService.getUuid();
//                    Log.e(TAG,"indicate_chara="+indicate_UUID_chara+"----indicate_service="+indicate_UUID_service);
//
//                }
            }
        }
    }

    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }

    private void lingmindu(){

        BluetoothGattService service=mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        byte[] data;
        try {
        String hex = et_lingmindu.getText().toString();
        String hexx = ((Integer.toHexString(Integer.parseInt(hex))));
        //String hexstr = "04" + "01" + hexx + "00";
        if (hexx.length() == 1 ){
            hexx ='0'+hexx;
        }
        String content="04" + "01" + hexx + "00";
        //String content=et_lingmindu.getText().toString();


        if (!TextUtils.isEmpty(content)){
            data=HexUtil.hexStringToBytes(content);
        }else{
            data=HexUtil.hexStringToBytes(hex);
        }
        if (data.length>20){//数据大于个字节 分批次写入
            Log.e(TAG, "灵敏度: length="+data.length);
            int num=0;
            if (data.length%20!=0){
                num=data.length/20+1;
            }else{
                num=data.length/20;
            }
            for (int i=0;i<num;i++){
                byte[] tempArr;
                if (i==num-1){
                    tempArr=new byte[data.length-i*20];
                    System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                }else{
                    tempArr=new byte[20];
                    System.arraycopy(data,i*20,tempArr,0,20);
                }
                charaWrite.setValue(tempArr);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }
        }else{
            charaWrite.setValue(data);
            mBluetoothGatt.writeCharacteristic(charaWrite);
        }} catch (Exception e) {
    e.printStackTrace();
}
    }
    private void pid(){

        BluetoothGattService service=mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        byte[] data;
        try {
        String hex = et_pid.getText().toString();
        String hexx = ((Integer.toHexString(Integer.parseInt(hex))));
        if (hexx.length() == 1 ){
            hexx ="000" +hexx;
        }
        if (hexx.length() == 2 ){
            hexx ="00"  +hexx;
        }
        if (hexx.length() == 3 ){
            hexx ='0' +hexx;
        //String content="05" + "02" + hexx + "00";
        //String content=et_pid.getText().toString();
    }
        String content="05" + "03" + hexx + "00";

        if (!TextUtils.isEmpty(content)){
            data=HexUtil.hexStringToBytes(content);
        }else{
            data=HexUtil.hexStringToBytes(hex);
        }
        if (data.length>20){//数据大于个字节 分批次写入
            Log.e(TAG, "pid: length="+data.length);
            int num=0;
            if (data.length%20!=0){
                num=data.length/20+1;
            }else{
                num=data.length/20;
            }
            for (int i=0;i<num;i++){
                byte[] tempArr;
                if (i==num-1){
                    tempArr=new byte[data.length-i*20];
                    System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                }else{
                    tempArr=new byte[20];
                    System.arraycopy(data,i*20,tempArr,0,20);
                }
                charaWrite.setValue(tempArr);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }
        }else{
            charaWrite.setValue(data);
            mBluetoothGatt.writeCharacteristic(charaWrite);
        }} catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void yuzhi(){

        BluetoothGattService service=mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        byte[] data;
        try {
        String hex = et_yuzhi.getText().toString();
        String hexx = ((Integer.toHexString(Integer.parseInt(hex))));
        if (hexx.length() == 1 ){
            hexx ="000" +hexx;
        }
        if (hexx.length() == 2 ){
            hexx ="00"  +hexx;
        }
        if (hexx.length() == 3 ){
            hexx ='0' +hexx;
            //String content="05" + "02" + hexx + "00";
            //String content=et_pid.getText().toString();
        }
        String content="05" + "02" + hexx + "00";

        if (!TextUtils.isEmpty(content)){
            data=HexUtil.hexStringToBytes(content);
        }else{
            data=HexUtil.hexStringToBytes(hex);
        }
        if (data.length>20){//数据大于个字节 分批次写入
            Log.e(TAG, "阈值: length="+data.length);
            int num=0;
            if (data.length%20!=0){
                num=data.length/20+1;
            }else{
                num=data.length/20;
            }
            for (int i=0;i<num;i++){
                byte[] tempArr;
                if (i==num-1){
                    tempArr=new byte[data.length-i*20];
                    System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                }else{
                    tempArr=new byte[20];
                    System.arraycopy(data,i*20,tempArr,0,20);
                }
                charaWrite.setValue(tempArr);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }
        }else{
            charaWrite.setValue(data);
            mBluetoothGatt.writeCharacteristic(charaWrite);
        }} catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void dianliu(){

        BluetoothGattService service=mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb"));
        //BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        byte[] data;
        try {
            String hex = et_dianliu.getText().toString();
            String hexx = ((Integer.toHexString(Integer.parseInt(hex))));

            if (hexx.length() == 1 ){
                hexx ='0'+hexx;
            }
            String content="04" + "12" + hexx + "00";
            //String content=et_lingmindu.getText().toString();


            if (!TextUtils.isEmpty(content)){
                data=HexUtil.hexStringToBytes(content);
            }else{
                data=HexUtil.hexStringToBytes(hex);
            }
            if (data.length>20){//数据大于个字节 分批次写入
                Log.e(TAG, "灵敏度: length="+data.length);
                int num=0;
                if (data.length%20!=0){
                    num=data.length/20+1;
                }else{
                    num=data.length/20;
                }
                for (int i=0;i<num;i++){
                    byte[] tempArr;
                    if (i==num-1){
                        tempArr=new byte[data.length-i*20];
                        System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                    }else{
                        tempArr=new byte[20];
                        System.arraycopy(data,i*20,tempArr,0,20);
                    }
                    charaWrite.setValue(tempArr);
                    mBluetoothGatt.writeCharacteristic(charaWrite);
                }
            }else{
                charaWrite.setValue(data);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }} catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static final String HEX = "0123456789abcdef";
    public static String bytes2hex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
    }
}
