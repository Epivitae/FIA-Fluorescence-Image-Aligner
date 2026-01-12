<div align="center">
  <img src="src/main/resources/FIA.png" alt="FIA Logo" width="120">
  <h1>FIA: 荧光图像配准工具 (v3.2.0)</h1>
  
  <p>
    <strong>专为 ImageJ/Fiji 打造的双引擎、抗干扰运动校正插件</strong>
  </p>

  <p align="center">
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases">
      <img src="https://img.shields.io/badge/release-v3.2.0-blue.svg" alt="Release">
    </a>    
    <a href="https://doi.org/10.5281/zenodo.18211241">
      <img src="https://zenodo.org/badge/DOI/10.5281/zenodo.18211241.svg" alt="DOI">
    </a>
    <img src="https://img.shields.io/badge/platform-ImageJ%20%2F%20Fiji-red.svg" alt="Platform">
    <a href="https://opensource.org/licenses/MIT">
      <img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License">
    </a>    
  </p>
</div>

---

## 📖 简介

**FIA (Fluorescence Image Aligner)** 是一款专为荧光显微成像（如钙成像、电压成像）设计的 ImageJ/Fiji 配准插件。

**v3.2.0 里程碑版本**引入了革命性的 **“双引擎架构”**，完美融合了 OpenCV 的高精度与经典算法的高鲁棒性，能够解决从**亚像素级微颤**到**剧烈样本漂移**的各类运动伪影问题。

<div align="center">

![fia-reg](/images/compressed-fia-reg.gif)

</div>

## ✨ v3.2.0 核心特性

* **🚀 双引擎核心 (Dual-Engine)**：
    * **OpenCV ECC (默认)**：基于增强相关系数算法，提供亚像素级精度，支持旋转校正。适合高质量的功能成像数据。
    * **Legacy Stabilizer (经典)**：复刻了经典的 Lucas-Kanade 稳像算法。在处理**大幅度位移**（如动物剧烈运动）时具有极强的鲁棒性。
* **🌊 弹性配准 (Elastic Mode)**：新增基于**密集光流法 (Dense Optical Flow)** 的非刚性配准，可修复组织的生长、挤压或扭曲变形。
* **⚓ 动态锚点 ("所见即锚点")**：不再强制以第一帧为基准。FIA 会自动读取您**当前正在查看的时间帧**作为参考基准，向前后进行双向配准。
* **🧠 智能界面 (Smart UI)**：根据选择的引擎自动隐藏无关参数，界面清爽简洁。
* **📂 统一归档**：现已集成至 `Plugins > Biosensor Tool` 菜单下，方便查找。

## 📥 安装方法

1.  在 [Releases 页面](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases) 下载最新的 **`FIA-3.2.0.jar`**。
2.  将 `.jar` 文件直接拖入 **Fiji** 主窗口（或复制到 `Fiji.app/plugins/` 目录）。
3.  **重启 Fiji**。

安装后，插件将出现在菜单栏：**Plugins > Biosensor Tool > FIA Image Aligner**。

## 🎮 使用指南

### 1. 引擎选择建议

| 引擎 | 适用场景 | 优势 | 劣势 |
| :--- | :--- | :--- | :--- |
| **OpenCV** | **功能成像** (钙/电压信号) | 精度极高，支持旋转校正 | 对极大幅度位移可能失效 |
| **Legacy** | **行为学/大幅度漂移** | 极强的鲁棒性，支持滚动更新 | 不支持旋转校正 |
| **Elastic** | **软组织/发育生物学** | 可修复局部形变 | 计算量较大 |

### 2. 参数设置

* **Global Modes (第一步：整体配准)**:
    * **Translation**: 仅校正平移 (XY)。
    * **Rigid**: 校正平移 + 旋转 (仅 OpenCV)。
    * **Affine**: 校正平移 + 旋转 + 缩放/剪切。
* **Local Mode (第二步：局部配准)**:
    * **Elastic**: 使用光流法校正局部扭曲。
* **关键参数**:
    * **Max Iterations**: 最大迭代次数 (默认 200)。
    * **Pyramid Levels (Legacy)**: 金字塔层数。位移越大，需要的层数越高 (默认 1)。
    * **Update Coeff (Legacy)**: 参考帧更新系数。`1.0` = 固定参考帧 (配准)；`0.90` = 滚动更新 (稳像)。

### 3. 操作流程
1.  打开图像栈 (Stack)。
2.  拖动时间滑块，找到图像质量最好、位置最正的一帧 (例如第 50 帧)。
3.  保持滑块在第 50 帧，点击 **Run Alignment**。
4.  程序将自动以第 50 帧为锚点，完成所有帧的配准。

## 📜 引用与致谢

如果您在研究中使用了 FIA，请引用：

> **Wang, K. (2026).** *FIA: Fluorescence Image Aligner - Robust Motion Correction for ImageJ/Fiji (v3.2.0).* Zenodo. https://doi.org/10.5281/zenodo.18218148

**开发者**: Dr. Kui Wang
**团队/出品**: [www.cns.ac.cn](http://www.cns.ac.cn)

Copyright © 2026 [www.cns.ac.cn](http://www.cns.ac.cn)