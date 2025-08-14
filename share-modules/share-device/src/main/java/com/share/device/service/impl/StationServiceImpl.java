package com.share.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.web.domain.AjaxResult;
import com.share.device.domain.Cabinet;
import com.share.device.domain.Station;
import com.share.device.domain.StationLocation;
import com.share.device.domain.dto.FeeRuleDTO;
import com.share.device.feign.FeeRuleClient;
import com.share.device.mapper.CabinetMapper;
import com.share.device.mapper.StationMapper;
import com.share.device.repository.StationLocationRepository;
import com.share.device.service.ICabinetService;
import com.share.device.service.IRegionService;
import com.share.device.service.IStationService;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService {
    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private StationLocationRepository stationLocationRepository;
    @Autowired
    private IRegionService regionService;

    @Autowired
    private ICabinetService cabinetService;

    @Autowired
    private FeeRuleClient feeRuleClient; // 注入Feign客户端
    //@Autowired
    //private IFeeRuleService feeRuleService;
    /*@Autowired
    private CabinetMapper cabinetMapper;*/

    @Override
    public List<Station> selectStationList(Station station) {
        //------------第三种方法双集合批量提取，性能最高-------------------------------------------------------------------------------------------
        List<Station> stationList = stationMapper.selectStationList(station);//拿到所有站点数据列表
        // 1. 批量获取柜机ID和费用规则ID
        HashSet<Long> cabinetIds = new HashSet<>();// HashSet自动去重
        HashSet<Long> feeRuleIds = new HashSet<>();// HashSet自动去重
        stationList.forEach(item -> {//遍历站点数据列表，如果满足条件就把柜机ID和费用规则ID添加到HashSet集合中
            if (item.getCabinetId() != null && !item.getCabinetId().equals("")) {
                cabinetIds.add(item.getCabinetId());
            }
            if (item.getFeeRuleId() != null && !item.getFeeRuleId().equals("")) {
                feeRuleIds.add(item.getFeeRuleId());
            }
        });
        // 2. 批量查询柜机信息（空集合保护）
        /*HashMap<Long, String> cabinetNoMap = new HashMap<>();
        if(!cabinetIds.isEmpty()){
            Cabinet cabinetQuery  = new Cabinet();
            cabinetQuery.setIds(new ArrayList<>(cabinetIds));
            List<Cabinet> cabinetList = cabinetService.selectCabinetList(cabinetQuery);
            cabinetList.stream()
                    .forEach(cabinet -> cabinetNoMap.put(
                            cabinet.getId(),
                            cabinet.getCabinetNo()
                    ));
        }*/
        HashMap<Long, String> cabinetNoMap = new HashMap<>();
        /// 空集合保护 - 避免空集合查询
        if (!cabinetIds.isEmpty()) {
            List<Cabinet> cabinetList = cabinetService.listByIds(new ArrayList<>(cabinetIds));
            cabinetList.stream()
                    .forEach(cabinet -> cabinetNoMap.put(
                            cabinet.getId(),
                            cabinet.getCabinetNo()
                    ));
        }
        // 3. 批量查询费用规则（示例方法，需实现）
        HashMap<Long, String> feeRuleMap = new HashMap<>();
        if (!feeRuleIds.isEmpty()) {
            // 调用Feign客户端
            AjaxResult result = feeRuleClient.batchGet(new ArrayList<>(feeRuleIds));
            // 调用规则服务的批量查询接口
            //List<FeeRuleDTO> feeRuleList = feeRuleService.selectByIds(new ArrayList<>(feeRuleIds));
            // 异常降级处理
            if (result.isSuccess()){
                List<Map<String, Object>> feeRules = (List<Map<String, Object>>) result.get("data");
                feeRules.forEach(rule -> {
                    FeeRuleDTO dto = FeeRuleDTO.fromMap(rule);
                    feeRuleMap.put(dto.getId(), dto.getDescription());
                });
            } else {
                log.error("获取费用规则失败: {}", new Throwable(String.valueOf(result.get("msg"))));
                // 添加降级处理
                feeRuleIds.forEach(id -> feeRuleMap.put(id, "默认规则"));
            }

            /*feeRuleList.stream()
                    .forEach(feeRule -> feeRuleMap.put(
                            feeRule.getId(),
                            feeRule.getName()
                    ));*/
        }
        // 4. 并行流设置关联字段,目的是给所有站点数据列表中添加柜机编号和费用规则名称
        //// 使用parallelStream()并行处理数据组装，提升性能
        stationList.parallelStream()//使用getOrDefault提供默认值，避免空指针
                .forEach(item -> {
                    item.setCabinetNo(cabinetNoMap.getOrDefault(item.getCabinetId(), "N/A"));
                    item.setFeeRuleName(feeRuleMap.getOrDefault(item.getFeeRuleId(), "默认规则"));
                });
        return stationList;


        //------------第二种方法用批量查询优化性能，但是不完善--------------------------------------------------------------------------------------------------
        /*List<Station> stationList = stationMapper.selectStationList(station);//拿到所有站点数据列表
        //遍历stationList站点数据列表，获取每个station站点里的 柜机编号cabinetIds列表
        List<Long> cabinetIds = stationList.stream()
                .filter(item -> item.getCabinetId() != null)//排除柜机编号为null的站点
                .map(Station::getCabinetId)
                .collect(Collectors.toList());

        Cabinet query = new Cabinet();
        query.setIds(cabinetIds); //Cabinet 类有 setIds 方法接收 List<Long>
        List<Cabinet> cabinets = cabinetService.selectCabinetList(query);//根据柜机编号列表 查询 柜机数据列表
        //List<Cabinet> cabinetList = cabinetService.listByIds(cabinetIds);
        Map<Long, String> cabinetNoMap = cabinets.stream()// 遍历柜机列表
                .collect(Collectors.toMap(
                        Cabinet::getId,// 以柜机ID作为键
                        Cabinet::getCabinetNo)// 以柜机编号作为值
                );
        //目的是给所有站点数据列表中添加柜机编号
        stationList.forEach(item -> {
            if (item.getCabinetId() != null) {
                item.setCabinetNo(cabinetNoMap.get(item.getCabinetId()));
            }
        });
        return stationList;*/


        //-----------第一种方法，性能差循环查询数据库-------------------------------------------------------------------------------------------------------
        /*//获取每个station里的柜机编号，封装到每个对象里面
        //当前station里面只有柜机id，根据柜机id查询柜机编号
        list.forEach(item -> {
            Long cabinetId = item.getCabinetId();
            if (cabinetId != null){
                item.setCabinetNo(cabinetService.getById(cabinetId).getCabinetNo());
            }
        });
        return list;,
        */


    }

    //添加
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveStation(Station station) {
        String provinceName = regionService.getNameByCode(station.getProvinceCode());
        String cityName = regionService.getNameByCode(station.getCityCode());
        String districtName = regionService.getNameByCode(station.getDistrictCode());
        station.setFullAddress(provinceName + cityName + districtName + station.getAddress());

        //同步站点位置信息到MongoDB
        StationLocation stationLocation = new StationLocation();
        stationLocation.setId(ObjectId.get().toString());//设置唯一 ID（使用 ObjectId.get().toString() 生成）。
        stationLocation.setStationId(station.getId());
        stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));//设置站点 ID、经纬度（从 station 对象获取并转为 GeoJsonPoint）、创建时间。
        stationLocation.setCreateTime(new Date());

        stationLocationRepository.save(stationLocation);//通过 StationLocationRepository 接口操作 MongoDB，该接口继承了 MongoRepository，提供了对 StationLocation 实体的 CRUD 方法。
        return stationMapper.insert(station);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateStation(Station station) {
        String provinceName = regionService.getNameByCode(station.getProvinceCode());
        String cityName = regionService.getNameByCode(station.getCityCode());
        String districtName = regionService.getNameByCode(station.getDistrictCode());
        station.setFullAddress(provinceName + cityName + districtName + station.getAddress());

        //同步站点位置信息到MongoDB
        StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());
        if (stationLocation == null) {
            // 新建位置记录
            stationLocation = new StationLocation();
            stationLocation.setId(ObjectId.get().toString());
            stationLocation.setStationId(station.getId());
            stationLocation.setCreateTime(new Date());
        }
        stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));

        stationLocationRepository.save(stationLocation);
        return stationMapper.updateById(station);
    }

    @Override
    public int setData(Station station) {
        this.updateById(station);

        //更正柜机使用状态
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        cabinet.setStatus("1");
        cabinetService.updateById(cabinet);
        return 1;
    }

    //将数据库数据更新到MongoDB
    @Override
    public void updateData() {
        List<Station> stationList = this.list(); // stationMapper.selectList(null) 或 super.list()。
        for (Station station : stationList) {
            StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());//根据站点ID从MongoDB中获取对应的站点位置信息。
            if (stationLocation == null) {
                stationLocation = new StationLocation();
                stationLocation.setId(ObjectId.get().toString());
                stationLocation.setStationId(station.getId());
                stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
                stationLocation.setCreateTime(new Date());
                stationLocationRepository.save(stationLocation);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeByIds(Collection<?> list) {//遍历传入的 ID 列表
        for (Object id : list) {
            //对每个 ID，
            // 调用 stationLocationRepository.deleteByStationId() 删除与该站点相关的‘地理位置信息’ ；
            stationLocationRepository.deleteByStationId(Long.parseLong(id.toString()));
        }
        //最后调用父类的 super.removeByIds(list) 删除站点本身
        return super.removeByIds(list);
    }

}
