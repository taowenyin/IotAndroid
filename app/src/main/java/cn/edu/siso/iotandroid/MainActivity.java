package cn.edu.siso.iotandroid;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

public class MainActivity extends AppCompatActivity {

    private ImageButton serverDataBtn = null, serverClearBtn = null;
    private RadioGroup serverDataType = null;
    private LineChartView lightChartView = null, mcuChartView = null;

    private static int addMcuDataTimes = 0; // 添加数据的次数
    private static int addLightDataTimes = 0; // 添加数据的次数

    public static final int MAX_POINTS_COUNT = 10; // 页面中点的个数
    public static final int CHART_TRANSPARENCY = 20; // 填充的透明度
    public static final int Y_AXIS_MAX_COUNT = 12; // Y轴点最大值数量
    public static final int Y_AXIS_MAX_VALUE = Y_AXIS_MAX_COUNT * 10; // Y轴最大值

    private Timer dataTimer = null;

    private IotRunnable iotRunnable = null;
    private Handler handler = null;
    private static String SERVER_IP = "39.104.87.214";

    public static String TAG = "MainActivity";

    public boolean isServerStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverDataBtn = findViewById(R.id.server_data_btn);
        serverClearBtn = findViewById(R.id.server_clear_btn);
        serverDataType = findViewById(R.id.server_data_type);
        lightChartView = findViewById(R.id.light_chart);
        mcuChartView = findViewById(R.id.mcu_chart);

        /**
         * 光照数据表的初始化
         */

        // 创建光照的数据线
        List<Line> lightLines = new ArrayList<Line>(); // 数据线集合
        List<PointValue> lightData = new ArrayList<PointValue>(); // 数据点集合
        Line lightLine = new Line();
        lightLine.setColor(ChartUtils.COLORS[0]);
        lightLine.setValues(lightData);
        lightLines.add(lightLine);

        // 创建光照的X、Y轴坐标
        Axis lightAxisX = new Axis();
        Axis lightAxisY = new Axis();
        lightAxisX.setHasLines(true).setName("次数（单位：次）").setTextColor(Color.parseColor("#000000"));
        lightAxisY.setName("光照值（单位：流明）").setTextColor(Color.parseColor("#000000"));
        List<AxisValue> lightYAxisLabels = new ArrayList<AxisValue>();
        for (int i = 0; i <= Y_AXIS_MAX_COUNT; i++) {
            int yValue = i * 10;
            lightYAxisLabels.add(new AxisValue(yValue).setLabel(String.valueOf(yValue)).setValue(yValue));
        }
        lightAxisY.setValues(lightYAxisLabels);

        // 创建图表数据集
        LineChartData lightCharData = new LineChartData();
        lightCharData.setAxisXBottom(lightAxisX); // 设置X轴坐标
        lightCharData.setAxisYLeft(lightAxisY); // 设置Y坐标
        lightCharData.setLines(lightLines);
        lightCharData.setBaseValue(Float.NEGATIVE_INFINITY);

        lightChartView.setLineChartData(lightCharData); // 为图表对象设置数据集

        // 设置图表视口
        Viewport lightViewPort = new Viewport(lightChartView.getMaximumViewport()); // 创建图表的视口
        lightViewPort.top = Y_AXIS_MAX_VALUE;
        lightViewPort.bottom = 0;
        lightViewPort.left = 0;
        lightViewPort.right = MAX_POINTS_COUNT - 1;
        lightChartView.setMaximumViewport(lightViewPort);
        lightChartView.setCurrentViewport(lightViewPort);

        /**
         * MCU温度数据表的初始化
         */

        // 创建光照图表数据线
        List<Line> mcuLines = new ArrayList<Line>();
        List<PointValue> mcuData = new ArrayList<PointValue>();
        Line mcuLine = new Line();
        mcuLine.setColor(ChartUtils.COLORS[1]);
        mcuLine.setValues(mcuData);
        mcuLines.add(mcuLine);

        // 创建X、Y轴坐标
        Axis mcuAxisX = new Axis();
        Axis mcuAxisY = new Axis();
        mcuAxisX.setHasLines(true).setName("次数（单位：次）").setTextColor(Color.parseColor("#000000"));
        mcuAxisY.setName("温度值（单位：C）").setTextColor(Color.parseColor("#000000"));
        List<AxisValue> mcuYAxisLabels = new ArrayList<AxisValue>();
        for (int i = 0; i <= Y_AXIS_MAX_COUNT; i++) {
            int yValue = i * 10;
            mcuYAxisLabels.add(new AxisValue(yValue).setLabel(String.valueOf(yValue)).setValue(yValue));
        }
        mcuAxisY.setValues(mcuYAxisLabels);

