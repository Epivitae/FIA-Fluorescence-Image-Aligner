package com.github.epivitae.fia;

/**
 * PROJECT: FIA (Fluorescence Image Aligner)
 * AUTHOR: Kui Wang
 * NOTE: Version and Release Notes are auto-loaded from pom.xml
 * RECOMMENDED VERSION: v0.1.4 (Non-destructive Output & Help UI Fix)
 */

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import net.imagej.ImageJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack; // New import
import ij.WindowManager;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.io.SaveDialog;

import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.video.Video;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Plugin(type = Command.class, menuPath = "Plugins>FIA>FIA Image Aligner")
public class FIA_Command implements Command {

    private static String APP_VERSION = "Unknown";

    @Override
    public void run() {
        loadVersionInfo();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new FIAGui().setVisible(true);
        });
    }

    private void loadVersionInfo() {
        try (InputStream input = getClass().getResourceAsStream("/fia-version.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                APP_VERSION = prop.getProperty("version", "v0.0.0");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class FIAGui extends JFrame {
        
        private JToggleButton btnTranslation, btnRigid, btnAffine;
        private JButton btnHelp;
        private JTextField txtMaxIter, txtEpsilon;
        private JCheckBox chkLog, chkSaveMatrix;
        private JButton btnRun;
        private JProgressBar progressBar;
        private JLabel statusLabel;

        // Fonts
        private final Font FONT_HEADER_TITLE = new Font("Arial", Font.BOLD, 18);
        private final Font FONT_SECTION_HEAD = new Font("Arial", Font.BOLD, 11);
        private final Font FONT_LABEL = new Font("Arial", Font.PLAIN, 12);
        private final Font FONT_INPUT = new Font("Arial", Font.PLAIN, 13);
        private final Font FONT_BTN_RUN = new Font("Arial", Font.BOLD, 13);
        private final Font FONT_BTN_MODE = new Font("Arial", Font.BOLD, 12);
        private final Font FONT_SMALL = new Font("Arial", Font.PLAIN, 10);
        private final Font FONT_CHECKBOX = new Font("Arial", Font.PLAIN, 11);

        private final Color COLOR_THEME = new Color(33, 100, 200); 
        
        public FIAGui() {
            setTitle("FIA Controller " + APP_VERSION);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setResizable(false);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            setContentPane(mainPanel);

            // --- 1. Header ---
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            headerPanel.setOpaque(false);

            ImageIcon logoIcon = loadLogo();
            if (logoIcon != null) {
                JLabel logoLabel = new JLabel(logoIcon);
                headerPanel.add(logoLabel);
            }

            JPanel textBlock = new JPanel();
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
            textBlock.setOpaque(false);
            
            JLabel titleLabel = new JLabel("FIA Image Aligner");
            titleLabel.setFont(FONT_HEADER_TITLE);
            titleLabel.setForeground(COLOR_THEME);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textBlock.add(titleLabel);
            
            int currentYear = Year.now().getValue();
            String metaText = String.format("v%s  |  © %d www.cns.ac.cn", APP_VERSION, currentYear);
            JLabel metaLabel = new JLabel(metaText);
            metaLabel.setFont(FONT_SMALL);
            metaLabel.setForeground(Color.GRAY);
            metaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textBlock.add(metaLabel);

            headerPanel.add(textBlock);
            headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(headerPanel);
            
            mainPanel.add(Box.createVerticalStrut(15));

            // --- 2. Main Split Panel ---
            JPanel splitPanel = new JPanel(new BorderLayout(5, 0));
            splitPanel.setBorder(createRiaBorder("Alignment Parameters"));
            splitPanel.setOpaque(false);
            
            // === LEFT COLUMN (Mode + Help) ===
            JPanel leftCol = new JPanel();
            leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
            leftCol.setOpaque(false);
            leftCol.setPreferredSize(new Dimension(105, 140)); 

            JLabel lblMode = new JLabel("Transform Mode");
            lblMode.setFont(FONT_SECTION_HEAD);
            lblMode.setForeground(Color.DARK_GRAY);
            lblMode.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftCol.add(lblMode);
            leftCol.add(Box.createVerticalStrut(5));

            btnTranslation = createVerticalToggle("Translation");
            btnTranslation.addActionListener(e -> selectMode(btnTranslation));
            btnRigid = createVerticalToggle("Rigid");
            btnRigid.addActionListener(e -> selectMode(btnRigid));
            btnAffine = createVerticalToggle("Affine");
            btnAffine.addActionListener(e -> selectMode(btnAffine));

            leftCol.add(btnTranslation);
            leftCol.add(Box.createVerticalStrut(4));
            leftCol.add(btnRigid);
            leftCol.add(Box.createVerticalStrut(4));
            leftCol.add(btnAffine);
            
            // Help Button (Half width, no question mark)
            leftCol.add(Box.createVerticalStrut(8));
            btnHelp = new JButton("Help");
            btnHelp.setFont(new Font("Arial", Font.PLAIN, 10)); // 更小字体
            btnHelp.setMargin(new Insets(1,0,1,0));
            btnHelp.setFocusPainted(false);
            btnHelp.setBackground(new Color(245, 245, 245)); // 更淡的背景
            btnHelp.setForeground(Color.GRAY);
            // 限制最大宽度为 50，实现“半宽”效果
            btnHelp.setMaximumSize(new Dimension(50, 20)); 
            btnHelp.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnHelp.addActionListener(e -> showHelpDialog());
            leftCol.add(btnHelp);
            
            selectMode(btnRigid);
            splitPanel.add(leftCol, BorderLayout.WEST);

            // === SEPARATOR ===
            JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
            sep.setForeground(Color.LIGHT_GRAY);
            splitPanel.add(sep, BorderLayout.CENTER);

            // === RIGHT COLUMN (Compact Settings) ===
            JPanel rightCol = new JPanel();
            rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
            rightCol.setOpaque(false);
            rightCol.setBorder(new EmptyBorder(0, 8, 0, 0));

            // Iterations
            addCompactField(rightCol, "Max Iterations:", txtMaxIter = new JTextField("100"));
            
            // Precision
            addCompactField(rightCol, "<html>Precision (10<sup>-x</sup>):</html>", txtEpsilon = new JTextField("5"));

            rightCol.add(Box.createVerticalStrut(5));
            
            // Checkboxes
            chkLog = new JCheckBox("Verbose Log");
            chkLog.setFont(FONT_CHECKBOX);
            chkLog.setFocusPainted(false);
            chkLog.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightCol.add(chkLog);

            chkSaveMatrix = new JCheckBox("Save Matrix (.csv)");
            chkSaveMatrix.setFont(FONT_CHECKBOX);
            chkSaveMatrix.setFocusPainted(false);
            chkSaveMatrix.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightCol.add(chkSaveMatrix);

            splitPanel.add(rightCol, BorderLayout.EAST);
            
            mainPanel.add(splitPanel);
            mainPanel.add(Box.createVerticalStrut(10));

            // --- 3. Run Button ---
            btnRun = new JButton("Run Alignment");
            btnRun.setFont(FONT_BTN_RUN);
            btnRun.setForeground(COLOR_THEME);
            btnRun.setBackground(Color.WHITE);
            btnRun.setFocusPainted(false);
            btnRun.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnRun.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
            btnRun.addActionListener(this::startAlignment);
            
            mainPanel.add(btnRun);
            mainPanel.add(Box.createVerticalStrut(5));

            // --- 4. Progress ---
            progressBar = new JProgressBar(0, 100);
            progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressBar.setPreferredSize(new Dimension(200, 6));
            progressBar.setForeground(COLOR_THEME);
            mainPanel.add(progressBar);

            statusLabel = new JLabel("Ready");
            statusLabel.setFont(FONT_SMALL);
            statusLabel.setForeground(Color.GRAY);
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(statusLabel);

            pack();
            setLocationRelativeTo(null);
        }

        // --- Helpers ---

        private void showHelpDialog() {
            String msg = "<html><body style='width: 300px; font-family: Arial; font-size: 10px;'>" +
                    "<b>Transformation Modes:</b><br>" +
                    "<ul>" +
                    "<li><b>Translation:</b> Correction for x, y shifts only. (Fastest)</li>" +
                    "<li><b>Rigid:</b> Shifts (x, y) + Rotation. <span style='color:blue'>(Recommended)</span></li>" +
                    "<li><b>Affine:</b> Rigid + Shear/Scale. Use for tissue deformation.</li>" +
                    "</ul>" +
                    "<b>Parameters:</b><br>" +
                    "<ul>" +
                    "<li><b>Max Iterations:</b> Max ECC steps per frame. Default 100.</li>" +
                    "<li><b>Precision (10<sup>-x</sup>):</b> Convergence threshold. 5 means 1e-5.</li>" +
                    "</ul>" +
                    "</body></html>";
            JOptionPane.showMessageDialog(this, msg, "FIA Help", JOptionPane.INFORMATION_MESSAGE);
        }

        private void addCompactField(JPanel container, String labelText, JTextField field) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS)); 
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT); 
            
            JLabel lbl = new JLabel(labelText);
            lbl.setFont(FONT_LABEL);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT); 
            row.add(lbl);
            
            field.setFont(FONT_INPUT);
            field.setMaximumSize(new Dimension(70, 24)); 
            field.setAlignmentX(Component.LEFT_ALIGNMENT); 
            row.add(field);
            
            container.add(row);
            container.add(Box.createVerticalStrut(6));
        }

        private JToggleButton createVerticalToggle(String text) {
            JToggleButton btn = new JToggleButton(text);
            btn.setFont(FONT_BTN_MODE); 
            btn.setFocusPainted(false); 
            btn.setMargin(new Insets(4, 5, 4, 5));
            btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 28));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setBackground(Color.WHITE); 
            btn.setForeground(Color.BLACK); 
            btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            return btn;
        }

        private ImageIcon loadLogo() {
            java.net.URL imgURL = getClass().getResource("/FIA.png");
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                Image img = icon.getImage().getScaledInstance(-1, 40, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
            return null;
        }

        private Border createRiaBorder(String title) {
            TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title);
            tb.setTitleFont(new Font("Arial", Font.BOLD, 12)); tb.setTitleColor(COLOR_THEME);
            return new CompoundBorder(tb, new EmptyBorder(8, 8, 8, 8));
        }

        private void selectMode(JToggleButton target) {
            btnTranslation.setSelected(false); btnRigid.setSelected(false); btnAffine.setSelected(false);
            target.setSelected(true); updateToggleStyles();
        }

        private void updateToggleStyles() { styleBtn(btnTranslation); styleBtn(btnRigid); styleBtn(btnAffine); }

        private void styleBtn(JToggleButton btn) {
            if (btn.isSelected()) {
                btn.setForeground(COLOR_THEME); btn.setBackground(Color.WHITE); btn.setBorder(BorderFactory.createLineBorder(COLOR_THEME, 2));
            } else {
                btn.setForeground(Color.BLACK); btn.setBackground(Color.WHITE); btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            }
        }

        private void startAlignment(ActionEvent e) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp == null) { JOptionPane.showMessageDialog(this, "No image found."); return; }
            btnRun.setEnabled(false); btnRun.setText("Aligning..."); statusLabel.setText("Initializing...");
            
            String mode = "Rigid";
            if (btnTranslation.isSelected()) mode = "Translation"; else if (btnAffine.isSelected()) mode = "Affine";
            
            int maxIter = 100; int eps = 5;
            try { maxIter = Integer.parseInt(txtMaxIter.getText()); eps = Integer.parseInt(txtEpsilon.getText()); } catch (NumberFormatException ex) {}
            
            // 启动线程
            new AlignmentWorker(imp, mode, maxIter, eps, chkLog.isSelected(), chkSaveMatrix.isSelected()).execute();
        }

        class AlignmentWorker extends SwingWorker<Void, Integer> {
            ImagePlus srcImp; // 原始图像
            ImagePlus resImp; // 结果图像 (新)
            String mode; int maxIter, eps; boolean verbose; boolean saveMatrix;
            List<String> matrixLog = new ArrayList<>();
            
            public AlignmentWorker(ImagePlus imp, String mode, int maxIter, int eps, boolean verbose, boolean saveMatrix) {
                this.srcImp = imp; this.mode = mode; this.maxIter = maxIter; this.eps = eps; this.verbose = verbose; this.saveMatrix = saveMatrix;
            }
            
            @Override protected Void doInBackground() throws Exception {
                publish(0);
                try { OpenCV.loadShared(); } catch (Throwable e) { IJ.log("OpenCV Error: " + e.getMessage()); return null; }
                if (saveMatrix) matrixLog.add("Frame,m00,m01,m02,m10,m11,m12");

                // [Step 1] Duplicate the stack (Non-destructive)
                ImageStack srcStack = srcImp.getStack();
                ImageStack resStack = srcStack.duplicate(); // Deep copy
                resImp = new ImagePlus("FIA-" + srcImp.getTitle(), resStack);
                // Copy calibration (pixel size, etc)
                resImp.setCalibration(srcImp.getCalibration().copy());
                resImp.setDimensions(srcImp.getNChannels(), srcImp.getNSlices(), srcImp.getNFrames());
                // Open new window immediately? Or after? Let's do after to avoid flickering, or duplicate first.
                // For now, we process 'resImp' in background.

                int frames = resImp.getNFrames(); int slices = resImp.getNSlices(); int nTotal = resImp.getStackSize(); int channels = resImp.getNChannels();
                int nTimepoints = frames > 1 ? frames : (slices > 1 ? slices : nTotal / channels);
                
                if (verbose) IJ.log("--- FIA: " + mode + " ---");
                
                // Auto Reference on Result Stack
                int refChannel = 1;
                if (channels > 1) { 
                    double maxMean = -1; 
                    for (int c=1; c<=channels; c++) { 
                        int idx = resImp.getStackIndex(c, 1, 1); 
                        double mean = resImp.getStack().getProcessor(idx).getStats().mean; 
                        if (mean > maxMean) { maxMean = mean; refChannel = c; } 
                    } 
                }

                int warpMode = Video.MOTION_EUCLIDEAN;
                if (mode.equals("Translation")) warpMode = Video.MOTION_TRANSLATION; else if (mode.equals("Affine")) warpMode = Video.MOTION_AFFINE;
                
                int idx0 = resImp.getStackIndex(refChannel, 1, 1);
                Mat tpl = imagePlusToMat(resImp.getStack().getProcessor(idx0)); 
                Mat tplGray = new Mat(); tpl.convertTo(tplGray, CvType.CV_32F);
                Mat warp = Mat.eye(2, 3, CvType.CV_32F); 
                TermCriteria term = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, maxIter, Math.pow(10, -eps));
                
                if (saveMatrix) logMatrix(1, warp);

                for (int t=1; t<=nTimepoints; t++) {
                    if (isCancelled()) break;
                    
                    if (t > 1) {
                         // Use Result Stack for calculation to support dynamic updating if needed, 
                         // but standard ECC uses original frames compared to template.
                         // Here we use the frame from resStack which is currently a copy of src.
                         int idx = resImp.getStackIndex(refChannel, 1, t);
                         Mat curr = imagePlusToMat(resImp.getStack().getProcessor(idx));
                         Mat currGray = new Mat(); curr.convertTo(currGray, CvType.CV_32F);
                         try {
                             // Warm start: warp is carried over
                             Video.findTransformECC(tplGray, currGray, warp, warpMode, term, new Mat(), 5);
                             if (verbose) IJ.log("Frame " + t + ": OK");
                         } catch (Exception ex) {
                             if (verbose) IJ.log("Frame " + t + ": Fail");
                         }
                         if (saveMatrix) logMatrix(t, warp);
                    }
                    
                    // Apply to ALL channels of the Result Image
                    for (int c=1; c<=channels; c++) {
                        int idx = resImp.getStackIndex(c, 1, t);
                        ImageProcessor ip = resImp.getStack().getProcessor(idx); // This is the processor of the new image
                        Mat src = imagePlusToMat(ip);
                        Mat dst = new Mat();
                        // Inverse warp to align
                        Imgproc.warpAffine(src, dst, warp, src.size(), Imgproc.INTER_LINEAR + Imgproc.WARP_INVERSE_MAP);
                        updateImageProcessor(ip, dst);
                    }
                    publish((int)((double)t/nTimepoints*100));
                }
                return null;
            }
            
            private void logMatrix(int frame, Mat m) { float[] data = new float[6]; m.get(0, 0, data); String line = String.format("%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", frame, data[0], data[1], data[2], data[3], data[4], data[5]); matrixLog.add(line); }
            @Override protected void process(List<Integer> chunks) { int val = chunks.get(chunks.size()-1); progressBar.setValue(val); statusLabel.setText("Processing: " + val + "%"); }
            
            @Override protected void done() { 
                btnRun.setEnabled(true); btnRun.setText("Run Alignment"); statusLabel.setText("Done"); 
                
                // Show the NEW image
                if (resImp != null) {
                    resImp.show();
                    resImp.updateAndDraw();
                }

                IJ.showStatus("FIA: Finished"); 
                if (saveMatrix && !matrixLog.isEmpty()) saveMatrixFile(); 
            }
            
            private void saveMatrixFile() { SaveDialog sd = new SaveDialog("Save Transformation Matrix", "FIA_Matrix", ".csv"); if (sd.getDirectory() == null) return; String path = sd.getDirectory() + sd.getFileName(); try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) { for (String line : matrixLog) { writer.write(line); writer.newLine(); } IJ.log("Matrix saved to: " + path); } catch (IOException e) { IJ.error("Failed to save matrix: " + e.getMessage()); } }
        }
        
        private Mat imagePlusToMat(ImageProcessor ip) { int w = ip.getWidth(); int h = ip.getHeight(); if (ip instanceof ByteProcessor) { Mat m = new Mat(h, w, CvType.CV_8UC1); m.put(0,0,(byte[])ip.getPixels()); return m; } else if (ip instanceof ShortProcessor) { Mat m = new Mat(h, w, CvType.CV_16UC1); m.put(0,0,(short[])ip.getPixels()); return m; } else if (ip instanceof FloatProcessor) { Mat m = new Mat(h, w, CvType.CV_32FC1); m.put(0,0,(float[])ip.getPixels()); return m; } return null; }
        private void updateImageProcessor(ImageProcessor ip, Mat m) { if (ip instanceof ByteProcessor) { if(m.type()!=CvType.CV_8UC1) m.convertTo(m, CvType.CV_8UC1); m.get(0,0,(byte[])ip.getPixels()); } else if (ip instanceof ShortProcessor) { if(m.type()!=CvType.CV_16UC1) m.convertTo(m, CvType.CV_16UC1); m.get(0,0,(short[])ip.getPixels()); } else if (ip instanceof FloatProcessor) { if(m.type()!=CvType.CV_32FC1) m.convertTo(m, CvType.CV_32FC1); m.get(0,0,(float[])ip.getPixels()); } }
    }
}