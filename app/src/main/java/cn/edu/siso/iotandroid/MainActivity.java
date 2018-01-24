package cn.edu.siso.iotandroid;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;

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

    private ImageButton serverDataBtn = null;
    private Switch serverDataType = null;
    private LineChartView lightChartView = null, mcuChartView = null;

    private Axis lightAxisX = null, lightAxisY = null, mcuAxisX = null, mcuAxisY = null;
    private List<PointValue> lightData = null, mcuData = null;
    private List<Line> lightLines = null, mcuLines = null;
    private List<AxisValue> mcuYAxisLabels = null, lightYAxisLabels = null;

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
        serverDataType = findViewById(R.id.server_data_type);
        lightChartView = findViewById(R.id.light_chart);
        mcuChartView = findViewById(R.id.mcu_chart);

        lightChartView.setInteractive(false);//设置不可交互
        lightChartView.setScrollEnabled(true);
        lightChartView.setValueTouchEnabled(false);
        lightChartView.setFocusableInTouchMode(false);
        lightChartView.setViewportCalculationEnabled(true);
        lightChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        lightChartView.startDataAnimation();

        mcuChartView.setInteractive(false);//设置不可交互
        mcuChartView.setScrollEnabled(true);
        mcuChartView.setValueTouchEnabled(false);
        mcuChartView.setFocusableInTouchMode(false);
        mcuChartView.setViewportCalculationEnabled(true);
        mcuChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        mcuChartView.startDataAnimation();

        // 创建光照图表数据线
        lightLines = new ArrayList<Line>();
        lightData = new ArrayList<PointValue>();
        Line lightLine = new Line(lightData);
        lightLine.setColor(ChartUtils.COLORS[0]);
        lightLine.setShape(ValueShape.CIRCLE);
        lightLine.setCubic(true); //曲线是否平滑，即是曲线还是折线
        lightLine.setFilled(false); //是否填充曲线的面积
        lightLine.setHasLabels(true); //曲线的数据坐标是否加上备注
        lightLine.setHasLabelsOnlyForSelected(false); //点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        lightLine.setHasLines(true); //是否用线显示。如果为false 则没有曲线只有点显示
        lightLine.setHasPoints(true); //是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        lightLines.add(lightLine);
        // 创建X、Y轴坐标
        lightAxisX = new Axis();
        lightAxisY = new Axis();
        lightAxisX.setHasLines(true).setName("次数（单位：次）").setTextSize(10);
        lightAxisY.setHasLines(true).setName("光照值（单位：流明）").setTextSize(10);
        lightYAxisLabels = new ArrayList<AxisValue>();
        for (int i = 0; i < 10; i++) {
            int axisValue = i * 10;
            lightYAxisLabels.add(new AxisValue(axisValue).setLabel(String.valueOf(axisValue)));
        }
        lightAxisY.setValues(lightYAxisLabels);
        // 创建图表数据集
        LineChartData lightCharData = new LineChartData(lightLines);
        lightCharData.setAxisXBottom(lightAxisX); // 设置X轴坐标
        lightCharData.setAxisYLeft(lightAxisY); // 设置Y坐标
        lightChartView.setLineChartData(lightCharData); // 为图表对象设置数据集
        // 设置图表视口
        Viewport lightViewPort = new Viewport(); // 创建图表的视口
        lightViewPort.top = 100;//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        lightViewPort.bottom = 0;//Y轴下限，固定
        lightViewPort.left = 0;//X轴左边界，变化
        lightViewPort.right = 10;//X轴右边界，变化
        lightChartView.setCurrentViewport(lightViewPort);

        // 创建光照图表数据线
        mcuLines = new ArrayList<Line>();
        mcuData = new ArrayList<PointValue>();
        Line mcuLine = new Line(mcuData);
        mcuLine.setColor(ChartUtils.COLORS[1]);
        mcuLine.setShape(ValueShape.CIRCLE);
        mcuLine.setCubic(true); //曲线是否平滑，即是曲线还是折线
        mcuLine.setFilled(false); //是否填充曲线的面积
        mcuLine.setHasLabels(true); //曲线的数据坐标是否加上备注
        mcuLine.setHasLabelsOnlyForSelected(false); //点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        mcuLine.setHasLines(true); //是否用线显示。如果为false 则没有曲线只有点显示
        mcuLine.setHasPoints(true); //是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        mcuLines.add(mcuLine);
        // 创建X、Y轴坐标
        mcuAxisX = new Axis();
        mcuAxisY = new Axis();
        mcuAxisX.setHasLines(true).setName("次数（单位：次）").setTextSize(10);
        mcuAxisY.setHasLines(true).setName("温度值（单位：C）").setTextSize(10);
        mcuYAxisLabels = new ArrayList<AxisValue>();
        for (int i = 0; i < 10; i++) {
            int axisValue = i * 10;
            mcuYAxisLabels.add(new AxisValue(axisValue).setLabel(String.valueOf(axisValue)));
        }
        mcuAxisY.setValues(mcuYAxisLabels);
        // 创建图表数据集
        LineChartData mcuCharData = new LineChartData(mcuLines);
        mcuCharData.setAxisXBottom(mcuAxisX); // 设置X轴坐标
        mcuCharData.setAxisYLeft(mcuAxisY); // 设置Y坐标
        mcuChartView.setLineChartData(mcuCharData); // 为图表对象设置数据集
        // 设置图表视口
        Viewport mcuViewPort = new Viewport(); // 创建图表的视口
        mcuViewPort.top = 100;//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        mcuViewPort.bottom = 0;//Y轴下限，固定
        mcuViewPort.left = 0;//X轴左边界，变化
        mcuViewPort.right = 10;//X轴右边界，变化
        mcuChartView.setCurrentViewport(mcuViewPort);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                byte[] data = msg.getData().getByteArray("DATA");

                byte type = data[15];
                int value = ((data[16] & 0xFF) << 8) + (data[17] & 0xFF);
                if (type == 1) {
                    Log.i(TAG, "Light = " + value / 1000);
                    addDataPoint((value / 1000) + getRandomValue(20, 60), lightChartView);
                }
                if (type == 2) {
                    Log.i(TAG, "MCU = " + value);
                    addDataPoint(value + getRandomValue(1, 10), mcuChartView);
                }
            }
        };

        serverDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServerStart) {
                    serverDataBtn.setImageResource(R.drawable.pause);
                    isServerStart = true;

                    if (serverDataType.isChecked()) {
                        iotRunnable = new IotRunnable(handler); // 创建网络线程
                        new Thread(iotRunnable).start();
                    } else {
                        // 创建定时器，并启动定时器
                        dataTimer = new Timer();
                        dataTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                addDataPoint(getRandomValue(10, 100), lightChartView);
                                addDataPoint(getRandomValue(10, 100), mcuChartView);
                            }
                        }, 0, 1000);
                    }

                } else {
                    serverDataBtn.setImageResource(R.drawable.play);
                    isServerStart = false;

                    if (serverDataType.isChecked()) {
                        if (iotRunnable != null) {
                            iotRunnable.stop();
                            iotRunnable = null;
                        }
                    } else {
                        dataTimer.cancel();
                        dataTimer = null;
                    }
                }
            }
        });
    }

    private void addDataPoint(int value, LineChartView chart) {
        Line dataLine = chart.getLineChartData().getLines().get(0); // 获取数据线
        Axis axisX = chart.getChartData().getAxisXBottom(); // 获取X轴坐标
        Axis axisY = chart.getChartData().getAxisYLeft(); // 获取Y轴坐标

        List<PointValue> data = dataLine.getValues(); // 获取数据线的数据集
        List<AxisValue> xAxisLabels = axisX.getValues();
        List<AxisValue> yAxisLabels = axisY.getValues();

        int newIndex = data.size(); // 获取新数据要添加的索引

        data.add(new PointValue(newIndex, value)); // 添加新的数据
        xAxisLabels.add(new AxisValue(newIndex).setLabel(String.valueOf(newIndex))); // 添加X轴标签

        axisX.setValues(xAxisLabels);
        dataLine.setValues(data); // 添加数据线的数据

        List<Line> lines = new ArrayList<Line>(); // 创建数据线的集合
        lines.add(dataLine);
        LineChartData lineData = new LineChartData(lines); // 创建数据线对象
        lineData.setAxisXBottom(axisX); // 设置X轴坐标内容
        lineData.setAxisYLeft(axisY);

        chart.setLineChartData(lineData); // 在图表中设置数据

        Viewport port = new Viewport(); // 创建图表的视口
        port.top = 100;//Y轴上限，固定(不固定上下限的话，Y轴坐标值可自适应变化)
        port.bottom = 0;//Y轴下限，固定
        port.left = newIndex - 10;//X轴左边界，变化
        port.right = newIndex;//X轴右边界，变化

        chart.setCurrentViewport(port);
//        chart.setMaximumViewport(port);
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
