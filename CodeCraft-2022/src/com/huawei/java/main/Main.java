import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    //qos_s存储边缘节点和客户节点之间的qos，格式为<客户节点名称，<边缘节点名称，qos>>。为什么要多存一份？因为可以用不同的方式来拿数据。
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
//            System.out.println("site_bandwidth: " + site_bandwidth);
//            System.out.println("siteName: " + siteName);
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
            System.out.println("qos_s: " + qos_s);
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

    public static void dispatch(){
        int time_size = timeList.size();
        //先对第一个时间做一次调度，后续再写全部时间的调度
        String T = timeList.get(0);
        //得到当前时刻，所有客户节点的需求流量
        HashMap<String, Integer> demandMap = demand.get(T);
        List<Map.Entry<String, Integer>> demandList = new ArrayList<>(demandMap.entrySet());
        //按需求流量的大小进行排序，排序方式为从大到小
        demandList.sort((o1,o2) -> o2.getValue()-o1.getValue());
        System.out.println("demandList: " + demandList);

        //先处理最大流量需求的客户节点，取出该客户节点和所有边缘节点的qos进行筛选，选出小于qos_config
        HashMap<String, Integer> siteMap = qos_s.get(demandList.get(0).getKey());
        List<Map.Entry<String, Integer>> siteList = new ArrayList<>(siteMap.entrySet());
        //过滤出小于qos_config的边缘节点
        siteList = siteList.stream().filter(o1 -> o1.getValue()<400).collect(Collectors.toList());
//        System.out.println("siteList: " + siteList);




    }



    public static void main(String[] args) {
        init();
        dispatch();

    }
}