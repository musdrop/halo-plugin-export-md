package cn.lyn4ever.export2md.halo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

/**
 * @Author musdrop
 * @description 文章的属性
 * @date 2024/12/20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticalAttras {
    private String title;
    private List<String> categories;
    private List<String> tags;
    private Date date;
}
