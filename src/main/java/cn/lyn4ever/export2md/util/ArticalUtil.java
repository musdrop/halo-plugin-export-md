package cn.lyn4ever.export2md.util;

import cn.lyn4ever.export2md.halo.ArticalAttras;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArticalUtil {
    public static ArticalAttras getAttributes(BufferedReader reader) {
        ArticalAttras attributes = new ArticalAttras();
        String line;
        boolean insideYaml = false;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  // 定义日期格式

        try {
            // 逐行读取
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 判断是否在 YAML 部分
                if (line.startsWith("---")) {
                    if (insideYaml) {
                        break; // 遇到第二个---，停止读取属性部分
                    } else {
                        insideYaml = true; // 第一个---，开始解析属性
                    }
                } else if (insideYaml) {
                    // 解析标题（title）
                    if (line.startsWith("title:")) {
                        attributes.setTitle(line.substring(7).trim());
                    }
                    // 解析类别（categories）
                    else if (line.startsWith("categories:")) {
                        attributes.setCategories(line.substring(11).trim());
                    }
                    // 解析标签（tags）
                    else if (line.startsWith("tags:")) {
                        List<String> tags = new ArrayList<>();
                        // 读取多行的 tags
                        reader.mark(1024);  // 标记当前位置
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            // 以 - 开头的行，表示一个 tag，且不以 --- 开头
                            if (line.startsWith("-")&&!line.startsWith("---")) {
                                tags.add(line.substring(1).trim());
                                reader.mark(1024);  // 标记当前位置
                            } else {
                                //回退一行
                                reader.reset();
                                break;
                            }
                        }
                        attributes.setTags(tags);  // 设置 tags 为 List<String>
                    }
                    // 解析日期（date）
                    else if (line.startsWith("date:")) {
                        String dateString = line.substring(5).trim();
                        try {
                            // 将字符串转换为 Date 对象
                            Date date = dateFormat.parse(dateString);
                            attributes.setDate(date);  // 假设 ArticalAttras 有 setDate(Date date) 方法
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return attributes;
    }
}
