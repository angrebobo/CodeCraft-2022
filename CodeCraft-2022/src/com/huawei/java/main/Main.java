import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    public static void init(){
        String demandFile = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\demand.csv";
        String site_bandwidthFile = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\site_bandwidth.csv";
        String qosFile = "C:\\Users\\AngreBoBo\\Desktop\\华为\\线下调试数据\\线下调试数据\\data\\qos.csv";
        String line;
        String[] temp;
        //demandName存储客户节点的名称
        List<String> demandName = new ArrayList<>();
        //time存储各个时刻
        List<String> time = new ArrayList<>();
        //siteName存储边缘节点的名称
        List<String> siteName = new ArrayList<>();
        HashMap<String, Integer> site_bandwidth = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> demand = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> qos = new HashMap<>();

        //初始化边缘节点
        try(BufferedReader reader = new BufferedReader(new FileReader(site_bandwidthFile))) {
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                if( "site_name".equals(temp[0]) )
                    continue;
                siteName.add(temp[0]);
                site_bandwidth.put(temp[0], Integer.valueOf(temp[1]));
            }
            System.out.println(site_bandwidth);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //初始化客户节点
        try(BufferedReader reader = new BufferedReader(new FileReader(demandFile))) {
            while ( (line=reader.readLine()) != null ){
                temp = line.split(",");
                if( "mtime".equals(temp[0]) ){
                    demandName = transferArrayToList(temp);
                    //第0个元素是“mtime”，移除
                    demandName.remove(0);
                    continue;
                }
                //map存储<客户节点名称，客户节点在此刻需要的带宽>
                HashMap<String, Integer> map = new HashMap<>();
                for (int i = 0; i < demandName.size(); i++) {
                    map.put(demandName.get(i), Integer.valueOf(temp[i+1]));
                }
                time.add(temp[0]);
                //demand存储<时间，map>
                demand.put(temp[0], map);
            }
            System.out.println(demand);
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
                }

                HashMap<String, Integer> map = new HashMap<>();
                for (int i = 1; i < len; i++) {
                    map.put(name.get(i), Integer.valueOf(temp[i]));
                }
                qos.put(temp[0], map);
            }
            System.out.println(qos);
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

    public static void main(String[] args) {
        init();
    }
}