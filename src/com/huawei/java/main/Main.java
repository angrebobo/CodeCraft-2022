package com.huawei.java.main;
//import util.Check;


import util.Check;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description 
 * @param
 * @return
 */
public class Main {

    //demandName存储客户节点的名称
    static List<String> demandName = new ArrayList<>();
    //time存储各个时刻
    static List<String> timeList = new ArrayList<>();
    static int timeSum = 0;
    //siteName存储边缘节点的名称
    static List<String> siteName = new ArrayList<>();
    //qos_constraint存储qos的上限值
    static Integer qos_constraint;
    //site_bandwidth存储边缘节点的带宽上限，格式为<边缘节点名称，带宽上限>
    static HashMap<String, Integer> site_bandwidth = new HashMap<>();
    static long bandwidthSum = 0;
    //demand存储每个时刻客户节点的带宽需求，格式为<时刻，<客户节点名称，带宽需求>>
    static HashMap<String, HashMap<String, Integer>> demand = new HashMap<>();
    static long demandSum = 0;
    //qos_d存储边缘节点和客户节点之间的qos，格式为<边缘节点名称，<客户节点名称，qos>>
    static HashMap<String, HashMap<String, Integer>> qos_d = new HashMap<>();
    ////qos_s存储边缘节点和客户节点之间的qos，格式为<客户节点名称，<边缘节点名称，qos>>。为什么要多存一份？因为可以用不同的方式来拿数据。
    static HashMap<String, HashMap<String, Integer>> qos_s = new HashMap<>();

    // ！！！在idea本地跑用这个路径
        static String demandFile = "data/demand.csv";
        static String site_bandwidthFile = "data/site_bandwidth.csv";
        static String qosFile = "data/qos.csv";
        static String qos_config = "data/config.ini";
        static String filepath = "output/solution.txt";


    // ！！！提交到线上用这个环境
//    static String demandFile = "/data/demand.csv";
//    static String site_bandwidthFile = "/data/site_bandwidth.csv";
//    static String qosFile = "/data/qos.csv";
//    static String qos_config = "/data/config.ini";
//    static String filepath = "/output/solution.txt";

    /**
     * @Description 初始化方法，读入文件并存储到本地
     * @param
     * @return
     */
    public static void init(){
        String line;
        String[] temp;

        //初始化边缘节点
        try(BufferedReader reader = new BufferedReader(new FileReader(site_bandwidthFile))) {
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                if( "site_name".equals(temp[0]) )
                    continue;
                siteName.add(temp[0]);
                site_bandwidth.put(temp[0], Integer.valueOf(temp[1]));
//                bandwidthSum += Integer.valueOf(temp[1]);

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
//                    demandSum += Integer.valueOf(temp[i]);
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

        //初始化qos_s和qos_d
        try(BufferedReader reader = new BufferedReader(new FileReader(qosFile))){
            List<String> name = new LinkedList<>();
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                int len = temp.length;
                if("site_name".equals(temp[0])){
                    for (int i = 0; i < len; i++) {
                        name.add(temp[i]);
                    }
                    continue;
                }

                HashMap<String, Integer> map = new HashMap<>();
                for (int i = 1; i < len; i++) {
                    map.put(name.get(i), Integer.valueOf(temp[i]));
                }
                qos_d.put(temp[0], map);
            }
            //初始化qos_s
            int len_site = siteName.size();
            int len_demand = demandName.size();
            for (int i = 0; i < len_demand; i++) {
                String demand_name = demandName.get(i);
                HashMap<String, Integer> map = new HashMap<>();
                for (int j = 0; j < len_site; j++) {
                    map.put(siteName.get(j), qos_d.get(siteName.get(j)).get(demand_name));
                }
                qos_s.put(demand_name, map);
            }

//            System.out.println("qos_d: " + qos_d);
//            System.out.println("qos_s: " + qos_s);
        } catch (IOException e) {
            System.out.println("初始化 qos 失败");
        }

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
     * @Description 按照需求大小排序节点，按照历史宽带占用大小排序site
     * @param
     * @return
     */
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

        for (Map.Entry<String, Integer> entry : demandlist){
            //客户节点名称
            String curClient = entry.getKey();
            //客户节点带宽需求
            int curDemand = entry.getValue();
            //先处理最大流量需求的客户节点，取出该客户节点和所有边缘节点的qos进行筛选，选出小于qos_config
            HashMap<String, Integer> siteMap = new HashMap<>( qos_s.get(curClient) );
            List<Map.Entry<String, Integer>> siteList = new ArrayList<>(siteMap.entrySet());
            //过滤出小于qos_config的边缘节点
            siteList = siteList.stream().filter( o1 -> o1.getValue()<qos_constraint ).collect(Collectors.toList());

// 对site根据历史最大使用情况进行排序
//            siteList.sort((o1,o2) -> o2.getValue()-o1.getValue());
            HashMap<String, Integer> map = new HashMap<>();

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

//                System.out.println("map: " + map);

            }
            if(curDemand!=0){
                sign = false;
                entry.setValue(curDemand);
            }
            dispatchStrategy.put(curClient, map);
            //site_bandwidth_copy记录了节点的带宽剩余情况
        }



