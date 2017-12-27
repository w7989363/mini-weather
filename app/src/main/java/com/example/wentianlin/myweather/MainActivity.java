package com.example.wentianlin.myweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.wentianlin.myweather.app.MyApplication;
import com.example.wentianlin.myweather.bean.TodayWeather;
import com.example.wentianlin.myweather.util.NetUtil;
/**
 * Created by wentianlin on 2017/9/27.
 */

public class MainActivity extends Activity implements View.OnClickListener,ViewPager.OnPageChangeListener{
    private static final int UPDATE_TODAY_WEATHER = 1;
    private ImageView mCitySelect;
    private ViewPagerAdapter vpAdapter;
    private List<View> views;
    private ImageView[] dots;
    private int ids[] = {R.id.ind1, R.id.ind2};
    //定义相关控件的对象
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv, temperatureTv, climateTv, windTv, city_name_Tv;
    private TextView week1,week2,week3,week4,temp1,temp2,temp3,temp4,climate1,climate2,climate3,climate4,fengli1,fengli2,fengli3,fengli4;
    private ImageView weatherImg1,weatherImg2,weatherImg3,weatherImg4;
    private ImageView weatherImg, pmImg;
    private ImageView mUpdateBtn;
    private ProgressBar mProgressBar;
    private ViewPager vp;

//    private LocationClient mLocationClient;
//    private MyLocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        //更新按钮
        mUpdateBtn = (ImageView)findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        mProgressBar = (ProgressBar)findViewById(R.id.title_update_progress);

        //选择城市按钮
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather", "网络OK");
            Toast.makeText(MainActivity.this,"网络OK！", Toast.LENGTH_LONG).show();
        }else{
            Log.d("myWeather", "网络挂了");
            Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
        }

//        mLocationClient = ((MyApplication)getApplication()).mLocationClient;
//        mLocationListener = new MyLocationListener();
//        mLocationClient.registerLocationListener(mLocationListener);
//        LocationClientOption option = new LocationClientOption();
//        option.setIsNeedAddress(true);
//        mLocationClient.setLocOption(option);
//        mLocationClient.start();

        initView();
        //this.onClick(findViewById(R.id.title_update_btn));

    }

