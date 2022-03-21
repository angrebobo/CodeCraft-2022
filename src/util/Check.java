package util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author: HuangSiBo
 * @Description: 该类用来校验分配结果的正确性
 * @Data: Created in 17:25 2022/3/21
 */
public class Check {

    //检测客户节点的流量有没有全都分配出去
    public static boolean check_1(HashMap<String, HashMap<String, Integer>> demand,
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
                    System.out.println("时间："+time + ", " + "客户节点：" + demandName.get(i) + ", " + "的需求为 " + sum);
                    System.out.println("分配的流量为：" + s);
                    System.out.println("分配方案： " + temp);
                    System.out.println();
                    System.out.println();
                    ans = false;
                }
                else {
//                    System.out.println("时间："+time + ", " + "客户节点：" + demandName.get(i) + ", " + "的需求为 " + sum);
//                    System.out.println("分配的流量为：" + s);
//                    System.out.println("分配方案：" + values);
                }
            }
        }
        return ans;
    }
}
