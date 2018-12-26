package com.example.aria.uart2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
//import android.support.v7.app.AlertDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    private boolean isOpen;
    private Button openUart;
    private int retval;
    public int totalrecv;
    public readThread handlerThread;
    private Handler handler;
    private MainActivity activity;


    public int baudRate=115200;
    public byte baudRate_byte;
    public byte stopBit=1;
    public byte dataBit=8;
    public byte parity=0;
    public byte flowControl=0;

    private TextView displayData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayData=(TextView)findViewById(R.id.dataDisplay);
        openUart=(Button)findViewById(R.id.openButton);
        openUart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOpen) {


                    retval = MyApp.driver.ResumeUsbList();
                    if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    {
                        Toast.makeText(MainActivity.this, "打开设备失败!",
                                Toast.LENGTH_SHORT).show();
                        MyApp.driver.CloseDevice();
                    } else if (retval == 0) {
                        if (!MyApp.driver.UartInit()) {//对串口设备进行初始化操作
                            Toast.makeText(MainActivity.this, "设备初始化失败!",
                                    Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "打开" +
                                            "设备失败!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(MainActivity.this, "打开设备成功!",
                                Toast.LENGTH_SHORT).show();
                        isOpen = true;
                        openUart.setText("CloseUART");
//                        configButton.setEnabled(true);
//                        writeButton.setEnabled(true);
                        new readThread().start();//开启读线程读取串口接收的数据
                    } else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//                        builder.setIcon(R.drawable.icon);
                        builder.setTitle("未授权限");
                        builder.setMessage("确认退出吗？");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
//								MainFragmentActivity.this.finish();
                                System.exit(0);
                            }
                        });
                    }

//                    if (MyApp.driver.isConnected()) {
//                        if (MyApp.driver.SetConfig(baudRate, (byte) 8, (byte) 1, (byte) 0,//配置串口波特率，函数说明可参照编程手册
//                                (byte) 0)) {
//                            Toast.makeText(MainActivity.this, "串口设置成功!",
//                                    Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(MainActivity.this, "串口设置失败!",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    }else{
//
//                        Toast.makeText(MainActivity.this,"Device is not connenct",Toast.LENGTH_SHORT).show();
//                    }
                }else{
                    MyApp.driver.CloseDevice();
                    isOpen=false;
                    openUart.setText("OpenUart");
                    Toast.makeText(MainActivity.this,"串口已关闭！",Toast.LENGTH_SHORT).show();
                }
            }

        });

        Button setCongif=(Button)findViewById(R.id.setCFG);
        setCongif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (MyApp.driver.isConnected()) {
                    if (MyApp.driver.SetConfig(baudRate, dataBit, stopBit, parity,//配置串口波特率，函数说明可参照编程手册
                            flowControl)) {
                        Toast.makeText(MainActivity.this, "串口设置成功!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "串口设置失败!",
                                Toast.LENGTH_SHORT).show();
                    }
                }else{

                    Toast.makeText(MainActivity.this,"Device is not connected",Toast.LENGTH_SHORT).show();
                }
            }
        });



        MyApp.driver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION);

        if (!MyApp.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        isOpen=false;
        activity = this;




        handler = new Handler() {

            public void handleMessage(Message msg) {
//                readText.setText((String) msg.obj);
//                String str=(String) msg.obj;
                Toast.makeText(MainActivity.this,(String) msg.obj,Toast.LENGTH_SHORT).show();
//				readText.append((String) msg.obj);
                displayData.setText((String) msg.obj);
            }
        };


    }



    private class readThread extends Thread {

        public void run() {

            byte[] buffer = new byte[4096];



            while (true) {

                Message msg = Message.obtain();
                if (!isOpen) {
                    break;
                }
                int length = MyApp.driver.ReadData(buffer, 4096);
                if (length > 0) {
//					String recv = toHexString(buffer, length);
					String recv = new String(buffer, 0, length);
//                    totalrecv += length;
//                    String recv = String.valueOf(totalrecv);
                    msg.obj = recv;
                    handler.sendMessage(msg);
                }
            }
        }
    }
    /**
     * 将byte[]数组转化为String类型
     * @param arg
     *            需要转换的byte[]数组
     * @param length
     *            需要转换的数组长度
     * @return 转换后的String队形
     */
    private String toHexString(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }
}
