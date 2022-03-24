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
 * @Description 基于version5的版本，对第二次分配做优化
 * 采用两次分配的方案，第一次分配卡95%，第二次分配增加服务器的上下限使用率
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
    static Integer[][] qos;
    //demandConnectSite存储客户节点能连接到的边缘节点，格式为<客户节点名称，<边缘节点名称，该边缘节点名称能连接的客户节点数>>
    static HashMap<String, HashMap<String, Integer>> demandConnectSite = new HashMap<>();
    //siteConnectDemand存储边缘节点能连接到的客户节点，格式为<边缘节点名称，<客户节点名称，该客户节点名称能连接的边缘节点数>>
    static HashMap<String, HashMap<String, Integer>> siteConnectDemand = new HashMap<>();
    //siteConnectDemandSum存储边缘节点能连接到的客户节点，格式为<时刻，<边缘节点名称，连接的客户节点的流量总和>>
    static HashMap<String, HashMap<String, Integer>> siteConnectDemandSum = new HashMap<>();
    //存储日志
    static HashMap<String, HashMap<String, String>> log = new HashMap<>();

    // ！！！在idea本地跑用这个路径
        static String demandFile = "data/demand.csv";
        static String site_bandwidthFile = "data/site_bandwidth.csv";
        static String qosFile = "data/qos.csv";
        static String qos_config = "data/config.ini";
        static String filepath = "output/solution.txt";
        static String logPath = "output/log.txt";

    // ！！！提交到线上用这个环境
    /*static String demandFile = "/data/demand.csv";
    static String site_bandwidthFile = "/data/site_bandwidth.csv";
    static String qosFile = "/data/qos.csv";
    static String qos_config = "/data/config.ini";
    static String filepath = "/output/solution.txt";
    static String logPath = "/output/log.txt";*/

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
        qos = new Integer[siteName.size()][demandName.size()];
        try(BufferedReader reader = new BufferedReader(new FileReader(qosFile))){
            int row = 0;
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                int len = temp.length;
                if("site_name".equals(temp[0]))
                    continue;

                for (int i = 1; i < len; i++) {
                    qos[row][i-1] = (Integer.parseInt(temp[i]) < qosLimit) ? 1 : 0;
                }
                row++;
            }
        } catch (IOException e) {
            System.out.println("初始化 qos二维数组 失败");
        }

        //初始化siteConnectNum，demandConnectNum，siteConnectDemand，demandConnectSite
        HashMap<String, Integer> siteConnectNum = new HashMap<>();
        HashMap<String, Integer> demandConnectNum = new HashMap<>();
        for (int i = 0; i < siteName.size(); i++) {
            siteConnectNum.put(siteName.get(i), Arrays.stream(qos[i]).mapToInt(Integer::intValue).sum());
        }
        for (int i = 0; i < demandName.size(); i++) {
            int count = 0;
            for (int j = 0; j < siteName.size(); j++) {
                count += qos[j][i];
            }
            demandConnectNum.put(demandName.get(i), count);
        }

        for (int i = 0; i < siteName.size(); i++) {
            HashMap<String, Integer> map = new HashMap<>();
            for (int j = 0; j < demandName.size(); j++) {
                if(qos[i][j] == 1){
                    map.put(demandName.get(j), demandConnectNum.get(demandName.get(j)));
                    siteConnectDemand.put(siteName.get(i), map);
                }
            }
        }

        for (int i = 0; i < demandName.size(); i++) {
            HashMap<String, Integer> map = new HashMap<>();
            for (int j = 0; j < siteName.size(); j++) {
                if(qos[j][i] == 1){
                    map.put(siteName.get(j), siteConnectNum.get(siteName.get(j)));
                    demandConnectSite.put(demandName.get(i), map);
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
     * @Description 第一轮分配方案
     * @param
     * @return
     */
    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchFirst(
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays){

        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>();

        //每轮时间，最多有countLimit个边缘节点达到高负载
        Integer countLimit = (int)(siteName.size() * 0.05);

        //第一轮分配方案
        for (String time : timeList) {
            HashMap<String, Integer> needMap = siteConnectDemandSum.get(time);
            //根据连接的客户节点的流量总和大小来排序
            List<Map.Entry<String, Integer>> entryList1 = new ArrayList<>(needMap.entrySet());
            entryList1.sort((o1, o2) -> o2.getValue()-o1.getValue());
            //map存分配方案,注意格式是<边缘节点，<客户节点，分配的流量>>
            HashMap<String, HashMap<String, Integer>> map = new HashMap<>();
            int count = 0;

            for(Map.Entry<String, Integer> sortedEntry : entryList1){
                if(++count > countLimit)
                    break;

                String site = sortedEntry.getKey();
                //该节点没有高负载的次数了
                if(fullLoadDays.get(site) <= 0)
                    continue;

                //needSum表示所有客户节点的需求总和
                Integer needSum = sortedEntry.getValue();
                //remainBandWidth记录边缘节点的剩余带宽
                Integer remainBandWidth = timeSiteBandWidth.get(time).get(site);
                //demandNeed存储能连接的客户节点的带宽需求
                HashMap<String, Integer> demandNeed = Check.MyClone(siteConnectDemand.getOrDefault(site, null));
                //该边缘节点是个死节点，连接不到任何客户节点
                if(demandNeed == null){
                    continue;
                }
                //更新客户节点的流量需求
                demandNeed.replaceAll((k, v) -> demand_copy.get(time).get(k));

                //边缘节点在当前能满负载 并且 满负载天数还有剩余
                if (needSum >=25000/*remainBandWidth*0.5*/) {
                    //hashMap存储分配的流量，格式和上面的map对应，<客户节点，分配的流量>
                    HashMap<String, Integer> hashMap = new HashMap<>();
                    List<Map.Entry<String, Integer>> entryList = new ArrayList<>(demandNeed.entrySet());
                    //将客户节点按带宽需求从大到小排序
                    entryList.sort(((o1, o2) -> o2.getValue() - o1.getValue()));
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
                    //更新边缘节点的带宽
                    timeSiteBandWidth.get(time).put(site, remainBandWidth);
                    //当前时刻当前边缘节点高负载，做标记
                    fullLoadTime.get(time).put(site, 1);
                    fullLoadDays.put(site, fullLoadDays.get(site)-1);
                    map.put(site, hashMap);
                }
            }
            result.put(time, map);
        }
        return result;
    }

    /**
     * @Description 第二轮分配方案
     * @param
     * @return
     */
    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchSecond(
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays){

        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>();
        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demand_copy.get(time).entrySet());
            //按需求流量的大小进行排序，排序方式为从大到小
            demandList.sort((o1,o2) -> o2.getValue()-o1.getValue());
            HashMap<String, HashMap<String, Integer>> dispatchStrategy =
                    dispatchBasedMaxBandSite(time, demandList, timeSiteBandWidth, fullLoadTime, fullLoadDays);

            result.put(time, dispatchStrategy);
        }
        return result;
    }

    /**
     * @Description
     * @return
     */
    public static HashMap<String, HashMap<String, Integer>> dispatchBasedMaxBandSite(
            String time,
            List<Map.Entry<String, Integer>> demandMap,
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth,
            HashMap<String, HashMap<String, Integer>> fullLoadTime,
            HashMap<String, Integer> fullLoadDays){

        //dispatchStrategy存储最终的分配方案
        HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();

        //边缘节点的负载率上限
        Double usageUpper = 0.7;
        //边缘节点的负载率下限
        Double usageFloor = 0.2;
        String usage = "usage";
        HashMap<String, HashMap<String, Double>> usageOfSite = new HashMap<>();
        for (String site : siteName){
            HashMap<String, Double> temp = new HashMap<>();
            //边缘节点达到负载率上限需要的带宽
            temp.put("usageUpper", site_bandwidth.get(site) * usageUpper);
            //边缘节点达到负载率下限需要的带宽
            temp.put("usageFloor", site_bandwidth.get(site) * usageFloor);
            //边缘节点当前的使用率
            temp.put(usage, (double) ((site_bandwidth.get(site) - timeSiteBandWidth.get(time).get(site))/site_bandwidth.get(site)));
            usageOfSite.put(site, temp);
        }

        for (Map.Entry<String, Integer> entry : demandMap){
            //客户节点名称
            String curClient = entry.getKey();
            //客户节点带宽需求
            int curDemand = entry.getValue();
            //cruDemand_dynamic表示客户节点带宽需求，会动态变化
            int curDemand_dynamic = curDemand;

            HashMap<String, Integer> siteMap = new HashMap<>( demandConnectSite.get(curClient) );
            //siteList存储客户节点能连接的边缘节点
            List<String> siteList = new ArrayList<>(siteMap.keySet());
            //按边缘节点的剩余带宽从大到小排序
            siteList.sort((o1, o2) -> timeSiteBandWidth.get(time).get(o2)-timeSiteBandWidth.get(time).get(o1));
            //按按边缘节点的使用率来排序
//            siteList.sort( (o1, o2) -> (int) (usageOfSite.get(o1).get(usage)-usageOfSite.get(o2).get(usage)) );
//            siteList.sort( (o1, o2) -> (int) (usageOfSite.get(o2).get(usage)-usageOfSite.get(o1).get(usage)) );

            //存储权重信息
            /*HashMap<String, HashMap<String, BigDecimal>> weightMap = new HashMap<>();
            BigDecimal weightSum = new BigDecimal("0");
            for (String siteName : siteMap.keySet()){
                HashMap<String, BigDecimal> temp = new HashMap<String, BigDecimal>(){{
                    //边缘节点的带宽的 剩余 带宽容量
                    put("capacity", BigDecimal.valueOf( timeSiteBandWidth.get(time).get(siteName) ));
                    put("connect", BigDecimal.valueOf( siteMap.get(siteName) ));
                }};
                weightSum = weightSum.add( temp.get("capacity").divide(temp.get("connect"), 5, RoundingMode.CEILING) );
                weightMap.put(siteName, temp);
            }*/

            //存储分配结果
            HashMap<String, Integer> map = new HashMap<>();
            for(String siteName : siteList){
                //remainBandWidth表示当前site的剩余带宽
                Integer remainBandWidth = timeSiteBandWidth.get(time).get(siteName);

                //用户节点的带宽已分配完，结束循环
                if(curDemand_dynamic == 0)
                    break;
                //当前边缘节点已经无带宽可承担，跳过该节点
                if(remainBandWidth == 0)
                    continue;

                //该边缘节点在第一轮分配时已经分配过了，那就使该边缘节点尽可能高负载，把能分配的流量都给它
                //或者该边缘节点的满负载天数还有剩余，将全部的带宽分配给它
                if(fullLoadTime.get(time).get(siteName) == 1 || fullLoadDays.get(siteName) > 0){
                    if(curDemand_dynamic > remainBandWidth){
                        curDemand_dynamic -= remainBandWidth;

                        map.put(siteName, remainBandWidth);
                        timeSiteBandWidth.get(time).put(siteName, 0);
                    }
                    else {
                        map.put(siteName, curDemand_dynamic);
                        timeSiteBandWidth.get(time).put(siteName, remainBandWidth-curDemand_dynamic);
                        curDemand_dynamic = 0;
                    }
                    //更新边缘节点的使用率
                    usageOfSite.get(siteName).put("usage", (double) ((site_bandwidth.get(siteName) - timeSiteBandWidth.get(time).get(siteName))/site_bandwidth.get(siteName)));
                    if(fullLoadTime.get(time).get(siteName) == 0)
                        fullLoadDays.put(siteName, fullLoadDays.get(siteName)-1);

                    continue;
                }

                //到这一步，说明该边缘节点在第一轮分配时没有分配到带宽

                //权重计算
                /*BigDecimal numerator = weightMap.get(siteName).get("capacity").divide(weightMap.get(siteName).get("connect"), 5, RoundingMode.FLOOR);
                BigDecimal weight = numerator.divide(weightSum, 5, RoundingMode.FLOOR);
                Integer curDispatch = weight.multiply(BigDecimal.valueOf(curDemand)).setScale(0, BigDecimal.ROUND_DOWN).intValue();*/
                Integer curDispatch = curDemand_dynamic;
                //日志，记录每次的权重分配，方便后续排查错误
               /* String name = time + "," + "客户:" + curClient + "," +"边缘:" + siteName;
                HashMap<String, String> value = new HashMap<>();
                value.put("weight", String.valueOf(weight));
                value.put("alreadyDispatch", String.valueOf(curDispatch));
                log.put(name, value);*/

                //防止分配出去的带宽 超过 能分配的带宽
                /*if(curDispatch > curDemand_dynamic)
                    curDispatch = curDemand_dynamic;*/

                //如果分配curDispatch，计算分配后的使用率，使用率= (当前要分配+初始带宽-剩余带宽)/(初始带宽)
                Double uasgeSuppose = (double)(curDispatch + site_bandwidth.get(siteName) - remainBandWidth) / (double)(site_bandwidth.get(siteName));
                //负载率过高或者过低，不分给该边缘节点
                if(uasgeSuppose > usageUpper || uasgeSuppose < usageFloor)
                    continue;

                remainBandWidth -= curDispatch;
                //更新客户节点的流量需求
                curDemand_dynamic -= curDispatch;


                /*//当前边缘节点的剩余带宽大于客户节点的带宽需求，全部放到当前边缘节点
                if(remainBandWidth >= curDispatch){
                    remainBandWidth -= curDispatch;
                    //更新客户节点的流量需求
                    curDemand_dynamic -= curDispatch;
                }
                //当前边缘节点的剩余带宽小于客户节点的带宽需求，先放能放下的部分，继续放下一个边缘节点
                else{
                    curDemand_dynamic -= remainBandWidth;
                    remainBandWidth = 0;
                }*/

                //当前节点使用 = 分配前 - 分配后
                map.put(siteName, timeSiteBandWidth.get(time).get(siteName)- remainBandWidth);
                //记录剩余带宽
                timeSiteBandWidth.get(time).put(siteName, remainBandWidth);
            }

            //curDemand>0,说明上一轮没有分配完流量，现在再重新分配一次，采用随机法。这里的方法可以换，不过改进应该不大，毕竟剩余的流量较小。
            if (curDemand_dynamic > 0){
                for(String siteName : siteList){
                    Integer remainBandWidth = timeSiteBandWidth.get(time).get(siteName);

                    if(curDemand_dynamic == 0)
                        break;
                    if(remainBandWidth == 0)
                        continue;

                    if(remainBandWidth >= curDemand_dynamic){
                        remainBandWidth -= curDemand_dynamic;
                        curDemand_dynamic = 0;
                    }
                    else {
                        curDemand_dynamic -= remainBandWidth;
                        remainBandWidth = 0;
                    }

                    Integer alreadyDispatch = map.getOrDefault(siteName, 0);
                    map.put(siteName, alreadyDispatch + timeSiteBandWidth.get(time).get(siteName) - remainBandWidth);

                    HashMap<String,Integer> temp = timeSiteBandWidth.get(time);
                    temp.put(siteName, remainBandWidth);
                    timeSiteBandWidth.put(time, temp);

                    //日志，记录每次的权重分配，方便后续排查错误
                    /*String name = time + ", " + "客户：" + curClient + ", " +"边缘：" + siteName;
                    HashMap<String, String> value = new HashMap<>();
                    value.put("second" , "curDemand:" + curDemand +", alreadyDispatch:"+(site_bandwidth_copy.get(siteName)-remainBandWidth)+"");
                    log.put(name, value);*/
                }
            }
            dispatchStrategy.put(curClient, map);
        }
        return dispatchStrategy;
    }

    /**
     * @Description 分配分为第一次分配和第二次分配
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
        //每个边缘节点可以满负载的天数
        HashMap<String, Integer> fullLoadDays = new HashMap<>();
        //每个边缘节点满负载的时刻，格式为<时刻，<边缘节点，剩余带宽>>
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
        }

        //第一轮分配的分配方案，格式是<时间, <边缘节点，<客户节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result1 = dispatchFirst(timeSiteBandWidth, demand_copy, fullLoadTime, fullLoadDays);
        //第二轮分配的分配方案,格式是<时间, <客户节点，<边缘节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result2 = dispatchSecond(timeSiteBandWidth, demand_copy, fullLoadTime, fullLoadDays);

        //因为result1和result2的格式不同，要统一转化为result2的那种格式
        return ToFile.trans(result1, result2, siteName, demandName, timeList);
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