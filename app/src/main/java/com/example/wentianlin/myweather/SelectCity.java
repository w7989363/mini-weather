package com.example.wentianlin.myweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wentianlin.myweather.app.MyApplication;
import com.example.wentianlin.myweather.bean.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by wentianlin on 2017/10/18.
 */

public class SelectCity extends Activity implements View.OnClickListener{
    private TextView mCurrentCity;
    private ImageView mBackBtn;
    private EditText mSearchEdit;
    private ListView mCityListView;
    private List<City> mCityList;
    private List<Map<String,Object>> mCityMapList;
    private String selectedCityName;
    private String selectedCityCode;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //设置布局
        setContentView(R.layout.select_city);

        //初始化返回按钮
        mBackBtn = (ImageView)findViewById(R.id.title_back);
        //设置返回按钮监听器
        mBackBtn.setOnClickListener(this);

        //初始化查找输入框
        mSearchEdit = (EditText)findViewById(R.id.search_edit);
        //为输入框设置watcher
        mSearchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("MyApp", s.toString());
                updateAdapter(s.toString());
//                if(s.toString().equals("")){
//                    Log.d("MyApp","kong");
//                }
//                else{
//                    Log.d("MyApp", s.toString());
//                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //获取当前城市
        Intent i = getIntent();
        selectedCityName = i.getStringExtra("cityName");

        //初始化控件
        initViews();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_back:
                //向MainActivity发送城市代码
                Intent i = new Intent();
                i.putExtra("cityCode", selectedCityCode);
                i.putExtra("cityName", selectedCityName);
                setResult(RESULT_OK, i);
                finish();
                break;
            default:
                break;
        }

    }

    //初始化控件
    private void initViews(){
        //初始化当前城市控件
        mCurrentCity = (TextView)findViewById(R.id.title_name);
        mCurrentCity.setText("当前城市："+selectedCityName);

        //初始化城市列表
        mCityListView = (ListView)findViewById(R.id.city_list);
        //获取城市信息
        MyApplication myApplication = (MyApplication)getApplication();
        mCityList = myApplication.getCityList();
        //将城市信息转换map
        mCityMapList = new ArrayList<Map<String,Object>>();
        for(City city: mCityList){
            Map<String,Object> item = new HashMap<String,Object>();
            item.put("name",city.getCity());
            item.put("code",city.getNumber());
            mCityMapList.add(item);
            //Log.d("MyApp", item.toString());
        }
        //设置适配器
        updateAdapter("");


        //点击List中的item响应函数
        mCityListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获取父控件List
                ListView listView = (ListView)parent;
                //根据item在ListView中的position获得点击的item
                HashMap<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
                selectedCityName = map.get("name").toString();
                selectedCityCode = map.get("code").toString();
                //发消息给MainActivity
                Intent i = new Intent();
                i.putExtra("cityCode",selectedCityCode);
                i.putExtra("cityName",selectedCityName);
                setResult(RESULT_OK, i);
                finish();
                //Toast.makeText(SelectCity.this,map.get("code").toString(), Toast.LENGTH_LONG).show();
            }
        });



    }

    //更新适配器
    private void updateAdapter(String cityName){
        //没有城市名，显示所有城市
        if(cityName.equals("")){
            SimpleAdapter simplead = new SimpleAdapter(SelectCity.this,mCityMapList,R.layout.item,
                    new String[]{"name","code"},new int[]{R.id.item_city_name,R.id.item_city_code});
            mCityListView.setAdapter(simplead);
        }
        //否则根据城市名构造mapList，并返回simpleAdapter
        else{
            List<Map<String, Object>> ml = new ArrayList<Map<String,Object>>();
            //遍历每一个城市
            for(City city: mCityList){
                //如果城市名相同则加入到mapList
                if(city.getCity().equals(cityName)){
                    Map<String,Object> item = new HashMap<String,Object>();
                    item.put("name",city.getCity());
                    item.put("code",city.getNumber());
                    ml.add(item);
                }
            }
            //遍历结束生成适配器
            SimpleAdapter simplead =  new SimpleAdapter(SelectCity.this,ml,R.layout.item,
                    new String[]{"name","code"},new int[]{R.id.item_city_name,R.id.item_city_code});
            mCityListView.setAdapter(simplead);
        }
    }
}
