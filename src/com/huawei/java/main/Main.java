package com.huawei.java.main;

import util.Check;
import util.ToFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Description 相比Main1，优化了第一轮分配
 * @param
 * @return
 */
public class Main {

    //demandName存储客户节点的名称
    static List<String> demandName = new ArrayList<>();
    //time存储各个时刻
    static List<String> timeList = new ArrayList<>();
    //siteName存储边缘节点的名称
    static List<String> siteName = new ArrayList<>();
    //qos_constraint存储qos的上限值
    static Integer qosLimit;
    //site_bandwidth存储边缘节点的带宽上限，格式为<边缘节点名称，带宽上限>
    static HashMap<String, Integer> site_bandwidth = new HashMap<>();
    //demand存储每个时刻客户节点的带宽需求，格式为<时刻，<客户节点名称，带宽需求>>
    static HashMap<String, HashMap<String, Integer>> demand = new HashMap<>();
    //存储客户节点和边缘节点的qos
    static String[][] qos;
    //demandConnectSite存储客户节点能连接到的边缘节点，格式为<客户节点名称，<边缘节点名称，该边缘节点名称能连接的客户节点数>>
    static HashMap<String, HashMap<String, Integer>> demandConnectSite = new HashMap<>();
    //siteConnectDemand存储边缘节点能连接到的客户节点，格式为<边缘节点名称，<客户节点名称，该客户节点名称能连接的边缘节点数>>
    static HashMap<String, HashMap<String, Integer>> siteConnectDemand = new HashMap<>();
    static HashMap<String, HashMap<String, String>> log = new HashMap<>();
    //siteConnectDemandSum存储边缘节点能连接到的客户节点，格式为<时刻，<边缘节点名称，连接的客户节点的流量总和>>
    static HashMap<String, HashMap<String, Integer>> siteConnectDemandSum = new HashMap<>();
    static String maxValue = "max";

    // ！！！在idea本地跑用这个路径
//    static String demandFile = "data/demand.csv";
//    static String site_bandwidthFile = "data/site_bandwidth.csv";
//    static String qosFile = "data/qos.csv";
//    static String qos_config = "data/config.ini";
//    static String filepath = "output/solution.txt";
//    static String logPath = "output/log.txt";

    // ！！！提交到线上用这个环境
    static String demandFile = "/data/demand.csv";
    static String site_bandwidthFile = "/data/site_bandwidth.csv";
    static String qosFile = "/data/qos.csv";
    static String qos_config = "/data/config.ini";
    static String filepath = "/output/solution.txt";
    static String logPath = "/output/log.txt";

    /**
     * @Description 初始化方法，读入文件并存储到本地
     * @param
     * @return
     */
    public static void init(){
        String line;
        String[] temp;

        //初始化qos的上限值
        try(BufferedReader reader = new BufferedReader(new FileReader(qos_config))) {
            Properties properties = new Properties();
            properties.load(reader);
            for (Object s : properties.keySet()){
                if("qos_constraint".equals(s.toString())){
                    qosLimit = Integer.valueOf(properties.getProperty(s.toString()));
                }
            }
        } catch (IOException e) {
            System.out.println("初始化 qos_constraint 失败");
        }

        //初始化边缘节点
        try(BufferedReader reader = new BufferedReader(new FileReader(site_bandwidthFile))) {
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                if( "site_name".equals(temp[0]) )
                    continue;
                siteName.add(temp[0]);
                site_bandwidth.put(temp[0], Integer.valueOf(temp[1]));
            }
        }
        catch (IOException e) {
            System.out.println("初始化 边缘节点 失败");
        }

