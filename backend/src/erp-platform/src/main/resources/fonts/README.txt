Source Han Sans CN - 字体资源放置说明
==========================================

V1.3.9 Sprint 13 · Story 13.2 · AC-13.2.1 资源嵌入
字体：思源黑体简体常规（SourceHanSansCN-Normal.ttf）
许可证：SIL Open Font License 1.1 / Apache 2.0（商用 OK · 见 LICENSE.txt）

⚠️ IMPL 阶段说明（dev agent 2026-06-14）
  由于 dev agent 沙箱网络受限，无法从 Adobe Fonts CDN 下载 ~10MB 字体二进制。
  本目录在 IMPL 提交时未包含实际字体文件。FontProvider 已实现：
    - 加载成功 → 使用思源黑体（跨 OS 中文保真度 100%）
    - 加载失败 → 自动降级到 JDK SansSerif（不阻塞渲染 + WARN 日志）
  部署前请按下方"下载指引"补充实际字体文件。

下载指引
========

方案 A：GitHub Adobe Fonts（推荐 · 与 Story 13.2 architect REVIEW 一致）
  1. 浏览器访问：
     https://github.com/adobe-fonts/source-han-sans/raw/release/SubsetOTF/CN/SourceHanSansCN-Normal.otf
  2. 保存为 SourceHanSansCN-Normal.otf（约 10MB）
  3. OTF 转 TTF（如需 TTF 格式）：
     - 使用 fonttools（pip install fonttools）执行 otf2ttf
     - 或直接用 OTF（OpenPDF / AWT Font.createFont(TRUETYPE_FONT, ...) 均支持 OTF）
  4. 放置到本目录：
     backend/src/erp-platform/src/main/resources/fonts/SourceHanSansCN-Normal.ttf

方案 B：Google Noto Sans CJK SC（与 Source Han Sans 同源不同名）
  https://github.com/notofonts/noto-cjk/raw/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf
  （注：Story 13.2 architect REVIEW 优先选用 Source Han Sans CN，Noto Sans CJK SC 备用）

方案 C：阿里 iconfont 镜像（境内访问友好）
  https://www.iconfont.cn/fonts/detail?cnid=IjQ6dLOddW27 （思源黑体简体）
  下载后放置到本目录

验证（部署后）
==============
  jar tf target/erp-backend-1.3.9.jar | grep SourceHanSans
    期望：fonts/SourceHanSansCN-Normal.ttf
  ls -lh backend/src/erp-platform/src/main/resources/fonts/SourceHanSansCN-Normal.ttf
    期望：~10MB（[8MB, 12MB]）

历史
====
  2026-06-14 · Story 13.2 IMPL · dev agent Opus 4.8
  License: SIL OFL 1.1 (Apache 2.0 compatible)