        return sign;
    }



    /**
     * @Description 具体的调度方法:对客户的需求进行排序，先满足需求大的客户
     * 将客户节点能够连接的边缘节点列出来，按照能够提供的带宽大小排序，先放到能提供大带宽的边缘节点中
     * @param: demandMap存储<用户节点名称，用户节点的带宽需求>
     * @return
     */
    /*public static HashMap<String, HashMap<String, Integer>> dispatchBasedMaxBandSite(List<Map.Entry<String, Integer>> demandMap){
        //dispatchStrategy存储最终的分配方案
        HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();

        //复制一份 节点-剩余容量 map,因为每个时间节点开始都是满的，所以每次都直接复制最大值。
        HashMap<String, Integer> site_bandwidth_copy = new HashMap<>(site_bandwidth);

        for (Map.Entry<String, Integer> entry : demandMap){
            //客户节点名称
            String curClient = entry.getKey();
            //客户节点带宽需求
            int curDemand = entry.getValue();

            //先处理最大流量需求的客户节点，取出该客户节点和所有边缘节点的qos进行筛选，选出小于qos_config
            HashMap<String, Integer> siteMap = new HashMap<>( qos_s.get(curClient) );
            List<Map.Entry<String, Integer>> siteList = new ArrayList<>(siteMap.entrySet());
            //过滤出小于qos_config的边缘节点
            siteList = siteList.stream().filter( o1 -> o1.getValue()<400 ).collect(Collectors.toList());
            //siteList原先存的是<边缘节点名称，边缘节点到客户节点的qos>，现在替换成<边缘节点名称，边缘节点剩余的带宽>
            siteList.forEach(o1 -> o1.setValue( site_bandwidth_copy.get(o1.getKey())) );
            //对site根据当前容量进行排序
            siteList.sort((o1,o2) -> o2.getValue()-o1.getValue());

            HashMap<String, Integer> map = new HashMap<>();
            //遍历siteList
            for(Map.Entry<String, Integer> site : siteList){
                if(curDemand == 0)
                    break;

                //resband表示当前site的剩余带宽
                int resband = site.getValue();
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
                map.put(site.getKey(), site.getValue()-resband);
//                System.out.println("map: " + map);
                System.out.print(curClient);
                System.out.print(curDemand);

                System.out.print(site);

            }
            dispatchStrategy.put(curClient, map);
            //site_bandwidth_copy记录了节点的带宽剩余情况
        }

        return dispatchStrategy;
    }*/

    /**
     * @Description 每个时刻，执行一次调度
     * @param
     * @return
     */
    public static boolean dispatch(
            double rate,HashMap<String,
            HashMap<String, HashMap<String, Integer>>> result){
        //定义一个信号，如果分配过程有溢出，变成false
        boolean sign = true;
//      记录每个节点历史最大可使用带宽
        HashMap<String, Integer> siteWithMaxUseableBand = new HashMap<>();
//      初始化,所有使用情况都为
        for(String site : siteName) siteWithMaxUseableBand.put(site,(int)(site_bandwidth.get(site)*rate));

        HashMap<String, List<Map.Entry<String, Integer>>> alldemandList = new HashMap<>();
        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            HashMap<String, Integer> demandMap = demand.get(time);
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap.entrySet());
//            //按需求流量的大小进行排序，排序方式为从大到小
//            demandList.sort((o1,o2) -> o2.getValue()-o1.getValue());
//        System.out.println(demandList);
            //按照最大需求排序客户节点