        //初始化客户节点
        try(BufferedReader reader = new BufferedReader(new FileReader(demandFile))) {
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                int len = temp.length;
                if( "mtime".equals(temp[0]) ){
                    demandName = transferArrayToList(temp);
                    //第0个元素是“mtime”，移除
                    demandName.remove(0);
                    continue;
                }
                //map存储<客户节点名称，客户节点在此刻需要的带宽>
                HashMap<String, Integer> map = new HashMap<>();
                for (int i = 1; i < len; i++) {
                    map.put(demandName.get(i-1), Integer.valueOf(temp[i]));
                }
                timeList.add(temp[0]);
                //demand存储<时间，map>
                demand.put(temp[0], map);
            }
        } catch (IOException e) {
            System.out.println("初始化 客户节点 失败");
        }

        //初始化 qos二维数组
        qos = new String[siteName.size()+1][demandName.size()+1];
        try(BufferedReader reader = new BufferedReader(new FileReader(qosFile))){
            int row = 0;
            int len;
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                len = temp.length;
                if("site_name".equals(temp[0])){
                    for (int i = 0; i < len; i++) {
                        qos[row][i] = temp[i];
                    }
                }
                else {
                    for (int i = 0; i < len; i++) {
                        if(i > 0)
                            qos[row][i] = (Integer.parseInt(temp[i]) < qosLimit) ? "1" : "0";
                        else
                            qos[row][i] = temp[i];
                    }
                }
                row++;
            }
        } catch (IOException e) {
            System.out.println("初始化 qos二维数组 失败");
        }

        //初始化siteConnectNum，demandConnectNum，siteConnectDemand，demandConnectSite
        HashMap<String, Integer> siteConnectNum = new HashMap<>();
        HashMap<String, Integer> demandConnectNum = new HashMap<>();
        for (String siteName : siteName) {
            for (int i = 1; i < qos.length; i++) {
                if (qos[i][0].equals(siteName)) {
                    int n = 0;
                    for (int j = 1; j < qos[i].length; j++) {
                        n += Integer.parseInt(qos[i][j]);
                    }
                    siteConnectNum.put(siteName, n);
                    break;
                }
            }
        }

        for (String name : demandName) {
            for (int j = 1; j < qos[0].length; j++) {
                if(qos[0][j].equals(name)){
                    int n = 0;
                    for (int i = 1; i < qos.length; i++) {
                        n += Integer.parseInt(qos[i][j]);
                    }
                    demandConnectNum.put(name, n);
                    break;
                }
            }
        }

        for (String siteName : siteName) {
            for (int i = 0; i < qos.length; i++) {
                if (qos[i][0].equals(siteName)) {
                    HashMap<String, Integer> map = new HashMap<>();
                    for (int j = 1; j < qos[i].length; j++) {
                        if("1".equals(qos[i][j])){
                            map.put(qos[0][j], demandConnectNum.get(qos[0][j]));
                        }
                    }
                    siteConnectDemand.put(siteName, map);
                    break;
                }
            }
        }

        for (String demandName : demandName) {
            for (int j = 0; j < qos[0].length; j++) {
                if(qos[0][j].equals(demandName)){
                    HashMap<String, Integer> map = new HashMap<>();
                    for (int i = 1; i < qos.length; i++){
                        if("1".equals(qos[i][j])){
                            map.put(qos[i][0], siteConnectNum.get(qos[i][0]));
                        }
                    }
                    demandConnectSite.put(demandName, map);
                    break;
                }
            }
        }

        //初始化siteConnectDemandSum
        updateSiteConnectDemandSum(timeList, siteName);
    }

    /**
     * @Description 将数组转换成List。为什么写该方法：因为Arrays.asList转换后的List不支持增删
     * @param
     * @return
     */
    public static List<String> transferArrayToList(String[] array){
        List<String> temp = new ArrayList<>();
        Arrays.stream(array).forEach(temp::add);
        return temp;
    }

    public static void updateSiteConnectDemandSum(List<String> timeList, List<String> siteName){

        for (String time : timeList){
            HashMap<String, Integer> map = new HashMap<>();
            for(String site : siteName){
                if(siteConnectDemand.getOrDefault(site , null) != null){
                    List<Integer> values = new ArrayList<>();
                    for (Map.Entry<String, Integer> entry : siteConnectDemand.get(site).entrySet()){
                        values.add( demand.get(time).get(entry.getKey()) );
                    }
                    Integer sum = values.stream().mapToInt(Integer::intValue).sum();
                    map.put(site, sum);
                }
            }
            siteConnectDemandSum.put(time, map);
        }
    }

    /**
     * @Description 每个时刻，执行一次调度
     * @param
     * @return
     */
    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatch(){
        //每个时刻客户节点的带宽需求，格式为<时刻，<客户节点，带宽需求>>
        HashMap<String, HashMap<String, Integer>> demand_copy = Check.MyClone(demand);
        //记录每个时刻，边缘节点剩余的带宽,格式为<时刻，<边缘节点，剩余带宽>>
        HashMap<String, HashMap<String, Integer>> timeSiteBandWidth = new HashMap<>();
        //初始化边缘节点的带宽
        for(String time : timeList){
            timeSiteBandWidth.put(time, Check.MyClone(site_bandwidth));
        }
        //每个边缘节点可以高负载的天数
        int day = (int)(timeList.size() * 0.05);
        HashMap<String, Integer> fullLoadDays = new HashMap<>();
        //记录每个边缘节点满负载的时刻，格式为<时刻，<边缘节点，剩余带宽>>
        HashMap<String, HashMap<String, Integer>> fullLoadTime = new HashMap<>();
        for(String time : timeList){
            HashMap<String, Integer> temp = new HashMap<>();
            for(String site : siteName){
                temp.put(site, 0);
            }
            fullLoadTime.put(time, temp);
        }
        for(String site : siteName){
            fullLoadDays.put(site, day);
            // Key:边缘节点+"allTime",value:该边缘节点的最高带宽使用量
            fullLoadDays.put(site+maxValue, 0);
        }

        //第一轮分配的分配方案，格式是<时间, <边缘节点，<客户节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result1 = dispatchFirst(timeSiteBandWidth, demand_copy, fullLoadTime, fullLoadDays);

        //第二轮分配的分配方案,格式是<时间, <客户节点，<边缘节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result2 = dispatchSecond(timeSiteBandWidth, demand_copy, fullLoadTime, fullLoadDays);

        //因为result1和result2的格式不同，要统一转化为result2的那种格式
        return ToFile.trans(result1, result2, siteName, demandName, timeList);
    }

    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchFirst(
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays){

        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>();

        //每轮时间，最多有countLimit个边缘节点达到高负载
        int countLimit = (int)(siteName.size() * 0.05);

        //第一轮分配方案
        for (String time : timeList) {

            //根据连接的客户节点的流量总和大小来排序
            List<Map.Entry<String, Integer>> siteList = new ArrayList<>(siteConnectDemandSum.get(time).entrySet());
            siteList.sort((o1, o2) -> o2.getValue()-o1.getValue());
//            siteList.sort((o1, o2) -> o1.getValue()-o2.getValue());

            //map存分配方案,注意格式是<边缘节点，<客户节点，分配的流量>>
            HashMap<String, HashMap<String, Integer>> map = new HashMap<>();
            int count = 0;

            for(Map.Entry<String, Integer> entry : siteList){
                if(count >= countLimit)
                    break;

                String site = entry.getKey();
                //该节点没有高负载的次数了
                if(fullLoadDays.get(site) <= 0)
                    continue;

                //demandNeed存储能连接的客户节点的带宽需求
                HashMap<String, Integer> demandNeed = Check.MyClone(siteConnectDemand.get(site));
                //该边缘节点是个死节点，连接不到任何客户节点
                if(demandNeed.size() == 0)
                    continue;
                //将连接数更新为带宽
                demandNeed.replaceAll((d, v) -> demand_copy.get(time).get(d));
                //needSum表示所有客户节点的需求总和
                Integer needSum = demandNeed.values().stream().mapToInt(Integer::intValue).sum();

                //remainBandWidth记录边缘节点的剩余带宽
                Integer remainBandWidth = timeSiteBandWidth.get(time).get(site);

                //边缘节点在当前能满负载
                if (needSum >= /*10000*/ remainBandWidth*0.3) {
                    //hashMap存储分配的流量，格式和上面的map对应，<客户节点，分配的流量>
                    HashMap<String, Integer> hashMap = new HashMap<>();

                    List<Map.Entry<String, Integer>> entryList = new ArrayList<>(demandNeed.entrySet());
                    //将客户节点按带宽需求从大到小排序
                    entryList.sort(((o1, o2) -> o2.getValue() - o1.getValue()));
//                    entryList.sort(((o1, o2) -> o1.getValue() - o2.getValue()));

                    for (Map.Entry<String, Integer> demandEntry : entryList) {
                        if (remainBandWidth == 0)
                            break;
                        if(demandEntry.getValue() == 0)
                            continue;

                        //curDispatch记录该客户节点本次分配的流量
                        int curDispatch;
                        //将该客户节点的流量都分配给该边缘节点
                        if (remainBandWidth > demandEntry.getValue()) {
                            curDispatch = demandEntry.getValue();
                            remainBandWidth -= demandEntry.getValue();
                        }
                        //将该客户节点分配remainBandWidth带宽给该边缘节点
                        else {
                            curDispatch = remainBandWidth;
                            remainBandWidth = 0;
                        }

                        //更新客户节点的流量需求
                        demand_copy.get(time).put(demandEntry.getKey(), demandEntry.getValue()-curDispatch);
                        //更新分配方案
                        hashMap.put(demandEntry.getKey(), curDispatch);
                    }
                    //当前时刻高负载次数加一
                    count++;
                    //当前时刻当前边缘节点高负载，做标记
                    fullLoadTime.get(time).put(site, 1);
                    fullLoadDays.put(site, fullLoadDays.get(site)-1);
                    //更新该边缘节点在所有时刻中的带宽最大值
                    fullLoadDays.put(site+maxValue, Math.max(fullLoadDays.get(site+maxValue), timeSiteBandWidth.get(time).get(site)-remainBandWidth));
                    //更新边缘节点的带宽
                    timeSiteBandWidth.get(time).put(site, remainBandWidth);
                    //更新siteConnectDemandSum
                    updateSiteConnectDemandSum(Collections.singletonList(time), Collections.singletonList(site));

                    if(hashMap.size() != 0)
                        map.put(site, hashMap);
                }
            }
            result.put(time, map);
        }
        return result;
    }


    private static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchSecond(
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays) {

        //第二轮分配的分配方案,格式是<时间, <客户节点，<边缘节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>();

        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demand_copy.get(time).entrySet());

            double rate = 0.05;
            double before_rate = 0;
            //创建最大可用带宽
            HashMap<String, Integer> siteWithMaxUseAbleBand = new HashMap<>();

            //分配策略
            HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();
            //信号量
            boolean signal = false;

            while ( !signal ){
                siteWithMaxUseAbleBand.clear();
                for(Map.Entry<String, Integer> site : timeSiteBandWidth.get(time).entrySet()){
                    int a = site.getValue();
                    int b = (int)(site_bandwidth.get(site.getKey()) * (rate-before_rate));
                    if(b < a){
                        //这部分流量先分配给siteWithMaxUseAbleBand，如果siteWithMaxUseAbleBand有剩余，
                        //还会将剩余带宽更新回timeSiteBandWidth
                        site.setValue(site.getValue() - b);
                        siteWithMaxUseAbleBand.put(site.getKey(), b);
                    }
                    else
                        siteWithMaxUseAbleBand.put(site.getKey(), a);
                }

                /*//---------------检测结果正确性-----------------
                Integer demandNeedSum = 0;
                for(Map.Entry<String, Integer> entry: demandList){
                    demandNeedSum += entry.getValue();
                }
                System.out.println("分配前客户节点总需求: " + demandNeedSum);
                Integer siteSum;
                siteSum = siteWithMaxUseAbleBand.values().stream().mapToInt(Integer::intValue).sum();
                System.out.println("分配前边缘节点总带宽: " + siteSum);
                //---------------------------------------------*/

                signal = dispatchBasedDemandClientAndUsedBandSite(demandList, siteWithMaxUseAbleBand, dispatchStrategy, fullLoadTime, fullLoadDays, time);

                /*//---------------检测结果正确性-----------------
                Integer demandNeedSum1 = 0;
                for(Map.Entry<String, Integer> entry: demandList){
                    demandNeedSum1 += entry.getValue();
                }
                System.out.println("分配后客户节点总需求: " + demandNeedSum1);
                System.out.println("这一轮总共分配出去" + (demandNeedSum-demandNeedSum1) + "带宽");
                Integer siteSum1;
                siteSum1 = siteWithMaxUseAbleBand.values().stream().mapToInt(Integer::intValue).sum();
                System.out.println("分配后边缘节点总带宽: " + siteSum1);
                System.out.println("这一轮总共承担了" + (siteSum-siteSum1) + "带宽");
                //---------------------------------------------*/

                //将siteWithMaxUseAbleBand更新回timeSiteBandWidth
                for (String site : siteWithMaxUseAbleBand.keySet()){
                    //带宽还有剩余没用掉
                    if(siteWithMaxUseAbleBand.get(site) > 0){
                        int temp = timeSiteBandWidth.get(time).get(site) + siteWithMaxUseAbleBand.get(site);
                        timeSiteBandWidth.get(time).put(site, temp);
                    }
                }
                before_rate = rate;
//                rate = rate*1.1;
                rate += 0.05;
                rate = (rate>=1) ? 1 : rate;
                /*System.out.println(rate);
                System.out.println();*/
            }

            /*//---------------检测结果正确性-----------------
            //            int dsum = demand_copy.get(time).values().stream().mapToInt(Integer::intValue).sum();
            //            System.out.println("该时刻客户节点的总需求为:" +dsum);
            //            int starSum = 0;
            //            for(String name : demand_copy.get(time).keySet()){
            //                starSum += dispatchStrategy.get(name).values().stream().mapToInt(Integer::intValue).sum();
            //            }
            //            System.out.println("分配策略总共分配了" + starSum +"带宽");
            //
            //            //--------------------------------------------*/

            result.put(time, dispatchStrategy);
        }
        return result;
    }

    public static boolean dispatchBasedDemandClientAndUsedBandSite(
            List<Map.Entry<String, Integer>> demandList,
            HashMap<String, Integer> siteWithMaxUseAbleBand,
            HashMap<String, HashMap<String, Integer>> dispatchStrategy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays,
            String time){

        //定义一个信号，如果分配过程有溢出，变成false
        boolean sign = true;

        //连接数少的先满足
        demandList.sort((Comparator.comparingInt(o -> demandConnectSite.getOrDefault(o.getKey(), new HashMap()).size())));

        for (Map.Entry<String, Integer> entry : demandList){
            //客户节点名称
            String curClient = entry.getKey();
            //客户节点带宽需求
            int curDemand = entry.getValue();

            HashMap<String, Integer> siteMap = new HashMap<>( demandConnectSite.get(curClient) );
            //siteList存储客户节点能连接的边缘节点
            List<String> siteList = new ArrayList<>(siteMap.keySet());
            //按边缘节点的剩余带宽从大到小排序
            siteList.sort((o1, o2) -> siteWithMaxUseAbleBand.get(o2)-siteWithMaxUseAbleBand.get(o1));

            //存储权重信息
            HashMap<String, HashMap<String, BigDecimal>> weightMap = new HashMap<>();
            BigDecimal weightSum = new BigDecimal("0");
            for (String siteName : siteMap.keySet()){
                HashMap<String, BigDecimal> temp = new HashMap<String, BigDecimal>(){{
                    //边缘节点的带宽的 剩余 带宽容量
                    put("capacity", BigDecimal.valueOf( siteWithMaxUseAbleBand.get(siteName) ));
                    put("connect", BigDecimal.valueOf( siteMap.get(siteName) ));
                }};
                weightSum = weightSum.add( temp.get("capacity").divide(temp.get("connect"), 5, RoundingMode.CEILING) );
                weightMap.put(siteName, temp);
            }

            //存储这一轮的边缘节点的分配情况
            HashMap<String, Integer> map = dispatchStrategy.getOrDefault(curClient, new HashMap<>());

            //遍历边缘节点
            for(String site : siteList){
                if(curDemand == 0)
                    break;

                //resband表示当前site的剩余带宽
                int resband = siteWithMaxUseAbleBand.get(site);
                //当前边缘节点已经无带宽可承担，跳过该节点
                if(resband == 0)
                    continue;
                //beforResband记录分配前还剩下多少带宽
                int beforResband = resband;

                //该边缘节点在第一轮分配时已经分配过了，那就使该边缘节点尽可能高负载，把能分配的流量都给它
                //或者该边缘节点的满负载天数还有剩余，将全部的带宽分配给它
                if(fullLoadTime.get(time).get(site) == 1 || fullLoadDays.get(site) > 0){
                    int alreadyDispatch;
                    if(curDemand > resband){
                        alreadyDispatch = resband;
                        curDemand -= resband;
                        resband = 0;
                    }
                    else {
                        alreadyDispatch = curDemand;
                        resband -= curDemand;
                        curDemand = 0;
                    }
                    map.put(site, map.getOrDefault(site, 0) + alreadyDispatch);
                    siteWithMaxUseAbleBand.put(site, resband);
                    if(fullLoadTime.get(time).get(site) == 0){
                        fullLoadDays.put(site, fullLoadDays.get(site)-1);
                        fullLoadTime.get(time).put(site, 1);
                    }
                    //该边缘节点分配结束，进入下一个边缘节点
                    continue;
                }

                //到这里，说明该边缘节点在该时刻不处于高负载的状态，进入均匀分配的步骤

                //权重计算
                BigDecimal numerator = weightMap.get(site).get("capacity").divide(weightMap.get(site).get("connect"), 5, RoundingMode.FLOOR);
                BigDecimal weight = numerator.divide(weightSum, 5, RoundingMode.FLOOR);
                int curDispatch = weight.multiply(BigDecimal.valueOf(entry.getValue())).setScale(0, BigDecimal.ROUND_DOWN).intValue();

                //防止分配出去的带宽 超过 能分配的带宽
                if(curDispatch > curDemand){
                    curDispatch = curDemand;
                    curDemand = 0;
                }
                else {
                    curDemand -= curDispatch;
                }

                //当前边缘节点的剩余带宽大于客户节点的带宽需求，全部放到当前边缘节点
                if(resband >= curDispatch){
                    resband -= curDispatch;
                    curDispatch = 0;
                }
                //当前边缘节点的剩余带宽小于客户节点的带宽需求，先放能放下的部分，继续放下一个边缘节点
                else{
                    curDispatch -= resband;
                    resband = 0;
                }

                //没分配出去的部分加回到curDemand
                curDemand += curDispatch;
                //记录剩余带宽
                siteWithMaxUseAbleBand.put(site, resband);
                map.put(site, map.getOrDefault(site, 0) + beforResband-resband);
            }

            if (curDemand > 0){
                for(String siteName : siteList){
                    Integer remainBandWidth = siteWithMaxUseAbleBand.get(siteName);
                    if(curDemand == 0)
                        break;
                    if(remainBandWidth == 0)
                        continue;

                    if(remainBandWidth >= curDemand){
                        remainBandWidth -= curDemand;
                        curDemand = 0;
                    }
                    else {
                        curDemand -= remainBandWidth;
                        remainBandWidth = 0;
                    }
                    map.put(siteName, map.getOrDefault(siteName, 0) + siteWithMaxUseAbleBand.get(siteName)-remainBandWidth);
                    siteWithMaxUseAbleBand.put(siteName, remainBandWidth);
                }
            }
            entry.setValue(curDemand);
            dispatchStrategy.put(curClient, map);

            if(curDemand > 0){
                sign = false;
            }
        }
        return sign;
    }

    public static void main(String[] args) {
        init();
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = dispatch();
        ToFile.writeToFile(filepath, timeList, demandName, result);
//        ToFile.writeLog(logPath, log);

        //对最终的分配方案做校验
//        Check.check_1(demand, demandName, timeList, siteName, result);
//        Check.check_2(site_bandwidth, demandName, timeList, siteName, result);
    }
}