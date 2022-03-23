package util;

import java.io.*;
import java.util.*;

/**
 * @author: HuangSiBo
 * @Description: 该类用来校验分配结果的正确性
 * @Data: Created in 17:25 2022/3/21
 */
public class Check {

    //检测客户节点的流量有没有全都分配出去
    public static void check_1(HashMap<String, HashMap<String, Integer>> demand,
                                  List<String> demandName,
                                  List<String> timeList,
                                  List<String> siteName,
                                  HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchStrategy){
        boolean ans = true;
        for(String time : timeList){
            for (int i = 0; i < demandName.size(); i++) {
                int sum = demand.get(time).get(demandName.get(i));
                HashMap<String, Integer> temp = dispatchStrategy.get(time).get(demandName.get(i));
                Collection<Integer> values = temp.values();
                int s = values.stream().mapToInt(Integer::intValue).sum();
                if(s != sum){
                    System.out.println("不合理");
                    System.out.println(time + "," + "客户:" + demandName.get(i) + "," +"边缘:" + siteName);
                    System.out.println("客户需求为:" + sum);
                    System.out.println("分配的流量为：" + s);
                    System.out.println("分配方案： " + temp);
                    System.out.println();
                    System.out.println();
                    ans = false;
                }
            }
        }
        if(ans){
            System.out.println("客户节点的流量已经全都分配出去");
        }
        return ;
    }

    //检测边缘节点接收的流量是否超量。打印边缘节点在不同时刻的流量分配情况
    public static void check_2(HashMap<String, Integer> site_bandwidth,
                               List<String> demandName,
                               List<String> timeList,
                               List<String> siteName,
                               HashMap<String, HashMap<String, HashMap<String, Integer>>> dispatchStrategy){
        //dispatchList存储边缘节点在不同时刻的分配情况
        HashMap<String, HashMap<String,Integer>> dispatchList = new HashMap<>();

        for(String site : siteName){
            HashMap<String,Integer> temp = new HashMap<>();
            Integer sum;
            for(String time : timeList){
                sum = 0;
                for(String demand_name : demandName){
                    sum+=dispatchStrategy.get(time).get(demand_name).getOrDefault(site, 0);
                }
                temp.put(time, sum);
                if(sum > site_bandwidth.get(site)){
                    System.out.println("边缘节点:" + site + "在" +time +"时刻" +", 流量溢出");
                }
                dispatchList.put(site, temp);
            }
            //System.out.println(dispatchList);
        }

        //计算每个边缘在时间序列上95%位置的值
        HashMap<String, Integer> res = new HashMap<>();
        Integer bindWidthSum = 0;
        for (String site : siteName){
            Collection<Integer> values = dispatchList.get(site).values();
            List<Integer> list = new ArrayList<>();
            list.addAll(values);
            Collections.sort(list);
            if(list.size() == 0){
                System.out.println(site + "的分配情况为空");
                continue;
            }

            System.out.println();
            System.out.println("边缘节点" + site + "的流量分配情况");
            for (int i = 0; i < list.size(); i++) {
                if(i == 0) System.out.print("[ ");
                System.out.print(list.get(i) + " ");
                if(i == list.size()-1) System.out.print(" ]");
            }

            int index = (int) (Math.ceil(list.size() * 0.95) - 1);
            int value = list.get(index);
            bindWidthSum += value;
            res.put(site, value);
        }

        System.out.println();
        System.out.println("所有边缘节点在95%位置的总带宽" + bindWidthSum);
    }

    //能够实现深拷贝
    public static <T extends Serializable> T MyClone(T object) {
        T result = null;
        try {
            ByteArrayOutputStream temp1 = new ByteArrayOutputStream();
            ObjectOutputStream temp2 = new ObjectOutputStream(temp1);
            temp2.writeObject(object);
            temp2.close();

            ByteArrayInputStream temp3 = new ByteArrayInputStream(temp1.toByteArray());
            ObjectInputStream temp4 = new ObjectInputStream(temp3);
            result = (T) temp4.readObject();
            temp4.close();
        } catch (Exception e) {
            System.out.println("clone object fail");
        }
        return result;
    }
}
