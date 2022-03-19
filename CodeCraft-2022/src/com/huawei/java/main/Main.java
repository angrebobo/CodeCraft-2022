import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
    //qos_d存储边缘节点和客户节点之间的qos，格式为<边缘节点名称，<客户节点名称，qos>>
    static HashMap<String, HashMap<String, Integer>> qos_d = new HashMap<>();
    ////qos_s存储边缘节点和客户节点之间的qos，格式为<客户节点名称，<边缘节点名称，qos>>。为什么要多存一份？因为可以用不同的方式来拿数据。
    static HashMap<String, HashMap<String, Integer>> qos_s = new HashMap<>();

    /**
     * @Description 初始化方法，读入文件并存储到本地
     * @param
     * @return
     */
    public static void init(){
        String demandFile = "CodeCraft-2022/demand.csv";
        String site_bandwidthFile = "CodeCraft-2022/site_bandwidth.csv";
        String qosFile = "CodeCraft-2022/qos.csv";
        String qos_config = "CodeCraft-2022/config.ini";
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
            }
//             System.out.println("site_bandwidth: " + site_bandwidth);
//             System.out.println("siteName: " + siteName);
        }
        catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        //初始化qos
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
            HashMap<String, Integer> map = new HashMap<>();
            int len_site = siteName.size();
            int len_demand = demandName.size();
            for (int i = 0; i < len_demand; i++) {
                String demand_name = demandName.get(i);
                for (int j = 0; j < len_site; j++) {
                    map.put(siteName.get(j), qos_d.get(siteName.get(j)).get(demand_name));
                }
                qos_s.put(demand_name, map);
            }

//            System.out.println("qos_d: " + qos_d);
//            System.out.println("qos_s: " + qos_s);
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
    public static HashMap<String, Integer> dispatchbasedMaxBandSite(List<Map.Entry<String, Integer>> demandMap){
        //深复制一份 节点-剩余容量 map； 因为每个时间节点开始都是满的，所以每次都直接复制最大值。
        // String直接使用引用进行浅复制，因为只会更改当前带宽值，所以引用String应该没关系
        HashMap<String, Integer> site_resband = new HashMap<>();
        for (Map.Entry<String, Integer> entry : site_bandwidth.entrySet()){
            String s = entry.getKey();
            int b =entry.getValue();
            site_resband.put(s,b);
        }
        //System.out.println(site_resband);

        //对每个用户都得到用户名和需求
        for (Map.Entry<String, Integer> entry : demandMap){
            String curClient = entry.getKey();
            int curDemand = entry.getValue();

            //根据用户名得到所有能连接的site，和其剩余带宽
            //先求site
            HashMap<String, Integer> ALLsite = qos_s.get(curClient);

            //保存一个可以连接的site和其对应的剩余带宽的map
            HashMap<String, Integer> SitecanConnect_withband = new HashMap<>();

            for(Map.Entry<String, Integer> Site_qos : ALLsite.entrySet()){
                String s = Site_qos.getKey();
                int qos =Site_qos.getValue();
                if(qos<=qos_constraint){
                    SitecanConnect_withband.put(s,site_resband.get(s));
                }
            }

            //对site根据当前容量进行排序
            List<Map.Entry<String, Integer>> connectedSiteList = new ArrayList<>(SitecanConnect_withband.entrySet());
            connectedSiteList.sort((o1,o2) -> o2.getValue()-o1.getValue());
            //遍历site

            for(Map.Entry<String, Integer> site_maxband :connectedSiteList){
                String s = site_maxband.getKey();
                int resband =site_maxband.getValue();
                //满足 直接都放进去
                //不满足 放完，对下一个site继续放
                if(resband>=curDemand){
                    resband -= curDemand;
                    curDemand = 0;
                }
                else{
                    curDemand -= resband;
                    resband = 0;
                }
                //记录剩余带宽

                site_resband.put(s,resband);

            }
            //到此，第一个用户的需求根据可以连接的节点的带宽从大到小分配结束
            //site_resband记录了节点的带宽剩余情况。
        }
        //到此，site_resband记录了所有节点的带宽剩余情况。
        //如果要得到答案（带宽的使用情况），则用 最大值-剩余情况
        //最大值-剩余情况==0的话 说明该节点没有被使用，无需添加
        HashMap<String, Integer> site_used = new HashMap<>();
        for (Map.Entry<String, Integer> entry : site_resband.entrySet()){
            String s = entry.getKey();
            int b =site_bandwidth.get(s) - entry.getValue();
            if(b==0) continue;
            site_used.put(s,b);
        }
        return site_used;
    }

    public static void dispatch(){
        int time_size = timeList.size();
        //先对第一个时间做一次调度，后续再写全部时间的调度
        String T = timeList.get(0);
        //得到当前时刻，所有客户节点的需求流量
        HashMap<String, Integer> demandMap = demand.get(T);
        List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap.entrySet());
        //按需求流量的大小进行排序，排序方式为从大到小
        demandList.sort((o1,o2) -> o2.getValue()-o1.getValue());
        System.out.println(demandList);
        //调用max优先分配，返回一个节点使用情况的map
        HashMap<String, Integer> site_used = dispatchbasedMaxBandSite(demandList);
        System.out.println(site_used);


    }



    public static void main(String[] args) {
        init();
        dispatch();

    }
}