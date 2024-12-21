Halo 2.0 插件——文章导入导出插件
## 前言
本项目 fork 自 [Lyn4ever29/halo-plugin-export-md](https://github.com/Lyn4ever29/halo-plugin-export-md)，
并在此基础上进行了一些修改，完善了部分功能，修改了一些界面细节

## 功能说明
- 快速导出文章为markdown或者html文件
- 导出后打包下载
- 提供文章导入功能，支持导入markdown文件
- 支持导入的markdown文件包含属性，目前仅支持标题、日期、分类、标签

## 功能预览
- 文章列表界面的单文章导出

![https://github.com/Lyn4ever29/halo-plugin-export-md/assets/25952589/dd247afe-76f0-402f-9f3c-68b265d7b8ab](https://github.com/Lyn4ever29/halo-plugin-export-md/assets/25952589/dd247afe-76f0-402f-9f3c-68b265d7b8ab)

- 工具栏本工具界面的批量导出

![导出列表](https://github.com/Lyn4ever29/halo-plugin-export-md/assets/25952589/2404ae3c-582b-4f5e-b9b6-96f7b029af69)


## 安装
- 从[Release](https://github.com/musdrop/halo-plugin-export-md/releases)下载所需版本jar包本地安装
- 远程下载，下载链接
```
https://github.com/musdrop/halo-plugin-export-md/releases/download/V1.3.0/halo-plugin-export2doc-1.2.3.jar
```

## 说明
- 与插件[ToolBench](https://www.halo.run/store/apps/app-SsYlH)一起使用时存在不兼容的问题，导致文章无法查看。
- 如果您有任何问题或者好的建议，请提issue给我。

## 本仓库更新日志
- v1.3.0
  - 完善了对导入文章的属性的解析，支持自动创建未定义的标签和分类
  - 支持无属性文章，支持跳过暂未实现解析的属性
  - 目前解析默认开启
  - 支持多种categories格式：
  ```yaml
  categories: 开发
  ```
  ```yaml
  categories: 开发
  - 日志
  ```
  ```yaml
  categories: 
  - 开发
  - 日志
  ```

- v1.2.3 
  - 优化导入导出的图标错误，箭头向上导出，向下导入
  - 添加对导入的文章的属性的解析与加载，默认启用暂无开关，仅支持title，data，categories，tags，格式如下
  ```yaml
  ---
  title: 我的halo插件
  date: 2023-04-22
  categories: 开发
  tags:
  - spring
  - vue
  - 插件
  ---
  ```

## 原仓库更新日志
- v1.2.3 优化导入导出的 UI。（来自[@ruibaby](https://github.com/ruibaby)的PR）
  - 支持显示空状态，引导用户导出文章。
  - 支持导出/删除记录之后自动更新列表数据，不再需要手动刷新页面。
  - 使用 Halo 在全局注册的 Uppy 组件，减少构建产物体积。（已同步修改 plugin.yaml 的 requires 为 2.11）
  - 使用 Halo 官方新提供的 @halo-dev/ui-plugin-bundler-kit 用于构建插件。
  - 移除部分无用样式和注释。
- v1.2.2
  - 导出文章时，添加封面图字段
  - 添加权限控制，感谢[@chengzhongxue](https://github.com/chengzhongxue)的PR
- v1.2.1
  - 修复halo2.11版本升级后，导入功能失效的问题（由于Halo修改了文章发布机制，导致导入失效，已更新至最新代码）。
  - 如果是halo2.10版本，请使用**v1.1.4**
- v1.2.0
  - 更改导出文章压缩包目录为halo2工作目录
  - 修改有关草稿箱的说明，导入后的文章处于待发布状态，需要用户自行发布
- v1.1.4 在文章列表添加导出快捷方式，可以导出单文件。
- v1.1.0 
  - 支持导入Markdown文件
  - 导出的Mardkdown文件支持属性，属性示例如下：
  
  ```yaml
  ---
  title: 试试Nacos作注册中心和配置中心，爱不释手的感觉
  date: 2023-04-22 20:28:05
  auther: lyn4ever
  excerpt: 在使用SpringCloud做分布式微服务架构时，注册中心是必不可少的一个组件。
  permalink: /2022/166359134426
  categories:
    - java
    - springcloud
  tags:
    - springcloud
    - nacos
    - 注册中心
  ---
  ```
- v1.0.0
  - 简单导出功能

## Todo
- [x] 导出为Markdown、HTML文件
- [x] 导出的Markdown文件包含属性
- [x] Markdown文章导入
- [x] 导入Markdown时支持基本的属性解析
- [ ] 导入Markdown时支持属性自定义
- [ ] 根据筛选条件（日期、分类、标签等）导出
- [ ] 导出适配其他平台的目录格式，如VuePress、Hexo等
- [ ] 从其他平台导入博客（待定）
