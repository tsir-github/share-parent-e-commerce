package com.share.device.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.share.common.core.exception.ServiceException;
import com.share.device.service.IMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements IMapService {

    @Autowired
    private RestTemplate restTemplate;// Web 服务的模板类

    @Value("${tencent.map.key}")
    private String key;

    /**
     * 根据地址关键字计算经纬度坐标
     *
     * @param keyword 地址关键字
     * @return 经纬度坐标对象，包含latitude(纬度)和longitude(经度)字段
     * @throws ServiceException 当地图解析异常时抛出
     */
    @Override
    public JSONObject calculateLatLng(String keyword) {
        String url = "https://apis.map.qq.com/ws/geocoder/v1/?address={address}&key={key}";

        Map<String, String> map = new HashMap<>();
        map.put("address", keyword);
        map.put("key", key);
        // 发送HTTP GET请求获取地理位置信息
        JSONObject response = restTemplate.getForObject(url, JSONObject.class, map);
        if (response.getIntValue("status") != 0) {
            throw new ServiceException("地图解析异常");
        }

        //返回第一条最佳线路
        JSONObject result = response.getJSONObject("result");
        System.out.println(result.toJSONString());
        return result.getJSONObject("location");
    }

    @Override//四个参数：起始经度，起始纬度，结束经度，结束纬度
    public Double calculateDistance(String startLongitude, String startLatitude,
                                    String endLongitude, String endLatitude) {

        String url = "https://apis.map.qq.com/ws/direction/v1/walking/?from={from}&to={to}&key={key}";
        HashMap<String, String> map = new HashMap<>();
        map.put("from", startLatitude + "," + startLongitude);
        map.put("to", endLatitude + "," + endLongitude);
        map.put("key", key);

        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        if(result.getIntValue("status") != 0){
            log.error("地图服务调用失败：{}", result.getString("message"));
            throw new ServiceException("地图服务调用失败");
        }
        //返回第一条最佳线路
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        // 单位：米
        return route.getBigDecimal("distance").doubleValue();
    }

}