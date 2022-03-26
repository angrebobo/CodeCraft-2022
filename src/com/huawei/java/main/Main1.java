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
 * @Description 对客户的需求进行排序，先满足需求大的客户
 *  对客户的需求进行排序，先满足需求大的客户
 *  将客户节点能够连接的边缘节点列出来，按照边缘节点的容量和连接数作为权重分配的因素
 *  权重公式为(容量/连接数)
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
    static Integer qos_constraint;
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
    static HashMap<String, HashMap<String, String>> log = new HashMap<>();

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
                    qos_constraint = Integer.valueOf(properties.getProperty(s.toString()));
                }
            }
//            System.out.println("qos_constraint: " + qos_constraint);

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
//             System.out.println("site_bandwidth: " + site_bandwidth);
//             System.out.println("siteName: " + siteName);
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
//            System.out.println("demand: " + demand);
//            System.out.println("demandName: " + demandName);
//            System.out.println("time: " + timeList);
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
                    qos[row][i-1] = (Integer.parseInt(temp[i]) < qos_constraint) ? 1 : 0;
                }
                row++;
            }
//            for (int i = 0; i < qos.length; i++) {
//                for (int j = 0; j < qos[0].length; j++) {
//                    System.out.print(qos[i][j] + " ");
//                }
//                System.out.println();
//            }
        } catch (IOException e) {
            System.out.println("初始化 qos二维数组 失败");
        }

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
     * @Description 对客户的需求进行排序，先满足需求大的客户
     * 将客户节点能够连接的边缘节点列出来，按照边缘节点的容量和连接数作为权重分配的因素
     * 权重公式为(容量/连接数)
     * @return
     */
    public static HashMap<String, HashMap<String, Integer>> dispatchBasedMaxBandSite(List<Map.Entry<String, Integer>> demandMap, String time, HashMap<String, HashMap<String, Integer>> timeSiteBandWidth){
        //dispatchStrategy存储最终的分配方案
        HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();

        for (Map.Entry<String, Integer> entry : demandMap){
            //客户节点名称
            String curClient = entry.getKey();
            //客户节点带宽需求
            int curDemand = entry.getValue();
            //cruDemand_dynamic表示客户节点带宽需求，会动态变化
            int cruDemand_dynamic = curDemand;
            HashMap<String, Integer> siteMap = new HashMap<>( demandConnectSite.get(curClient) );

            //存储权重信息
            HashMap<String, HashMap<String, BigDecimal>> weightMap = new HashMap<>();
            BigDecimal weightSum = new BigDecimal("0");
            for (String siteName : siteMap.keySet()){
                HashMap<String, BigDecimal> temp = new HashMap<String, BigDecimal>(){{
                    put("capacity", BigDecimal.valueOf(site_bandwidth.get(siteName)));
                    put("connect", BigDecimal.valueOf(siteMap.get(siteName)));
                }};
                weightSum = weightSum.add( temp.get("capacity").divide(temp.get("connect"), 5, RoundingMode.CEILING) );
                weightMap.put(siteName, temp);
            }

            HashMap<String, Integer> map = new HashMap<>();

            for(String siteName : siteMap.keySet()){
                //remainBandWidth表示当前site的剩余带宽
                Integer remainBandWidth = timeSiteBandWidth.get(time).get(siteName);

                //用户节点的带宽已分配完，结束循环
                if(cruDemand_dynamic == 0)
                    break;
                //当前边缘节点已经无带宽可承担，跳过该节点
                if(remainBandWidth == 0)
                    continue;

                //权重计算
                BigDecimal numerator = weightMap.get(siteName).get("capacity").divide(weightMap.get(siteName).get("connect"), 5, RoundingMode.FLOOR);
                BigDecimal weight = numerator.divide(weightSum, 5, RoundingMode.FLOOR);
                Integer curDispatch = weight.multiply(BigDecimal.valueOf(curDemand)).setScale(0, BigDecimal.ROUND_DOWN).intValue();

                //日志，记录每次的权重分配，方便后续排查错误
//                String name = time + "," + "客户:" + curClient + "," +"边缘:" + siteName;
//                HashMap<String, String> value = new HashMap<>();
//                value.put("weight", String.valueOf(weight));
//                value.put("alreadyDispatch", String.valueOf(curDispatch));
//                log.put(name, value);

                if(curDispatch > cruDemand_dynamic)
                    curDispatch = cruDemand_dynamic;

                //当前边缘节点的剩余带宽大于客户节点的带宽需求，全部放到当前边缘节点
                if(remainBandWidth >= curDispatch){
                    remainBandWidth -= curDispatch;
                    //更新客户节点的流量需求
                    cruDemand_dynamic -= curDispatch;
                }
                //当前边缘节点的剩余带宽小于客户节点的带宽需求，先放能放下的部分，继续放下一个边缘节点
                else{
                    cruDemand_dynamic -= remainBandWidth;
                    remainBandWidth = 0;
                }

                //当前节点使用 = 分配前 - 分配后
                map.put(siteName, timeSiteBandWidth.get(time).get(siteName)- remainBandWidth);
//                System.out.println("map: " + map);

                //记录剩余带宽
                HashMap<String,Integer> temp = timeSiteBandWidth.get(time);
                temp.put(siteName, remainBandWidth);
                timeSiteBandWidth.put(time, temp);
            }

            //curDemand>0,说明上一轮没有分配完流量，现在再重新分配一次，采用随机法。这里的方法可以换，不过改进应该不大，毕竟剩余的流量较小。
            if (cruDemand_dynamic > 0){
                for(String siteName : siteMap.keySet()){
                    Integer remainBandWidth = timeSiteBandWidth.get(time).get(siteName);

                    if(cruDemand_dynamic == 0)
                        break;
                    if(remainBandWidth == 0)
                        continue;

                    if(remainBandWidth >= cruDemand_dynamic){
                        remainBandWidth -= cruDemand_dynamic;
                        cruDemand_dynamic = 0;
                    }
                    else {
                        cruDemand_dynamic -= remainBandWidth;
                        remainBandWidth = 0;
                    }

                    Integer alreadyDispatch = map.get(siteName);
                    map.put(siteName, alreadyDispatch + timeSiteBandWidth.get(time).get(siteName) - remainBandWidth);

                    HashMap<String,Integer> temp = timeSiteBandWidth.get(time);
                    temp.put(siteName, remainBandWidth);
                    timeSiteBandWidth.put(time, temp);

                    //日志，记录每次的权重分配，方便后续排查错误
//                    String name = time + ", " + "客户：" + curClient + ", " +"边缘：" + siteName;
//                    HashMap<String, String> value = new HashMap<>();
//                    value.put("second" , "curDemand:" + curDemand +", alreadyDispatch:"+(site_bandwidth_copy.get(siteName)-remainBandWidth)+"");
//                    log.put(name, value);
                }
            }

            dispatchStrategy.put(curClient, map);
            //site_bandwidth_copy记录了节点的带宽剩余情况
        }

        return dispatchStrategy;
    }

    /**
     * @Description 每个时刻，执行一次调度
     * @param
     * @return
     */
    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatch(){
        //第一轮分配的分配方案，格式是<时间, <边缘节点，<客户节点，分配的流量>>>
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result1 = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> demand_copy = Check.MyClone(demand);

        //记录每个时刻，边缘节点剩余的带宽
        HashMap<String, HashMap<String, Integer>> timeSiteBandWidth = new HashMap<>();
        HashMap<String, Integer> fullLoadDays = new HashMap<>();
        //每个边缘节点可以满负载的天数
        int day = timeList.size() - (int) Math.ceil( timeList.size() * 0.95 );
        for (String site : siteName){
            fullLoadDays.put(site, day);
        }
        for(String time : timeList){
            timeSiteBandWidth.put(time, Check.MyClone(site_bandwidth));
        }

        //第一轮分配方案
        for (String time : timeList) {
            //map存分配方案,注意格式是<边缘节点，<客户节点，分配的流量>>
            HashMap<String, HashMap<String, Integer>> map = new HashMap<>();

            for (String site : siteName) {
                //remainBandWidth记录边缘节点的剩余带宽
                Integer remainBandWidth = timeSiteBandWidth.get(time).get(site);
                //demandNeed存储能连接的客户节点的带宽需求
                HashMap<String, Integer> demandNeed = Check.MyClone(demand_copy.get(time));
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
                //hashMap存储分配的流量，格式和上面的map对应，<客户节点，分配的流量>
                HashMap<String, Integer> hashMap = new HashMap<>();

                //边缘节点在当前能满负载 并且 满负载天数还有剩余(int)(remainBandWidth*0.75)
                if (needSum >=10000 && fullLoadDays.get(site) > 0) {
                    List<Map.Entry<String, Integer>> entryList = new ArrayList<>(demandNeed.entrySet());
                    //将客户节点按带宽需求从大到小排序
                    entryList.sort(( (o1, o2) -> o1.getValue() - o2.getValue() ));
                    for (Map.Entry<String, Integer> entry : entryList) {
                        if (remainBandWidth == 0)
                            break;

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
                    HashMap<String,Integer> temp = timeSiteBandWidth.get(time);
                    temp.put(site, remainBandWidth);
                    //更新满负载的天数
                    fullLoadDays.put(site, fullLoadDays.get(site) - 1);
                }
                if(hashMap.size()!=0)
                    map.put(site, hashMap);

            }

            result1.put(time, map);
        }
//        for(String time : timeList){
//            System.out.println(time + ":");
//            System.out.println(result1.get(time));
//            System.out.println();
//        }

        //第二轮分配的分配方案,格式是<时间, <客户节点，<边缘节点，分配的流量>>>

        double rate = 0.00002;
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result2 = new HashMap<>();

        while (!dispatch(rate,result2,demand_copy,timeList, timeSiteBandWidth)){
            rate = rate*1.1;
            rate = rate>=1?1:rate;
            result2 = new HashMap<>();
            System.out.println(rate);
        }
        //因为result1和result2的格式不同，要统一转化为result2的那种格式
        return ToFile.trans(result1, result2, siteName, demandName, timeList);
    }

    private static boolean dispatch(
            double rate,
            HashMap<String, HashMap<String, HashMap<String, Integer>>> result,
            HashMap<String, HashMap<String, Integer>> demand_copy,
            List<String> timeList,
            HashMap<String, HashMap<String, Integer>> timeSiteBandWidth) {

        boolean sign = true;
        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            HashMap<String, Integer> demandMap = demand_copy.get(time);
            //需求转为list用于遍历
            HashMap<String, Integer> demandMap_copy = new HashMap<>();
            for(Map.Entry<String, Integer> entry:demandMap.entrySet()) demandMap_copy.put(entry.getKey(),entry.getValue());
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap_copy.entrySet());

            //剩余当前节点带宽
            HashMap<String, Integer> resSiteBand = timeSiteBandWidth.get(time);
            //site_bandwidth是带宽上限,
            //创建最大可用带宽
            HashMap<String, Integer> siteWithMaxUseableBand = new HashMap<>();
            for(Map.Entry<String, Integer> B :resSiteBand.entrySet()){
                siteWithMaxUseableBand.put(B.getKey(),Math.min(B.getValue(),(int)(site_bandwidth.get(B.getKey())*rate)));
            }
            //分配策略
            HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();
            if(!dispatchBasedDemandClientAndUsedBandSite(demandList,siteWithMaxUseableBand,dispatchStrategy)){
                return false;
            }
            result.put(time, dispatchStrategy);
        }

        return sign;
    }


    public static boolean dispatchBasedDemandClientAndUsedBandSite(
            List<Map.Entry<String, Integer>> demandlist,
            HashMap<String, Integer> siteWithMaxUseableBand,
            HashMap<String, HashMap<String, Integer>> dispatchStrategy){
        //dispatchStrategy存储最终的分配方案
//        System.out.println(demandlist);
        //定义一个信号，如果分配过程有溢出，变成false
        boolean sign = true;

        //复制一份 节点-剩余容量 map,因为每个时间节点开始都是满的，所以每次都直接复制最大值*rate。
        HashMap<String, Integer> site_bandwidth_copy = new HashMap<>(siteWithMaxUseableBand);
//        System.out.println(site_bandwidth_copy);
//连接数少的先满足
        demandlist.sort(( (o1, o2) -> demandConnectSite.getOrDefault(o1.getKey(),new HashMap()).size() - demandConnectSite.getOrDefault(o2.getKey(),new HashMap()).size() ));
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
        ToFile.writeToFile(filepath,timeList,demandName,result);
//        ToFile.writeLog(logPath, log);

        //校验
        Check.check_1(demand, demandName, timeList, siteName, result);
        Check.check_2(site_bandwidth, demandName, timeList, siteName, result);
    }
}