        // 创建图表数据集
        LineChartData mcuCharData = new LineChartData();
        mcuCharData.setAxisXBottom(mcuAxisX); // 设置X轴坐标
        mcuCharData.setAxisYLeft(mcuAxisY); // 设置Y坐标
        mcuCharData.setLines(mcuLines);
        mcuCharData.setBaseValue(Float.NEGATIVE_INFINITY);

        mcuChartView.setLineChartData(mcuCharData); // 为图表对象设置数据集

        // 设置图表视口
        Viewport mcuViewPort = new Viewport(mcuChartView.getMaximumViewport()); // 创建图表的视口
        mcuViewPort.top = Y_AXIS_MAX_VALUE;
        mcuViewPort.bottom = 0;
        mcuViewPort.left = 0;
        mcuViewPort.right = MAX_POINTS_COUNT - 1;
        mcuChartView.setMaximumViewport(mcuViewPort);
        mcuChartView.setCurrentViewport(mcuViewPort);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                byte[] data = msg.getData().getByteArray("DATA");

                byte type = data[15];
                int value = ((data[16] & 0xFF) << 8) + (data[17] & 0xFF);
                if (type == 1) {
                    Log.i(TAG, "Light = " + value / 1000);
                    addDataPoint((value / 1000), addLightDataTimes++, lightChartView);
                }
                if (type == 2) {
                    Log.i(TAG, "MCU = " + value);
                    addDataPoint(value, addMcuDataTimes++, mcuChartView);
                }
            }
        };

        serverDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServerStart) {
                    serverDataBtn.setImageResource(R.drawable.pause);
                    isServerStart = true;

                    if (serverDataType.getCheckedRadioButtonId() == R.id.iot_data) {
                        iotRunnable = new IotRunnable(handler); // 创建网络线程
                        new Thread(iotRunnable).start();
                    }
                    if (serverDataType.getCheckedRadioButtonId() == R.id.emulator_data) {
                        // 创建定时器，并启动定时器
                        dataTimer = new Timer();
                        dataTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                addDataPoint(getRandomValue(0, 100), addLightDataTimes++, lightChartView);
                                addDataPoint(getRandomValue(0, 100), addMcuDataTimes++, mcuChartView);
                            }
                        }, 0, 1000);
                    }

                } else {
                    serverDataBtn.setImageResource(R.drawable.play);
                    isServerStart = false;

                    if (serverDataType.getCheckedRadioButtonId() == R.id.iot_data) {
                        if (iotRunnable != null) {
                            iotRunnable.stop();
                            iotRunnable = null;
                        }
                    }
                    if (serverDataType.getCheckedRadioButtonId() == R.id.emulator_data) {
                        dataTimer.cancel();
                        dataTimer = null;
                    }
                }
            }
        });

        serverClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServerStart) {
                    Toast.makeText(getApplicationContext(), "先停止数据接收", Toast.LENGTH_LONG).show();
                } else {
                    resetDataPoint(lightChartView);
                    resetDataPoint(mcuChartView);
                }
            }
        });
    }

    private void resetDataPoint(LineChartView chart) {
        Line line = chart.getLineChartData().getLines().get(0); // 获取当前图表的数据线
        Axis axisX = chart.getChartData().getAxisXBottom(); // 获取X坐标
        Axis axisY = chart.getChartData().getAxisYLeft(); // 获取Y坐标

        List<PointValue> data = line.getValues(); // 获取数据线的点序列
        List<AxisValue> axisXLabels = axisX.getValues(); // 获取X坐标的点序列

        data.clear();
        axisXLabels.clear();

        line.setValues(data); // 设置数据线的新值
        axisX.setValues(axisXLabels);

        List<Line> lines = new ArrayList<Line>();
        lines.add(line);
        LineChartData lineChartData = new LineChartData(lines);
        lineChartData.setAxisYLeft(axisY);
        lineChartData.setAxisXBottom(axisX);
        lineChartData.setBaseValue(Float.NEGATIVE_INFINITY);
        lineChartData.setValueLabelBackgroundEnabled(false);
        lineChartData.setValueLabelsTextColor(line.getColor());
        chart.setLineChartData(lineChartData);

        Viewport v = new Viewport(chart.getCurrentViewport());
        v.bottom = 0;
        v.top = Y_AXIS_MAX_VALUE;
        v.left = 0;
        v.right = MAX_POINTS_COUNT - 1;
        chart.setMaximumViewport(v);
        chart.setCurrentViewport(v);

        addMcuDataTimes = 0;
        addLightDataTimes = 0;
    }

    private void addDataPoint(int value, int times, LineChartView chart) {
        Line line = chart.getLineChartData().getLines().get(0); // 获取当前图表的数据线
        Axis axisX = chart.getChartData().getAxisXBottom(); // 获取X坐标
        Axis axisY = chart.getChartData().getAxisYLeft(); // 获取Y坐标

        List<PointValue> data = line.getValues(); // 获取数据线的点序列
        List<AxisValue> axisXLabels = axisX.getValues(); // 获取X坐标的点序列

        /**
         * 当图表中的点数量大于最大值时就需要进行删除头部的点
         */
        if (times >= MAX_POINTS_COUNT) {
            data.remove(0); // 从数据点序列中移除头部的点
            axisXLabels.remove(0); // 从X坐标序列中移除头部的点

            // 修改旧数据点的X值，全都往前移动一个
            for (int i = 0; i < MAX_POINTS_COUNT - 1; i++) {
                data.get(i).set(i, data.get(i).getY());
            }
            data.add(new PointValue(data.size(), value)); // 添加新数据，X值还是最大值

            // 修改旧坐标的Label和Value，Label根据次数累加，而Value则保持0-最大值
            for (int i = 0; i < MAX_POINTS_COUNT - 1; i++) {
                char[] xLabel = axisXLabels.get(i).getLabelAsChars();
                axisXLabels.get(i).setValue(i).setLabel(String.valueOf(xLabel));
            }
            axisXLabels.add(new AxisValue(axisXLabels.size()).setLabel(String.valueOf(times)));
        } else {
            data.add(new PointValue(times, value));
            axisXLabels.add(new AxisValue(times).setLabel(String.valueOf(times)).setValue(times));
        }

        line.setValues(data); // 设置数据线的新值
        line.setCubic(true); // 设置数据线平滑
        line.setFilled(true); // 设置数据线填充面积
        line.setAreaTransparency(CHART_TRANSPARENCY); // 设置填充面积的透明度
        line.setHasLabels(true); // 在数据线上显示数据值
        axisX.setValues(axisXLabels);

        List<Line> lines = new ArrayList<Line>();
        lines.add(line);
        LineChartData lineChartData = new LineChartData(lines);
        lineChartData.setAxisYLeft(axisY);
        lineChartData.setAxisXBottom(axisX);
        lineChartData.setBaseValue(Float.NEGATIVE_INFINITY);
        lineChartData.setValueLabelBackgroundEnabled(false);
        lineChartData.setValueLabelsTextColor(line.getColor());
        chart.setLineChartData(lineChartData);

        Viewport v = new Viewport(chart.getCurrentViewport());
        v.bottom = 0;
        v.top = Y_AXIS_MAX_VALUE;
        v.left = 0;
        v.right = MAX_POINTS_COUNT - 1;
        chart.setMaximumViewport(v);
        chart.setCurrentViewport(v);
    }

    private int getRandomValue(int begin, int end) {
        Random random = new Random();
        return random.nextInt(end) % (end - begin + 1) + begin;
    }

    private class IotRunnable implements Runnable {

        private boolean isRunning = true;
        private Socket clientSocket = null;
        private Handler handler = null;

        public IotRunnable(Handler handler) {
            this.handler = handler;
        }

        public void stop() {
            isRunning = false;
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                clientSocket = new Socket(SERVER_IP, 8080);
                byte buffer[] = new byte[1];
                int dataLength = 0;
                InputStream clientInput = clientSocket.getInputStream();
                List<Byte> data = new ArrayList<Byte>();

                boolean isHeader = false, isTail = false, isBeginFrame = false;

                while (isRunning && (dataLength = clientInput.read(buffer)) != -1) {
                    if (dataLength > 0) {
                        if (buffer[0] == 'V' && !isBeginFrame) {
                            isHeader = true;
                        } else {
                            if (buffer[0] == '!' && isHeader) {
                                isBeginFrame = true;
                                isHeader = false;

                                data.add(((byte) 'V'));
                            } else {
                                isHeader = false;
                            }
                        }
                        if (isBeginFrame) {
                            data.add(buffer[0]);
                        }
                        if (buffer[0] == 'S' && isBeginFrame) {
                            isTail = true;
                        } else {
                            if (buffer[0] == '$' && isTail) {
                                isTail = false;
                                isBeginFrame = false;

                                Message message = new Message();
                                Bundle bundle = new Bundle();
                                byte[] dataArray = new byte[data.size()];
                                for (int i = 0; i < data.size(); i++) {
                                    dataArray[i] = data.get(i);
                                }
                                bundle.putByteArray("DATA", dataArray);
                                message.setData(bundle);
                                handler.sendMessage(message);
                                data.clear();
                            } else {
                                isTail = false;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
