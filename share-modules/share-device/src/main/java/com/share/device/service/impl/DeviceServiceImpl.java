package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.device.domain.Cabinet;
import com.share.device.domain.Station;
import com.share.device.domain.StationLocation;
import com.share.device.domain.StationVo;
import com.share.device.service.ICabinetService;
import com.share.device.service.IDeviceService;
import com.share.device.service.IMapService;
import com.share.device.service.IStationService;
import com.share.rule.api.RemoteFeeRuleService;
import com.share.rule.domain.FeeRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DeviceServiceImpl implements IDeviceService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private IStationService stationService;
    @Autowired
    private ICabinetService cabinetService;
    @Autowired
    private IMapService mapService;
    @Autowired
    private RemoteFeeRuleService remoteFeeRuleService;
    @Override
    public List<StationVo> nearbyStation(String latitude, String longitude) {
        //坐标，确定中心点
        // GeoJsonPoint(double x, double y) x 表示经度，y 表示纬度。
        GeoJsonPoint geoJsonPoint = new GeoJsonPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
        //画圈的半径,50km范围
        Distance distance = new Distance(50, Metrics.KILOMETERS);
        //画了一个圆圈
        Circle circle = new Circle(geoJsonPoint, distance);
        //查询mongDB数据//条件排除自己
        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        List<StationLocation> list = mongoTemplate.find(query, StationLocation.class);
        log.info("list:{}", list);
        if (CollectionUtils.isEmpty(list)) return null;

        //获取站点id列表
        List<Long> stationIdList = list.stream()
                // 提取每个位置对应的站点ID
                .map(StationLocation::getStationId)
                .collect(Collectors.toList()); // 收集为List<Long>

        //根据所有站点id获取对应的 站点数据
        LambdaQueryWrapper<Station> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Station::getId,stationIdList).isNotNull(Station::getCabinetId);//柜机编号不为空
        List<Station> stationList = stationService.list(queryWrapper);

        //用stream遍历stationList站点列表,获取柜机id列表
        List<Long> cabinetIdList = stationList.stream()
                .map(Station::getCabinetId)
                .collect(Collectors.toList());

        //获取柜机id与柜机信息Map
        List<Cabinet> cabinetList = cabinetService.listByIds(cabinetIdList);//Collection<? extends Serializable> idList
        Map<Long, Cabinet> cabinetIdToCabinetMap = cabinetList.stream()
                .collect(Collectors.toMap(
                        Cabinet::getId,
                        Cabinet -> Cabinet
                ));

        //List<Station> -- List<StationVo>
        ArrayList<StationVo> stationVoList = new ArrayList<>();
        stationList.forEach(station -> {
            StationVo stationVo = new StationVo();
            BeanUtils.copyProperties(station, stationVo);

            //计算距离
            Double distanceStation = mapService.calculateDistance(longitude, latitude,
                    station.getLongitude().toString(), station.getLatitude().toString());
            stationVo.setDistance(distanceStation);

            //获取柜机信息
            Cabinet cabinet = cabinetIdToCabinetMap.get(station.getCabinetId());
            //Cabinet cabinet = cabinetService.getById(station.getCabinetId());
            //可用充电宝数量大于0，可借用
            if (cabinet.getAvailableNum()>0){
                stationVo.setIsUsable("1");
            }else {
                stationVo.setIsUsable("0");
            }
            // 获取空闲插槽数量大于0，可归还
            if (cabinet.getFreeSlots()>0){
                stationVo.setIsReturn("1");
            }else {
                stationVo.setIsReturn("0");
            }

            //获取站点规则数据
            R<FeeRule> feeRuleResult = remoteFeeRuleService.getFeeRule(station.getFeeRuleId());
            //stationVo.setFeeRule(feeRuleResult.getData().getDescription());
            if (feeRuleResult != null && feeRuleResult.getData() != null) {
                stationVo.setFeeRule(feeRuleResult.getData().getDescription());
            }

            stationVoList.add(stationVo);
        });

        return stationVoList;







    }

    @Override
    public StationVo getStation(Long id, String latitude, String longitude) {
        Station station = stationService.getById(id);
        StationVo stationVo = new StationVo();
        BeanUtils.copyProperties(station, stationVo);
        // 计算距离
        Double distance = mapService.calculateDistance(longitude, latitude, station.getLongitude().toString(), station.getLatitude().toString());
        stationVo.setDistance(distance);

        // 获取柜机信息
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        //可用充电宝数量大于0，可借用
        if(cabinet.getAvailableNum() > 0) {
            stationVo.setIsUsable("1");
        } else {
            stationVo.setIsUsable("0");
        }
        // 获取空闲插槽数量大于0，可归还
        if (cabinet.getFreeSlots() > 0) {
            stationVo.setIsReturn("1");
        } else {
            stationVo.setIsReturn("0");
        }

        // 获取费用规则
        FeeRule feeRule = remoteFeeRuleService.getFeeRule(station.getFeeRuleId()).getData();
        stationVo.setFeeRule(feeRule.getDescription());
        return stationVo;
    }
}
