<div align="center">
  <img src="src/main/resources/FIA.png" alt="FIA Logo" width="120">
  <h1>FIA: Fluorescence Image Aligner</h1>
  
  <p>
    <strong>Robust, Intensity-Invariant Motion Correction for Time-Lapse Microscopy</strong>
  </p>

  <p>
    <a href="https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases">
      <img src="https://img.shields.io/badge/release-v1.0.0-blue.svg" alt="Release">
    </a>
    <a href="https://opensource.org/licenses/MIT">
      <img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License">
    </a>
    <img src="https://img.shields.io/badge/platform-ImageJ%20%2F%20Fiji-red.svg" alt="Platform">
  </p>
</div>

---

## 📖 Overview

**FIA (Fluorescence Image Aligner)** is a lightweight ImageJ/Fiji plugin designed to correct motion artifacts (drift, jitter, rotation) in time-lapse fluorescence microscopy data.

Unlike conventional center-of-mass registration, FIA utilizes the **OpenCV ECC (Enhanced Correlation Coefficient)** algorithm. This makes it highly robust against **fluctuating fluorescence signals**, such as those seen in:
* Calcium Imaging (GCaMP)
* Voltage Imaging
* Bioluminescence recordings

FIA employs a **non-destructive** workflow, ensuring your raw data is never overwritten.

## ✨ Key Features

* **🛡️ Non-Destructive**: Always generates a new aligned stack (`FIA-[Filename]`), preserving your original data.
* **🧠 Intensity Invariant**: Accurately aligns images even when neurons flash on and off.
* **🚀 Warm Start Strategy**: Uses the transformation matrix from the previous frame as the initial guess for the next, preventing alignment loss in long recordings.
* **🌈 Multi-Channel Support**: Automatically detects the channel with the best signal-to-noise ratio (highest mean intensity) as the reference, and applies the correction to all channels.
* **📊 Quantitative Output**: Option to save the precise transformation matrix (`.csv`) for post-hoc analysis.

## 📥 Installation

1.  Download the latest **`FIA-1.0.0.jar`** from the [Releases Page](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases).
2.  Drag and drop the `.jar` file into your **Fiji** main window (or copy it to the `Fiji.app/plugins/` folder).
3.  **Restart Fiji**.

You will see the plugin under **Plugins > FIA > FIA Image Aligner**.

## 🎮 Usage Guide

### 1. Launch
Open your time-lapse stack (T-series) in Fiji, then run **Plugins > FIA > FIA Image Aligner**.

### 2. Controller Settings

<p align="center">
  <img src="images/FIA-main.png" alt="FIA Controller" width="200">
</p>

* **Transform Mode**:
    * **Translation**: Corrects x, y shifts only. Fastest, suitable for mechanical stage drift.
    * **Rigid (Recommended)**: Corrects x, y shifts + Rotation. Best for most biological samples (e.g., awake zebrafish/mice).
    * **Affine**: Adds Shear/Scale correction. Use only if tissue deformation is suspected.
    * *Tip: Click the **Help** button in the UI for details.*
* **Max Iterations**: (Default: 100) Maximum ECC iterations per frame. Increase if alignment fails.
* **Precision ($10^{-x}$)**: (Default: 5) Convergence threshold ($10^{-5}$).
* **Verbose Log**: Prints detailed convergence info to the Console.
* **Save Matrix (.csv)**: If checked, prompts to save the geometric transformation matrix after the run.

### 3. Result
Click **Run Alignment**. 
* A progress bar will show the status.
* Upon completion, a **new window** will open with the aligned stack.
* The raw data window remains untouched.

## ⚙️ Algorithm Details

FIA implements the parametric image alignment algorithm using the Enhanced Correlation Coefficient (ECC) maximization (Evangelidis & Psarakis, 2008). 

1.  **Reference Selection**: The first frame of the brightest channel is used as the anchor template.
2.  **Preprocessing**: Images are converted to `Float32` for sub-pixel precision.
3.  **Iterative Optimization**: The algorithm maximizes the correlation coefficient between the template and the warped current frame.
4.  **Warm Start**: $Matrix_{t}$ is initialized using $Matrix_{t-1}$. This ensures smooth trajectory tracking and speeds up convergence.

## 🛠️ Requirements

* **Fiji (ImageJ)**: Recent version recommended.
* **OpenCV**: This plugin relies on the OpenCV libraries bundled with modern Fiji (ImageJ2). No manual installation is usually required.

## 📜 License & Credits

**FIA** is developed by **Dr. Kui Wang** at the **Institute of Neuroscience (ION), Chinese Academy of Sciences (CAS)**.

Distributed under the **MIT License**. See `LICENSE` for more information.

Copyright © 2026 [www.cns.ac.cn](http://www.cns.ac.cn)