//    @Override
//    protected void onDestroy() {
//        //取消注册的位置监听，以免内存泄露
//        mLocationClient.unRegisterLocationListener(mLocationListener);
//        // 退出时销毁定位
//        mLocationClient.stop();
//        super.onDestroy();
//    }

    //控件点击响应函数
    @Override
    public void onClick(View view){
        //城市列表被点击
        if(view.getId() == R.id.title_city_manager){
            Intent i = new Intent(this,SelectCity.class);
            //startActivity(i);
            i.putExtra("cityName", cityTv.getText().toString());
            startActivityForResult(i,1);

        }
        //更新按钮被点击
        if(view.getId() == R.id.title_update_btn){
            SharedPreferences sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code","101010100");
            Log.d("myWeather",cityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                mUpdateBtn.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                queryWeatherCode(cityCode);

            }else{
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    //Activity结束后回调函数
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 1 && resultCode == RESULT_OK){
            String newCityCode = data.getStringExtra("cityCode");
            String newCityName = data.getStringExtra("cityName");
            Log.d("myWeather","城市代码："+newCityCode);
            //保存配置
            SharedPreferences sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("main_city_name", newCityName);
            editor.putString("main_city_code", newCityCode);
            editor.commit();

            this.onClick(findViewById(R.id.title_update_btn));

        }
    }

    //消息回调函数
    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg){
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    //根据天气信息对象 更新UI
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    //获取天气网络数据包
    private void queryWeatherCode(String cityCode)  {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myWeather", address);
        //创建线程执行网络请求操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con=null;
                TodayWeather todayWeather = null;
                try{
                    URL url = new URL(address);
                    con = (HttpURLConnection)url.openConnection( );
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);
                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder() ;
                    String str;
                    while((str=reader.readLine()) != null){
                        response.append(str);
                        //Log.d("myWeather", str);
                    }
                    String responseStr=response.toString();
                    Log.d("myWeather", responseStr);
                    todayWeather = parseXML(responseStr);
                    if(todayWeather != null){
                        Log.d("myWeather",todayWeather.toString());
                        //将天气信息对象打包发送给回调函数
                        Message msg =new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj=todayWeather;
                        mHandler.sendMessage(msg);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(con != null){
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    //解析XML
    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;
        int fengxiangCount=0;
        int fengliCount =0;
        int dateCount=0;
        int highCount =0;
        int lowCount=0;
        int typeCount =0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather", "parsing XML...");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    // 判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather= new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if(xmlPullParser.getName().equals("type") && typeCount%2 == 1){
                                eventType = xmlPullParser.next();
                                typeCount++;
                                break;
                            }
                            if(xmlPullParser.getName().equals("fengli") && fengliCount%2 == 0){
                                eventType = xmlPullParser.next();
                                fengliCount++;
                                break;
                            }
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli1(xmlPullParser.getText());
                                fengliCount++;
                            }
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli2(xmlPullParser.getText());
                                fengliCount++;
                            }
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 7) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli3(xmlPullParser.getText());
                                fengliCount++;
                            }
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 9) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli4(xmlPullParser.getText());
                                fengliCount+=2;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            }
                            else if (xmlPullParser.getName().equals("date") && dateCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate1(xmlPullParser.getText());
                                dateCount++;
                            }
                            else if (xmlPullParser.getName().equals("date") && dateCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate2(xmlPullParser.getText());
                                dateCount++;
                            }
                            else if (xmlPullParser.getName().equals("date") && dateCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate3(xmlPullParser.getText());
                                dateCount++;
                            }
                            else if (xmlPullParser.getName().equals("date") && dateCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate4(xmlPullParser.getText());
                                dateCount++;
                            }else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            else if (xmlPullParser.getName().equals("high") && highCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh1(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            else if (xmlPullParser.getName().equals("high") && highCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh2(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            else if (xmlPullParser.getName().equals("high") && highCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh3(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            else if (xmlPullParser.getName().equals("high") && highCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh4(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            else if (xmlPullParser.getName().equals("low") && lowCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow1(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            else if (xmlPullParser.getName().equals("low") && lowCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow2(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            else if (xmlPullParser.getName().equals("low") && lowCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow3(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            else if (xmlPullParser.getName().equals("low") && lowCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow4(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                            else if (xmlPullParser.getName().equals("type") && typeCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType1(xmlPullParser.getText());
                                typeCount++;
                            }
                            else if (xmlPullParser.getName().equals("type") && typeCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType2(xmlPullParser.getText());
                                typeCount++;
                            }
                            else if (xmlPullParser.getName().equals("type") && typeCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType3(xmlPullParser.getText());
                                typeCount++;
                            }
                            else if (xmlPullParser.getName().equals("type") && typeCount == 8) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType4(xmlPullParser.getText());
                                typeCount+=2;
                            }
                        }
                        break;
                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    //初始化控件内容
    void initView(){
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        vp = (ViewPager)findViewById(R.id.viewpager);



        city_name_Tv.setText("N/A");
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        weekTv.setText("N/A");
        temperatureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        //初始化ViewPager
        LayoutInflater inflater = LayoutInflater.from(this);
        views = new ArrayList<View>();
        views.add(inflater.inflate(R.layout.more_weather1, null));
        views.add(inflater.inflate(R.layout.more_weather2, null));
        vpAdapter = new ViewPagerAdapter(views,this);
        vp.setAdapter(vpAdapter);
        vp.setOnPageChangeListener(this);
        //初始化导航点
        dots = new ImageView[views.size()];
        for(int i=0;i<views.size();i++){
            dots[i] = (ImageView)findViewById(ids[i]);
        }


    }

    //设置天气图片
    void setWeatherImg(ImageView view, String climate){
        switch (climate){
            case "暴雪":
                view.setImageResource(R.drawable.biz_plugin_weather_baoxue);
                break;
            case "暴雨":
                view.setImageResource(R.drawable.biz_plugin_weather_baoyu);
                break;
            case "大暴雨":
                view.setImageResource(R.drawable.biz_plugin_weather_dabaoyu);
                break;
            case "大雪":
                view.setImageResource(R.drawable.biz_plugin_weather_daxue);
                break;
            case "大雨":
                view.setImageResource(R.drawable.biz_plugin_weather_dayu);
                break;
            case "多云":
                view.setImageResource(R.drawable.biz_plugin_weather_duoyun);
                break;
            case "雷阵雨":
                view.setImageResource(R.drawable.biz_plugin_weather_leizhenyu);
                break;
            case "雷阵雨冰雹":
                view.setImageResource(R.drawable.biz_plugin_weather_leizhenyubingbao);
                break;
            case "晴":
                view.setImageResource(R.drawable.biz_plugin_weather_qing);
                break;
            case "沙尘暴":
                view.setImageResource(R.drawable.biz_plugin_weather_shachenbao);
                break;
            case "特大暴雨":
                view.setImageResource(R.drawable.biz_plugin_weather_tedabaoyu);
                break;
            case "雾":
                view.setImageResource(R.drawable.biz_plugin_weather_wu);
                break;
            case "小雪":
                view.setImageResource(R.drawable.biz_plugin_weather_xiaoxue);
                break;
            case "小雨":
                view.setImageResource(R.drawable.biz_plugin_weather_xiaoyu);
                break;
            case "阴":
                view.setImageResource(R.drawable.biz_plugin_weather_yin);
                break;
            case "雨夹雪":
                view.setImageResource(R.drawable.biz_plugin_weather_yujiaxue);
                break;
            case "阵雪":
                view.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
                break;
            case "阵雨":
                view.setImageResource(R.drawable.biz_plugin_weather_zhenyu);
                break;
            case "中雪":
                view.setImageResource(R.drawable.biz_plugin_weather_zhongxue);
                break;
            case "中雨":
                view.setImageResource(R.drawable.biz_plugin_weather_zhongyu);
                break;
            default:
                view.setImageResource(R.drawable.biz_plugin_weather_qing);
                break;
        }

    }

    //更新UI中的控件
    void updateTodayWeather(TodayWeather todayWeather){
        //更新progressBar的显示
        mProgressBar.setVisibility(View.INVISIBLE);
        mUpdateBtn.setVisibility(View.VISIBLE);
        //更新天气显示控件
        city_name_Tv.setText(todayWeather.getCity()+"天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime()+ "发布");
        humidityTv.setText("湿度："+todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getHigh()+"~"+todayWeather.getLow());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:"+todayWeather.getFengli());
        //PM25图像更新，要注意有的PM25信息为null
        if(todayWeather.getPm25()!=null){
            int pm25 = Integer.parseInt(todayWeather.getPm25());
            if(pm25<=50){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
            }else if(pm25<=100){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
            }else if(pm25<=150){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
            }else if(pm25<=200){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
            }else if(pm25<=300){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
            }else{
                pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
            }
        }
        //天气图片 注意null
        if(todayWeather.getType()!=null){
            setWeatherImg(weatherImg,todayWeather.getType());
            //String wType = todayWeather.getType();

        }


        //获取更多天气控件
        week1 = (TextView)findViewById(R.id.moreday1xingqi);
        week2 = (TextView)findViewById(R.id.moreday2xingqi);
        week3 = (TextView)findViewById(R.id.moreday3xingqi);
        week4 = (TextView)findViewById(R.id.moreday4xingqi);

        temp1 = (TextView)findViewById(R.id.moreday1temp);
        temp2 = (TextView)findViewById(R.id.moreday2temp);
        temp3 = (TextView)findViewById(R.id.moreday3temp);
        temp4 = (TextView)findViewById(R.id.moreday4temp);

        climate1 = (TextView)findViewById(R.id.moreday1climate);
        climate2 = (TextView)findViewById(R.id.moreday2climate);
        climate3 = (TextView)findViewById(R.id.moreday3climate);
        climate4 = (TextView)findViewById(R.id.moreday4climate);

        fengli1 = (TextView)findViewById(R.id.moreday1fengli);
        fengli2 = (TextView)findViewById(R.id.moreday2fengli);
        fengli3 = (TextView)findViewById(R.id.moreday3fengli);
        fengli4 = (TextView)findViewById(R.id.moreday4fengli);

        weatherImg1 = (ImageView)findViewById(R.id.moreday1img);
        weatherImg2 = (ImageView)findViewById(R.id.moreday2img);
        weatherImg3 = (ImageView)findViewById(R.id.moreday3img);
        weatherImg4 = (ImageView)findViewById(R.id.moreday4img);

        //更新更多天气信息
        week1.setText(todayWeather.getDate1());
        week2.setText(todayWeather.getDate2());
        week3.setText(todayWeather.getDate3());
        week4.setText(todayWeather.getDate4());
        temp1.setText(todayWeather.getHigh1()+"~"+todayWeather.getLow1());
        temp2.setText(todayWeather.getHigh2()+"~"+todayWeather.getLow2());
        temp3.setText(todayWeather.getHigh3()+"~"+todayWeather.getLow3());
        temp4.setText(todayWeather.getHigh4()+"~"+todayWeather.getLow4());
        climate1.setText(todayWeather.getType1());
        climate2.setText(todayWeather.getType2());
        climate3.setText(todayWeather.getType3());
        climate4.setText(todayWeather.getType4());
        fengli1.setText("风力:"+todayWeather.getFengli1());
        fengli2.setText("风力:"+todayWeather.getFengli2());
        fengli3.setText("风力:"+todayWeather.getFengli3());
        fengli4.setText("风力:"+todayWeather.getFengli4());
        if(todayWeather.getType1()!=null){
            setWeatherImg(weatherImg1,todayWeather.getType1());
        }
        if(todayWeather.getType2()!=null){
            setWeatherImg(weatherImg2,todayWeather.getType2());
        }
        if(todayWeather.getType3()!=null){
            setWeatherImg(weatherImg3,todayWeather.getType3());
        }
        if(todayWeather.getType4()!=null){
            setWeatherImg(weatherImg4,todayWeather.getType4());
        }

        Toast.makeText(MainActivity.this,"更新成功！",Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for(int i=0;i<ids.length;i++){
            if(i == position){
                dots[i].setImageResource(R.drawable.page_indicator_focused);
            }
            else{
                dots[i].setImageResource(R.drawable.page_indicator_unfocused );
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}


