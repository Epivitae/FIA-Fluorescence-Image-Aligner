<div align="center">
  <img src="src/main/resources/FIA.png" alt="FIA Logo" width="120">
  <h1>FIA: Fluorescence Image Aligner</h1>
  
  <p>
    <strong>Dual-Engine Motion Correction & Elastic Registration for ImageJ/Fiji</strong>
  </p>

  <p align="center">
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases">
      <img src="https://img.shields.io/badge/release-v3.0.0-blue.svg" alt="Release">
    </a>    
    <a href="https://doi.org/10.5281/zenodo.18211241">
      <img src="https://zenodo.org/badge/DOI/10.5281/zenodo.18211241.svg" alt="DOI">
    </a>
    <img src="https://img.shields.io/badge/platform-ImageJ%20%2F%20Fiji-red.svg" alt="Platform">
    <a href="https://opensource.org/licenses/MIT">
      <img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License">
    </a>    
    <br> 
    <img src="https://img.shields.io/github/repo-size/Epivitae/FIA-Fluorescence-Image-Aligner" alt="Repo Size">
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

## 📖 Overview

**FIA (Fluorescence Image Aligner) v3.0.0** is a major milestone release introducing a **Dual-Engine Architecture** to solve the full spectrum of motion artifacts in fluorescence microscopy. 

Whether you are dealing with sub-pixel jitter in functional imaging (Calcium/Voltage) or massive displacements in behaving animals, FIA provides a unified solution. It bridges the gap between high-precision rigid alignment and robust legacy stabilization, while adding **Elastic Registration** for soft tissue deformation.

## ✨ Key Features (v3.0.0)

* **🚀 Dual-Engine Core**:
    * **OpenCV ECC**: Best for high-precision, sub-pixel rigid alignment.
    * **Legacy Stabilizer**: A Java port of the classic Lucas-Kanade algorithm (Kang Li), superior for large displacements and robust stabilization.
* **🌊 Elastic Registration**: Uses **Dense Optical Flow** to correct non-rigid deformations (e.g., tissue growth, squashing, complex warping).
* **⚓ Dynamic Anchor ("What You See Is What You Anchor")**: Alignment is no longer forced to Frame 1. FIA automatically uses the **currently viewed frame** as the reference, allowing you to anchor to the most stable moment in your recording.
* **🧠 Smart UI**: The interface automatically adapts, hiding irrelevant parameters based on the selected engine and mode.
* **🛡️ Non-Destructive**: Always generates a new aligned stack (`FIA-Result`), ensuring your raw data is never modified.
* **📂 Biosensor Tool Suite**: Now integrated into the standardized `Plugins > Biosensor Tool` menu.

## 📥 Installation

1.  Download the latest **`FIA-3.0.0.jar`** from the [Releases Page](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases).
2.  Drag and drop the `.jar` file into your **Fiji** main window (or copy it to the `Fiji.app/plugins/` folder).
3.  **Restart Fiji**.

You will see the plugin under **Plugins > Biosensor Tool > FIA Image Aligner**.

## 🎮 Usage Guide

### 1. Engine Selection
FIA v3 offers different engines for different scenarios:

| Engine | Best For | Pros | Cons |
| :--- | :--- | :--- | :--- |
| **OpenCV** | **Functional Imaging** (Ca2+, Voltage) | High Precision, True Rigid Rotation | May fail on very large shifts |
| **Legacy** | **Behaving Animals / Large Drift** | Extremely Robust, Rolling Reference | No Rotation support |
| **Elastic** | **Soft Tissue / Developmental** | Fixes Deformation | Computationally heavier |

### 2. Controller Settings

<p align="center">
  <img src="images/FIA-main.png" alt="FIA Controller" width="250">
  <br><em>(The new Smart UI in v3.0.0)</em>
</p>

* **Global Modes (Step 1)**:
    * **Translation**: XY shift only.
    * **Rigid**: XY shift + Rotation (OpenCV only).
    * **Affine**: Shift + Rotation + Scale/Shear.
* **Local Mode (Step 2)**:
    * **Elastic**: Corrects local deformations using Optical Flow.
* **Parameters**:
    * **Max Iterations**: (OpenCV/Legacy) Max loops per frame (Default: 200).
    * **Pyramid Levels**: (Legacy) Higher levels handle larger displacements (Default: 1).
    * **Update Coeff**: (Legacy) `1.0` = Fixed Reference (Registration); `0.90` = Rolling Reference (Stabilization).
    * **Flow WinSize**: (Elastic) Window size for optical flow. Default `5` for fine details.

### 3. Dynamic Anchor Strategy
1.  Scroll through your stack to find a "perfect" frame (good focus, centered).
2.  Leave the slider on that frame (e.g., Frame 50).
3.  Click **Run Alignment**.
4.  FIA will align Frames 1-49 *forward* to Frame 50, and Frames 51-End *backward* to Frame 50.

## ⚙️ Algorithm Details

* **OpenCV Engine**: Implements the Parametric Image Alignment using Enhanced Correlation Coefficient (ECC) maximization (Evangelidis & Psarakis, 2008). It is intensity-invariant.
* **Legacy Engine**: Implements a pyramidal Lucas-Kanade optical flow algorithm for global motion estimation. It uses a "Rolling Reference" update mechanism to handle gradual drift smoothly.
* **Elastic Mode**: Utilizes Farneback's Dense Optical Flow to calculate a pixel-wise displacement field, remapping the image to correct non-linear distortions.

## 🛠️ Requirements

* **Fiji (ImageJ)**: Recent version recommended.
* **OpenCV**: This plugin relies on the OpenCV libraries bundled with modern Fiji (ImageJ2).

## 📜 Citation

If you use FIA in your research, please cite:

> **Wang, K. (2026).** *FIA: Fluorescence Image Aligner - Robust Motion Correction for ImageJ/Fiji (v3.0.0).* Zenodo. https://doi.org/10.5281/zenodo.18211241

## 📜 License

**FIA** is developed by **Dr. Kui Wang** at the **Institute of Neuroscience (ION), Chinese Academy of Sciences (CAS)**.

Distributed under the **MIT License**. See `LICENSE` for more information.

Copyright © 2026 [www.cns.ac.cn](http://www.cns.ac.cn)