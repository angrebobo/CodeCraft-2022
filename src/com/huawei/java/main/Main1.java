package com.huawei.java.main;

import util.Check;
import util.ToFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @Description 相比Main1，优化了第一轮分配
 * @param
 * @return
 */
public class Main1 {

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
    static String demandFile = "data/demand.csv";
    static String site_bandwidthFile = "data/site_bandwidth.csv";
    static String qosFile = "data/qos.csv";
    static String qos_config = "data/config.ini";
    static String filepath = "output/solution.txt";
    static String logPath = "output/log.txt";

    // ！！！提交到线上用这个环境
//    static String demandFile = "/data/demand.csv";
//    static String site_bandwidthFile = "/data/site_bandwidth.csv";
//    static String qosFile = "/data/qos.csv";
//    static String qos_config = "/data/config.ini";
//    static String filepath = "/output/solution.txt";
//    static String logPath = "/output/log.txt";

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
        for (String time : timeList){
            HashMap<String, Integer> map = new HashMap<>();
            for(String site : siteName){
                HashMap<String, Integer> temp1 = siteConnectDemand.getOrDefault(site , null);
                if(temp1 != null){
                    List<Integer> values = new ArrayList<>();
                    for (Map.Entry<String, Integer> entry : temp1.entrySet()){
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
     * @Description 将数组转换成List。为什么写该方法：因为Arrays.asList转换后的List不支持增删
     * @param
     * @return
     */
    public static List<String> transferArrayToList(String[] array){
        List<String> temp = new ArrayList<>();
        Arrays.stream(array).forEach(temp::add);
        return temp;
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

            List<String> siteList = new ArrayList<>(siteName);
            HashMap<String, Integer> demandNeed = Check.MyClone(siteConnectDemand);
            //该边缘节点是个死节点，连接不到任何客户节点
            if(siteConnectDemand.getOrDefault(site, null) == null){
                continue;
            }
            for(String demand : demandName){
                if(!siteConnectDemand.get(site).containsKey(demand))
                    demandNeed.remove(demand);
            }
            //needSum表示所有客户节点的需求总和
            Integer needSum = demandNeed.values().stream().mapToInt(Integer::intValue).sum();



            HashMap<String, Integer> needMap = siteConnectDemandSum.get(time);
            //根据连接的客户节点的流量总和大小来排序
            List<Map.Entry<String, Integer>> entryList1 = new ArrayList<>(needMap.entrySet());
            entryList1.sort((o1, o2) -> o2.getValue()-o1.getValue());
            //map存分配方案,注意格式是<边缘节点，<客户节点，分配的流量>>
            HashMap<String, HashMap<String, Integer>> map = new HashMap<>();
            int count = 0;

            for(Map.Entry<String, Integer> sortedEntry : entryList1){
                if(count >= countLimit)
                    break;
                String site = sortedEntry.getKey();
                //该节点没有高负载的次数了
                if(fullLoadDays.get(site) <= 0)
                    continue;

                //demandNeed存储能连接的客户节点的带宽需求
                HashMap<String, Integer> demandNeed = Check.MyClone(siteConnectDemand.get(site));
                //该边缘节点是个死节点，连接不到任何客户节点
                if(demandNeed.size() == 0)
                    continue;
                for(String demand : demandNeed.keySet()){
                    demandNeed.put(demand, demand_copy.get(time).get(demand));
                }
                //needSum表示所有客户节点的需求总和
                Integer needSum = demandNeed.values().stream().mapToInt(Integer::intValue).sum();
                System.out.println(time+"_"+site+"_"+needSum);
                //remainBandWidth记录边缘节点的剩余带宽
                Integer remainBandWidth = timeSiteBandWidth.get(time).get(site);

                //边缘节点在当前能满负载 并且 满负载天数还有剩余
                if (needSum >= 10000) {
                    //hashMap存储分配的流量，格式和上面的map对应，<客户节点，分配的流量>
                    HashMap<String, Integer> hashMap = new HashMap<>();
                    List<Map.Entry<String, Integer>> entryList = new ArrayList<>(demandNeed.entrySet());
                    //将客户节点按带宽需求从大到小排序
//                    entryList.sort(((o1, o2) -> o2.getValue() - o1.getValue()));
                    entryList.sort(((o1, o2) -> o1.getValue() - o2.getValue()));

                    for (Map.Entry<String, Integer> entry : entryList) {
                        if (remainBandWidth == 0)
                            break;
                        if(entry.getValue() == 0)
                            continue;

                        //将该客户节点的流量都分配给该边缘节点
                        if (remainBandWidth > entry.getValue()) {
                            remainBandWidth -= entry.getValue();
                            //更新客户节点的流量需求
                            demand_copy.get(time).put(entry.getKey(), 0);
                            hashMap.put(entry.getKey(), entry.getValue());
                        } else {
                            demand_copy.get(time).put(entry.getKey(), entry.getValue() - remainBandWidth);
                            hashMap.put(entry.getKey(), remainBandWidth);
                            remainBandWidth = 0;
                        }
                    }
                    //当前时刻当前边缘节点高负载，做标记
                    fullLoadTime.get(time).put(site, 1);
                    fullLoadDays.put(site, fullLoadDays.get(site)-1);
                    fullLoadDays.put(site+maxValue, Math.max(fullLoadDays.get(site+maxValue), timeSiteBandWidth.get(time).get(site)-remainBandWidth));
                    //更新边缘节点的带宽
                    timeSiteBandWidth.get(time).put(site, remainBandWidth);
                    if(hashMap.size() != 0)
                        map.put(site, hashMap);
                    count++;
                }
            }
            result.put(time, map);
        }
        /*for (String time : timeList){
            System.out.println(time);
            System.out.println(result.get(time));
        }*/
        return result;
    }

    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchSecond(
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays){

        //第二轮分配的分配方案,格式是<时间, <客户节点，<边缘节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result2 = new HashMap<>();

        double rate = 0.00002;
        while (!dispatchStrategy(rate, result2,demand_copy, timeSiteBandWidth,fullLoadTime, fullLoadDays)){
            rate = rate*1.1;
            rate = rate>=1?1:rate;
            result2 = new HashMap<>();
            System.out.println(rate);
        }

        return result2;
    }

    private static boolean dispatchStrategy(
            double rate,
            HashMap<String, HashMap<String, HashMap<String, Integer>>> result,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays) {

        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            HashMap<String, Integer> demandMap = demand_copy.get(time);
            //需求转为list用于遍历
            HashMap<String, Integer> demandMap_copy = Check.MyClone(demandMap);
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap_copy.entrySet());

            //剩余当前节点带宽
            HashMap<String, Integer> resSiteBand = timeSiteBandWidth.get(time);
            //site_bandwidth是带宽上限 ,
            //创建最大可用带宽
            HashMap<String, Integer> siteWithMaxUseableBand = new HashMap<>();
            for(Map.Entry<String, Integer> B :resSiteBand.entrySet()){
                siteWithMaxUseableBand.put(B.getKey(),Math.min(B.getValue(),(int)(site_bandwidth.get(B.getKey())*rate)));
            }
            //分配策略
            HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();
            if(!dispatchBasedDemandClientAndUsedBandSite(demandList,siteWithMaxUseableBand,dispatchStrategy,fullLoadTime, fullLoadDays)){
                return false;
            }
            result.put(time, dispatchStrategy);
        }
        return true;
    }


    public static boolean dispatchBasedDemandClientAndUsedBandSite(
            List<Map.Entry<String, Integer>> demandlist,
            HashMap<String, Integer> siteWithMaxUseableBand,
            HashMap<String, HashMap<String, Integer>> dispatchStrategy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays){
        //dispatchStrategy存储最终的分配方案
//        System.out.println(demandlist);
        //定义一个信号，如果分配过程有溢出，变成false
        boolean sign = true;

        //复制一份 节点-剩余容量 map,因为每个时间节点开始都是满的，所以每次都直接复制最大值*rate。
        HashMap<String, Integer> site_bandwidth_copy = new HashMap<>(siteWithMaxUseableBand);
//        System.out.println(site_bandwidth_copy);
        //连接数少的先满足
        demandlist.sort((Comparator.comparingInt(o -> demandConnectSite.getOrDefault(o.getKey(), new HashMap()).size())));
        for (Map.Entry<String, Integer> entry : demandlist){
            //客户节点名称
            String curClient = entry.getKey();
            //客户节点带宽需求
            int curDemand = entry.getValue();
            //先处理最大流量需求的客户节点，取出该客户节点和所有边缘节点的qos进行筛选，选出小于qos_config
            HashMap<String, Integer> siteMap = new HashMap<>( demandConnectSite.get(curClient) );
            List<Map.Entry<String, Integer>> siteList = new ArrayList<>(siteMap.entrySet());
            //过滤出小于qos_config的边缘节点
            HashMap<String, Integer> map = new HashMap<>();
            //连接数少的先满足

            for(Map.Entry<String, Integer> site : siteList){
                if(curDemand == 0)
                    break;
                //resband表示当前site的剩余带宽
                int resband = site_bandwidth_copy.get(site.getKey());
                int beforResband = resband;
                //当前边缘节点的剩余带宽大于客户节点的带宽需求，全部放到当前边缘节点
                if(resband >= curDemand){
                    resband -= curDemand;
                    curDemand = 0;
                }
                //当前边缘节点的剩余带宽小于客户节点的带宽需求，先放能放下的部分，继续放下一个边缘节点
                else{
                    curDemand -= resband;
                    resband = 0;
                }
                //记录剩余带宽
                site_bandwidth_copy.put(site.getKey(), resband);

                //当前节点使用 = 分配前 - 分配后
                if(beforResband - resband!=0){
                    map.put(site.getKey(), map.getOrDefault(site.getKey(),0) + beforResband - resband);
                }

            }
            if(curDemand>0){
                sign = false;
            }

            entry.setValue(curDemand);

            dispatchStrategy.put(curClient, map);

            //site_bandwidth_copy记录了节点的带宽剩余情况
        }
        return sign;
    }

    public static void main(String[] args) {
        init();
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = dispatch();
        ToFile.writeToFile(filepath, timeList, demandName, result);
//        ToFile.writeLog(logPath, log);

        //对最终的分配方案做校验
        Check.check_1(demand, demandName, timeList, siteName, result);
        Check.check_2(site_bandwidth, demandName, timeList, siteName, result);
    }
}