//            HashMap<String, HashMap<String, Integer>> dispatchStrategy = dispatchBasedMaxBandSite(demandList);

            HashMap<String, HashMap<String, Integer>> dispatchStrategy = new HashMap<>();
            if(!dispatchBasedDemandClientAndUsedBandSite(demandList,siteWithMaxUseableBand,dispatchStrategy)){
                sign = false;
            }
            result.put(time, dispatchStrategy);
            alldemandList.put(time,demandList);

        }
        if(sign==false){
            int times =(int) (timeList.size()*0.04);
            sign = secondDispat(alldemandList,result,times,rate);
        }
        return sign;
    }

    static boolean secondDispat(HashMap<String, List<Map.Entry<String, Integer>>> alldemandList,
                                HashMap<String, HashMap<String, HashMap<String, Integer>>> result,
                                int times,
                                double rate) {
        boolean sign = true;
        //记录最大被填满次数
        HashMap<String, Integer> timesMap = new HashMap<>();

        for (String time : timeList){
            //得到当前时刻，所有客户节点的剩余需求流量
            List<Map.Entry<String, Integer>> demandList = alldemandList.get(time);
            Map<String,Integer> demandmap = new HashMap<>();
            for(Map.Entry<String, Integer> entry : demandList) demandmap.put(entry.getKey(), entry.getValue());

            for (String site : siteName){
                int resTime = timesMap.getOrDefault(site,times)-1;
                if(resTime<0) continue;
                timesMap.put(site,resTime);

                HashMap<String, Integer> clientMap = new HashMap<>( qos_d.get(site));
                List<Map.Entry<String, Integer>> clientList = new ArrayList<>(clientMap.entrySet());
                clientList = clientList.stream().filter( o1 -> o1.getValue()<qos_constraint ).collect(Collectors.toList());

                int dispatBand = site_bandwidth.get(site) - (int)(site_bandwidth.get(site)*rate);
                int dispatBand_copy = dispatBand;
                for(Map.Entry<String, Integer> cilent:clientList){
                    if(dispatBand==0) break;
                    String curClient = cilent.getKey();
                    HashMap<String, Integer> dispatchStrategy = result.get(time).get(curClient);
                    if(!dispatchStrategy.containsKey(site)) continue;
                    int curdemand = demandmap.get(curClient);
                    if(curdemand==0) continue;
                    if(curdemand<=dispatBand){
                        dispatBand -=curdemand;
                        curdemand = 0;
                    }
                    else{
                        curdemand-= dispatBand;
                        dispatBand = 0;
                    }

                    demandmap.put(curClient,curdemand);
                    dispatchStrategy.put(site,dispatchStrategy.get(site) + dispatBand_copy-dispatBand);
                    dispatBand_copy = dispatBand;
                }
            }
            for(Map.Entry<String, Integer> entry : demandmap.entrySet()){
                if(entry.getValue()>0) return false;
            }
        }



        return sign;
    }

    /**
     * @Description 将调度策略写入到文件中
     * @param
     * @return
     */
    public static void writeToFile(HashMap<String, HashMap<String, HashMap<String, Integer>>> result){

        File file = new File(filepath);
        if(!file.exists()){
            file.getParentFile().mkdir();
            try {
                //创建文件
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("文件创建失败");
            }
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filepath))) {
            for(String time : timeList){

                HashMap<String, HashMap<String, Integer>> demandMap = result.get(time);

                //key代表客户节点名称
                for(String demand_name : demandName){
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(demand_name).append(":");
                    HashMap<String, Integer> siteMap = demandMap.get(demand_name);

                    for(String siteName : siteMap.keySet()){
                        buffer.append("<").append(siteName).append(",").append(siteMap.get(siteName)).append(">").append(",");
                    }
                    //删除最后一个逗号
                    buffer.deleteCharAt(buffer.length() - 1);
                    //比赛的运行环境是Linux，所以手动添加换行符
                    buffer.append("\r\n");
                    bufferedWriter.write(buffer.toString());

                }
            }
        } catch (IOException e) {
            System.out.println("将调度方案写入文件失败");
        }
    }


    public static void main(String[] args) {
        init();
        for (String time : timeList){
            //得到当前时刻，所有客户节点的需求流量
            HashMap<String, Integer> demandMap = demand.get(time);
            List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap.entrySet());

            for (Map.Entry<String, Integer> entry : demandList) {
                //客户节点名称
                String curClient = entry.getKey();
                //客户节点带宽需求
                int curDemand = entry.getValue();

                demandSum+=curDemand;
                //先处理最大流量需求的客户节点，取出该客户节点和所有边缘节点的qos进行筛选，选出小于qos_config
                HashMap<String, Integer> siteMap = new HashMap<>(qos_s.get(curClient));
                List<Map.Entry<String, Integer>> siteList = new ArrayList<>(siteMap.entrySet());
                //过滤出小于qos_config的边缘节点
                siteList = siteList.stream().filter( o1 -> o1.getValue()<qos_constraint ).collect(Collectors.toList());

                for(Map.Entry<String, Integer> site : siteList){
                    bandwidthSum += site_bandwidth.get(site.getKey());
                }

            }
        }
        double rate = (double) demandSum/bandwidthSum;
        rate = rate>=1?1:rate;
        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>();
        while (!dispatch(rate,result)){
            rate = rate*1.1;
            rate = rate>=1?1:rate;
            result = new HashMap<>();
            System.out.println(rate);
        }
        System.out.println(Check.check_1(demand, demandName, timeList, siteName, result));
        writeToFile( result );




    }
}