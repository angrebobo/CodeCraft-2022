package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author: HuangSiBo
 * @Description:
 * @Data: Created in 21:56 2022/3/21
 */
public class ToFile {

    /**
     * @Description 将调度策略写入到文件中
     * @param
     * @return
     */
    public static void writeToFile(String filepath, List<String> timeList, List<String> demandName, HashMap<String, HashMap<String, HashMap<String, Integer>>> result){

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
                    if(buffer.charAt(buffer.length() - 1) == ',')
                        //删除最后一个逗号
                        buffer.deleteCharAt(buffer.length() - 1);
                    //比赛的运行环境是Linux，所以手动添加换行符
                    buffer.append("\r\n");
                    bufferedWriter.write(buffer.toString());
//                    System.out.print(buffer);
                }
            }
        } catch (IOException e) {
            System.out.println("将调度方案写入文件失败");
        }
    }

    public static void writeLog(String filepath, HashMap<String, HashMap<String, String>> log){
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
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filepath))){
            for(String k : log.keySet()){
                StringBuffer buffer = new StringBuffer();
                buffer.append(k).append(": ").append("\r\n");
                buffer.append("weight: ").append(log.get(k).get("weight")).append("\r\n");
                buffer.append("alreadyDispatch: ").append(log.get(k).get("alreadyDispatch")).append("\r\n");
                buffer.append("second: ").append(log.get(k).get("second")).append("\r\n");
                buffer.append("\r\n").append("\r\n");
                bufferedWriter.write(buffer.toString());
            }
        } catch (IOException e) {
            System.out.println("写日志失败");
        }
    }

    public static HashMap<String, HashMap<String, HashMap<String, Integer>>> trans(
            HashMap<String, HashMap<String, HashMap<String, Integer>>> result1,
            HashMap<String, HashMap<String, HashMap<String, Integer>>> result2,
            List<String> siteName,
            List<String> demandName,
            List<String> timeList){

        HashMap<String, HashMap<String, HashMap<String, Integer>>> result = new HashMap<>(result2);

        for (String demand : demandName){
            for (String time : timeList){
                for (String site : siteName){
                    //该边缘节点是个死节点，连接不到任何客户节点
                    if(result1.get(time).getOrDefault(site, null) == null)
                        continue;
                    if(result1.get(time).get(site).containsKey(demand)){
                        HashMap<String, Integer> temp = result2.get(time).getOrDefault(demand, new HashMap<>());
                        temp.put(site, result1.get(time).get(site).get(demand));

                        result2.get(time).put(demand, temp);
                    }
                }
            }
        }

        return result;
    }
}
