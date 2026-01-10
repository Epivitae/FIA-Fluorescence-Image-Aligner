# FIA: Fluorescence Image Aligner

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/Epivitae/FIA-Fluorescence-Image-Aligner?color=blue&label=Latest%20Version)](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases)
[![GitHub all releases](https://img.shields.io/github/downloads/Epivitae/FIA-Fluorescence-Image-Aligner/total?color=success&label=Downloads)](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases)
[![GitHub repo size](https://img.shields.io/github/repo-size/Epivitae/FIA-Fluorescence-Image-Aligner?color=orange)](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner)

[![Language](https://img.shields.io/badge/Language-Java-b07219.svg)](https://www.java.com/)
[![Build System](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Platform](https://img.shields.io/badge/Platform-ImageJ%2FFiji-1b2a4e?logo=imagej)](https://imagej.net/)
[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/graphs/commit-activity)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/Epivitae/FIA-Fluorescence-Image-Aligner?style=social)](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/stargazers)

---

**FIA (Fluorescence Image Aligner)** is an ImageJ/Fiji plugin designed for **intensity-invariant registration** of time-lapse microscopy data. 

Unlike standard registration tools (e.g., StackReg) based on minimizing Mean Square Error (MSE), FIA uses **OpenCV's Enhanced Correlation Coefficient (ECC)** algorithm. This makes it robust against:
* **Fluctuating signals** (e.g., Calcium imaging, Voltage imaging, flashing sensors).
* **Photobleaching** or unstable illumination.
* **Low signal-to-noise ratio** conditions.

---

## 🚀 Why FIA?

| Feature | StackReg / TurboReg | FIA (This Plugin) |
| :--- | :--- | :--- |
| **Core Algorithm** | Minimization of MSE | Maximization of Correlation (ECC) |
| **Best For** | Structural imaging (fixed brightness) | **Functional imaging** (fluctuating brightness) |
| **Flashing Neurons** | May cause artifacts (warping) | **Stable** (ignores brightness changes) |
| **Transformations** | Rigid, Affine, Projective | Euclidean, Affine, Homography |

## 🛠 Installation

1.  **Install Fiji**: Download and install [Fiji](https://imagej.net/software/fiji/).
2.  **Enable IJ-OpenCV**:
    * Open Fiji.
    * Go to `Help > Update...`.
    * Click `Manage Update Sites`.
    * Check **IJ-OpenCV**.
    * Click `Close` and then `Apply changes`.
3.  **Install FIA**:
    * Download the latest `.jar` from the [Releases](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/releases) page.
    * Drag and drop the file into Fiji's `plugins` folder.
    * Restart Fiji.

## 📖 Usage

1.  Open your time-lapse stack in Fiji.
2.  Go to `Plugins > FIA > Fluorescence Image Aligner`.
3.  **Select Parameters**:
    * **Transformation**: *Euclidean* (Rotation + Translation) is recommended for most biological samples.
    * **Reference**: Choose *Average Intensity Projection* for the best stability in calcium imaging.
4.  Click **OK**.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📚 Citation

If you use FIA in your research, please cite:
> Wang, K. (2026). FIA: Intensity-invariant registration for functional imaging. GitHub repository.

---
*Developed by Dr. Kui Wang, ION, CAS.*