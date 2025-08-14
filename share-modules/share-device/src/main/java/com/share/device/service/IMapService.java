package com.share.device.service;


import com.alibaba.fastjson.JSONObject;

public interface IMapService {

    JSONObject calculateLatLng(String keyword);
    //计算到门店的距离方法
    Double calculateDistance(String startLongitude,String startLatitude,String endLongitude,String endLatitude);

}