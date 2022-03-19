import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    //demandName存储客户节点的名称
    static List<String> demandName = new ArrayList<>();
    //time存储各个时刻
    static List<String> time = new ArrayList<>();
    //siteName存储边缘节点的名称
    static List<String> siteName = new ArrayList<>();
    //qos_constraint存储qos的上限值
    static Integer qos_constraint;
    //site_bandwidth存储边缘节点的带宽上限，格式为<边缘节点名称，带宽上限>
    static HashMap<String, Integer> site_bandwidth = new HashMap<>();
    //demand存储每个时刻客户节点的带宽需求，格式为<时刻，<客户节点名称，带宽需求>>
    static HashMap<String, HashMap<String, Integer>> demand = new HashMap<>();
    //qos存储边缘节点和客户节点之间的qos，格式为<边缘节点名称，<客户节点名称，qos>>
    static HashMap<String, HashMap<String, Integer>> qos = new HashMap<>();

    /**
     * @Description 初始化方法，读入文件并存储到本地
     * @param
     * @return
     */
    public static void init(){
        String demandFile = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\demand.csv";
        String site_bandwidthFile = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\site_bandwidth.csv";
        String qosFile = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\qos.csv";
        String qos_config = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\config.ini";
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
                time.add(temp[0]);
                //demand存储<时间，map>
                demand.put(temp[0], map);
            }
//            System.out.println("demand: " + demand);
//            System.out.println("demandName: " + demandName);
//            System.out.println("time: " + time);
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
                qos.put(temp[0], map);
            }
//            System.out.println("qos: " + qos);
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

    }



    public static void main(String[] args) {
        init();


    }
}