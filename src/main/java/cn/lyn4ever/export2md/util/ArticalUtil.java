package cn.lyn4ever.export2md.util;

import cn.lyn4ever.export2md.halo.ArticalAttras;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: musdrop
 * @description 文章工具类，解析文章属性
 */
public class ArticalUtil {


    // 从 BufferedReader 中解析文章属性
    public static ArticalAttras getAttributes(BufferedReader reader) {
        ArticalAttras attributes = new ArticalAttras();
        //设置默认值
        attributes.setTitle("无标题");
        attributes.setCategories(new ArrayList<>());
        attributes.setTags(new ArrayList<>());
        attributes.setDate(new Date());
        String line;
        boolean insideYaml = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  // 定义日期格式

        try {
            // 逐行读取
            reader.mark(1024);  // 标记当前位置
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
                        List<String> categories = new ArrayList<>();
                        //说明本行有类别
                        if(line.length()>11){
                            categories.add(line.substring(11).trim());
                        }
                        // 读取多行的 categories
                        reader.mark(1024);  // 标记当前位置
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            // 以 - 开头的行，表示一个 category，且不以 --- 开头
                            if (line.startsWith("-")&&!line.startsWith("---")) {
                                categories.add(line.substring(1).trim());
                                reader.mark(1024);  // 标记当前位置
                            } else {
                                //进入下一个属性了，回退一行
                                reader.reset();
                                break;
                            }
                        }
                        attributes.setCategories(categories);  // 设置 categories
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
                                //进入下一个属性了，回退一行
                                reader.reset();
                                break;
                            }
                        }
                        attributes.setTags(tags);  // 设置 tags
                    }
                    // 解析日期（date）
                    else if (line.startsWith("date:")) {
                        String dateString = line.substring(5).trim();
                        try {
                            // 将字符串转换为 Date 对象
                            Date date = dateFormat.parse(dateString);
                            attributes.setDate(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        // 其他属性，暂不处理
                    }
                }
                else {
                    reader.reset();// 回退至开头
                    break;  // 不在 YAML 部分，停止读取
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return attributes;
    }
}
