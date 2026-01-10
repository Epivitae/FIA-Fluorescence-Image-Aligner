<div align="center">
  <img src="src/main/resources/FIA.png" alt="FIA Logo" width="120">
  <h1>FIA: 荧光图像配准工具 (Fluorescence Image Aligner)</h1>
  
  <p>
    <strong>针对延时显微成像的鲁棒、光强不敏感运动校正工具</strong>
  </p>

  <p align="center">
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases">
      <img src="https://img.shields.io/badge/release-v1.0.0-blue.svg" alt="Release">
    </a>    
    <a href="https://doi.org/10.5281/zenodo.18206931">
      <img src="https://zenodo.org/badge/DOI/10.5281/zenodo.18206931.svg" alt="DOI">
    </a>
    <img src="https://img.shields.io/badge/platform-ImageJ%20%2F%20Fiji-red.svg" alt="Platform">
    <a href="https://opensource.org/licenses/MIT">
      <img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License">
    </a>    
    <br> <img src="https://img.shields.io/github/repo-size/Epivitae/FIA-Fluorescence-Image-Aligner" alt="Repo Size">
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/commits/main">
      <img src="https://img.shields.io/github/last-commit/Epivitae/FIA-Fluorescence-Image-Aligner" alt="Last Commit">
    </a>
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases">
      <img src="https://img.shields.io/github/downloads/Epivitae/FIA-Fluorescence-Image-Aligner/total" alt="Downloads">
    </a>    
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/stargazers">
      <img src="https://img.shields.io/github/stars/Epivitae/FIA-Fluorescence-Image-Aligner?style=social" alt="GitHub Stars">
    </a>
  </p>
</div>

---

## 📖 项目简介 (Overview)

**FIA (Fluorescence Image Aligner)** 是一款轻量级的 ImageJ/Fiji 插件，专为校正延时荧光显微成像数据中的运动伪影（如漂移、抖动、旋转）而设计。

与传统的基于质心的配准方法不同，FIA 采用了 **OpenCV ECC（增强相关系数）算法**。这使得它对**荧光信号的剧烈波动**具有极高的鲁棒性，特别适用于以下场景：
* 钙成像 (Calcium Imaging / GCaMP)
* 电压成像 (Voltage Imaging)
* 生物发光记录 (Bioluminescence)

FIA 采用**无损 (Non-destructive)** 工作流，确保您的原始数据永远不会被覆盖。

## ✨ 核心特性

* **🛡️ 无损处理**: 始终生成一个新的已配准图像栈 (`FIA-[原文件名]`)，完整保留原始数据。
* **🧠 光强不敏感**: 即使在神经元频繁闪烁（信号忽明忽暗）的情况下，也能实现精准配准。
* **🚀 热启动策略**: 利用上一帧的变换矩阵作为当前帧的初始猜测 (Initial Guess)，有效防止长时程记录中的配准丢失。
* **🌈 多通道支持**: 自动检测信噪比最佳（平均强度最高）的通道作为参考，并将计算出的位移校正同步应用到所有通道。
* **📊 定量输出**: 提供保存精确几何变换矩阵 (`.csv`) 的选项，便于后续的轨迹分析。

## 📥 安装指南

1.  从 [Releases 页面](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases) 下载最新的 **`FIA-1.0.0.jar`** 文件。
2.  将 `.jar` 文件直接拖入 **Fiji** 主窗口（或复制到 `Fiji.app/plugins/` 文件夹中）。
3.  **重启 Fiji**。

安装完成后，您可以在 **Plugins > FIA > FIA Image Aligner** 中找到该插件。

## 🎮 使用指南

### 1. 启动
在 Fiji 中打开您的延时图像栈 (T-series)，然后运行 **Plugins > FIA > FIA Image Aligner**。

### 2. 控制器设置

<p align="center">
  <img src="images/FIA-main.png" alt="FIA Controller" width="200">
</p>

* **变换模式 (Transform Mode)**:
    * **Translation (平移)**: 仅校正 x, y 位移。速度最快，适用于简单的机械漂移。
    * **Rigid (刚体 - 推荐)**: 校正 x, y 位移 + 旋转。适用于大多数生物样本（如清醒斑马鱼、小鼠）。
    * **Affine (仿射)**: 增加剪切/缩放校正。仅在怀疑有组织形变时使用。
    * *提示: 点击界面上的 **Help** 按钮可查看详细说明。*
* **最大迭代次数 (Max Iterations)**: (默认: 100) 每帧 ECC 算法的最大迭代步数。如果配准失败，可尝试增加此值。
* **收敛精度 (Precision $10^{-x}$)**: (默认: 5) 迭代收敛阈值 ($10^{-5}$)。
* **详细日志 (Verbose Log)**: 在控制台打印详细的收敛信息。
* **保存矩阵 (Save Matrix .csv)**: 勾选后，将在运行结束后提示保存几何变换矩阵文件。

### 3. 运行结果
点击 **Run Alignment**。
* 进度条将显示处理进度。
* 完成后，会弹出一个**新窗口**显示配准后的图像栈。
* 原始数据窗口将保持不变。

## ⚙️ 算法原理

FIA 基于增强相关系数 (ECC) 最大化原理实现了参数化图像配准 (Evangelidis & Psarakis, 2008)。

1.  **参考帧选择**: 自动选择最亮通道的第一帧作为锚定模板 (Template)。
2.  **预处理**: 图像被转换为 `Float32` 格式以实现亚像素精度。
3.  **迭代优化**: 算法通过最大化模板与变形后的当前帧之间的相关系数来寻找最佳变换参数。
4.  **热启动 (Warm Start)**: $Matrix_{t}$ 的初始化值来源于 $Matrix_{t-1}$。这保证了运动轨迹追踪的平滑性并加速了收敛。

## 🛠️ 运行环境

* **Fiji (ImageJ)**: 建议使用较新版本。
* **OpenCV**: 本插件依赖于现代 Fiji (ImageJ2) 内置的 OpenCV 库，通常无需手动安装。

## 📜 开源协议与致谢

**FIA** 由 **中国科学院脑科学与智能技术卓越创新中心（神经科学研究所）王逵 博士** 开发。

本项目遵循 **MIT License** 开源协议。详情请参阅 `LICENSE` 文件。

Copyright © 2026 [www.cns.ac.cn](http://www.cns.ac.cn)