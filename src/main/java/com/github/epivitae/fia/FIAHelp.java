package com.github.epivitae.fia;

/**
 * Helper class to store static help content and citation info.
 * Keeps the main controller code clean.
 */
public class FIAHelp {

    public static String getManual() {
        return "<html><body style='width: 400px; font-family: Arial; font-size: 11px;'>" +
                "<h3>🔍 Engine Guide</h3>" +
                "<ul>" +
                "<li><b>OpenCV (Default):</b> High precision ECC algorithm. Supports true Rigid (Rotation).</li>" +
                "<li><b>Legacy:</b> Based on 'Image Stabilizer' (Kang Li). Robust for large shifts. (No Rigid)</li>" +
                "</ul>" +
                
                "<hr>" +
                
                "<h3>🛠 Mode Selection</h3>" +
                "<b><font color='#2164C8'>Step 1: Global (Rigid/Affine)</font></b>" +
                "<ul>" +
                "<li><b>Translation:</b> XY shift only.</li>" +
                "<li><b>Rigid:</b> Shift + Rotation. (Requires OpenCV)</li>" +
                "<li><b>Affine:</b> Shift + Rotation + Scale/Shear.</li>" +
                "</ul>" +
                
                "<b><font color='#D25000'>Step 2: Local (Deformable)</font></b>" +
                "<ul>" +
                "<li><b>Elastic:</b> Uses Optical Flow to fix non-rigid deformation (growth/squashing).<br>" +
                "<i>*Best used after Step 1 has aligned the general position.</i></li>" +
                "</ul>" +
                
                "<hr>" +
                
                "<h3>⚙️ Key Parameters</h3>" +
                "<ul>" +
                "<li><b>Update Coeff (Legacy):</b> Default <b>0.90</b>. <br>Controls reference frame update. 1.0 = Fixed Ref, < 1.0 = Rolling Ref.</li>" +
                "<li><b>Flow WinSize (Elastic):</b> Default <b>5</b>. <br>Small (5-10) for local jitter; Large (20+) for global shape.</li>" +
                "</ul>" +
                
                "<hr>" +
                "<b>📚 Detailed Guide & Source Code:</b><br>" +
                "https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner" +
                
                "</body></html>";
    }

    public static String getAbout(String version) {
        return "<html><body style='width: 350px; font-family: Arial; font-size: 11px;'>" +
                "<h2>FIA Image Aligner</h2>" +
                "<b>Version: " + version + "</b><br>" +
                "(c) 2026 Kui Wang<br><br>" +
                
                "<b>Repository:</b><br>" +
                "https://github.com/Epivitae/FIA-Fluorescence-Image-Aligner<br><br>" +
                
                "<b>Citation:</b><br>" +
                "Wang, K. (2026). <i>FIA: Fluorescence Image Aligner - Robust Motion Correction for ImageJ/Fiji (v1.0.0).</i> Zenodo.<br>" +
                "https://doi.org/10.5281/zenodo.18206932" +
                "</body></html>";
    }
}