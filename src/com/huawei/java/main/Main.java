package com.huawei.java.main;

import util.Check;
import util.ToFile;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Description 对客户的需求进行排序，先满足需求大的客户
 *  * 将客户节点能够连接的边缘节点列出来，按照边缘节点能够连接的客户节点数进行排序（从小到大）
 *  * 例如当前时刻处理A客户节点，A可以连接到以下几个边缘节点(B,C,D)，B可以连接2个客户节点，C可以连接3个客户节点，D可以连接4个客户节点
 *  * (B,2) < (C,3) < (D,4)
 *  * 那么A客户节点分配给B边缘节点的流量要多一点，即B边缘节点的权重要高一点
 *  * B的权重就是4份，C的权重就是3份，C的权重就是2份
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
        /*static String demandFile = "data/demand.csv";
        static String site_bandwidthFile = "data/site_bandwidth.csv";
        static String qosFile = "data/qos.csv";
        static String qos_config = "data/config.ini";
        static String filepath = "output/solution.txt";
        static String logPath = "output/log.txt";*/

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
//        System.out.println("siteConnectDemand: " + siteConnectDemand);
//        System.out.println("demandConnectSite: " + demandConnectSite);
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
    public static HashMap<String, HashMap<String, Integer>> dispatchBasedMaxBandSite(List<Map.Entry<String, Integer>> demandMap, String time){
        //dispatchStrategy存储最终的分配方案
        HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();

        HashMap<String, Integer> site_bandwidth_copy = new HashMap<>(site_bandwidth);

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
                weightSum = weightSum.add( temp.get("capacity").divide(temp.get("connect").multiply(BigDecimal.valueOf(100)), 5, RoundingMode.CEILING) );
                weightMap.put(siteName, temp);
            }
//            System.out.println("weightMap: " + weightMap);

            HashMap<String, Integer> map = new HashMap<>();

            for(String siteName : siteMap.keySet()){
                //resband表示当前site的剩余带宽
                Integer resband = site_bandwidth_copy.get(siteName);

                //用户节点的带宽已分配完，结束循环
                if(cruDemand_dynamic == 0)
                    break;
                //当前边缘节点已经无带宽可承担，跳过该节点
                if(resband == 0)
                    continue;

                //权重计算
                BigDecimal numerator = weightMap.get(siteName).get("capacity").divide(weightMap.get(siteName).get("connect").multiply(BigDecimal.valueOf(100)), 5, RoundingMode.FLOOR);
                BigDecimal weight = numerator.divide(weightSum, 5, RoundingMode.FLOOR);
                Integer curDispatch = weight.multiply(BigDecimal.valueOf(curDemand)).setScale(0, BigDecimal.ROUND_DOWN).intValue();

                //日志，记录每次的权重分配，方便后续排查错误
                String name = time + "," + "客户:" + curClient + "," +"边缘:" + siteName;
                HashMap<String, String> value = new HashMap<>();
                value.put("weight", String.valueOf(weight));
                value.put("alreadyDispatch", String.valueOf(curDispatch));
                log.put(name, value);

                if(curDispatch > cruDemand_dynamic)
                    curDispatch = cruDemand_dynamic;

                //当前边缘节点的剩余带宽大于客户节点的带宽需求，全部放到当前边缘节点
                if(resband >= curDispatch){
                    resband -= curDispatch;
                    //更新客户节点的流量需求
                    cruDemand_dynamic -= curDispatch;
                }
                //当前边缘节点的剩余带宽小于客户节点的带宽需求，先放能放下的部分，继续放下一个边缘节点
                else{
                    cruDemand_dynamic -= resband;
                    resband = 0;
                }

                //当前节点使用 = 分配前 - 分配后
                map.put(siteName, site_bandwidth_copy.get(siteName)-resband);
//                System.out.println("map: " + map);

                //记录剩余带宽
                site_bandwidth_copy.put(siteName, resband);
            }

            //curDemand>0,说明第一轮没有分配完流量，现在再重新分配一次，采用随机法。这里的方法可以换，不过改进应该不大，毕竟剩余的流量较小。
            if (cruDemand_dynamic > 0){
                for(String siteName : siteMap.keySet()){
                    Integer resband = site_bandwidth_copy.get(siteName);

                    if(cruDemand_dynamic == 0)
                        break;
                    if(resband == 0)
                        continue;

                    if(resband >= cruDemand_dynamic){
                        resband -= cruDemand_dynamic;
                        cruDemand_dynamic = 0;
                    }
                    else {
                        cruDemand_dynamic -= resband;
                        resband = 0;
                    }

                    Integer alreadyDispatch = map.get(siteName);
                    map.put(siteName, alreadyDispatch+site_bandwidth_copy.get(siteName)-resband);
                    site_bandwidth_copy.put(siteName, resband);

                    //日志，记录每次的权重分配，方便后续排查错误
                    String name = time + ", " + "客户：" + curClient + ", " +"边缘：" + siteName;
                    HashMap<String, String> value = new HashMap<>();
                    value.put("second" , "curDemand:" + curDemand +", alreadyDispatch:"+(site_bandwidth_copy.get(siteName)-resband)+"");
                    log.put(name, value);
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
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>();
        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            HashMap<String, Integer> demandMap = demand.get(time);
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap.entrySet());
            //按需求流量的大小进行排序，排序方式为从大到小
            demandList.sort((o1,o2) -> o2.getValue()-o1.getValue());
//        System.out.println(demandList);
            HashMap<String, HashMap<String, Integer>> dispatchStrategy = dispatchBasedMaxBandSite(demandList, time);
//        System.out.println("dispatchStrategy: " + dispatchStrategy);
            result.put(time, dispatchStrategy);
        }
//        System.out.println("result: " + result);
        return result;
    }

    public static void main(String[] args) {
        init();
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = dispatch();
        ToFile.writeToFile(filepath,timeList,demandName,result);
//        ToFile.writeLog(logPath, log);

        //校验
//        Check.check_1(demand, demandName, timeList, siteName, result);
    }
}