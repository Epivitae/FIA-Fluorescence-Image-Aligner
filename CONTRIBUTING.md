# Contributing to FIA

Thank you for your interest in contributing to FIA! We welcome help from the community to improve this tool.

## How to Contribute

### Reporting Bugs
If you find a bug, please create a [New Issue](https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner/issues) describing:
1.  The version of Fiji and FIA you are using.
2.  Steps to reproduce the error.
3.  Screenshots or sample data (if possible).

### Pull Requests
1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## Development Setup

This project uses **Maven**.
1.  Clone the repo.
2.  Run `mvn clean package` to build the plugin.
3.  The output `.jar` will be in the `target/` folder.

## Coding Style
* Please follow standard Java naming conventions.
* Keep dependencies minimal (IJ-OpenCV is the core dependency).