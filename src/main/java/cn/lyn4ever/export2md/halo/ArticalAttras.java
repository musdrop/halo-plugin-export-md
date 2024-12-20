package cn.lyn4ever.export2md.halo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticalAttras {
    private String title;
    private String categories;
    private List<String> tags;
    private Date date;
}
