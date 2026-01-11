package com.github.epivitae.fia;

/**
 * PROJECT: FIA (Fluorescence Image Aligner)
 * AUTHOR: Kui Wang
 * VERSION: 1.4.0 (Offline Portable Edition)
 */

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.CompositeImage;
import ij.WindowManager;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.io.SaveDialog;

// OpenCV Imports
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.video.Video;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core; 
// 引入 OpenPnP 的加载器
import nu.pattern.OpenCV;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Plugin(type = Command.class, menuPath = "Plugins>FIA>FIA Image Aligner")
public class FIA_Command implements Command {

    private static String APP_VERSION = "Unknown";
    private static boolean openCVLoaded = false;

    @Override
    public void run() {
        loadVersionInfo();
        
        if (!openCVLoaded) {
            openCVLoaded = loadOpenCV();
        }

        if (!openCVLoaded) {
            return; // 失败则静默退出或已报错
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            new FIAGui().setVisible(true);
        });
    }

    // --- 极简加载逻辑 (OpenPnP) ---
    private boolean loadOpenCV() {
        try {
            // 这行代码会自动判断系统(Win/Mac/Linux)并加载内置的DLL
            OpenCV.loadShared();
            IJ.log("FIA: OpenCV (Offline) loaded successfully.");
            return true;
        } catch (Throwable e) {
            IJ.log("FIA Error: " + e.getMessage());
            e.printStackTrace();
            
            // 如果还失败，说明环境极其特殊，尝试最后的 System.loadLibrary
            try {
                System.loadLibrary("opencv_java451"); // 尝试加载特定版本
                return true;
            } catch (Throwable ex) {
                IJ.error("FIA Critical", "Could not load bundled OpenCV.\n" + e.getMessage());
                return false;
            }
        }
    }

    private void loadVersionInfo() {
        try (InputStream input = getClass().getResourceAsStream("/fia-version.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                APP_VERSION = prop.getProperty("version", "v0.0.0");
            }
        } catch (Exception ex) {}
    }

    // --- GUI Class (保持不变) ---
    class FIAGui extends JFrame {
        private JToggleButton btnTranslation, btnRigid, btnAffine;
        private JTextField txtMaxIter, txtEpsilon;
        private JCheckBox chkLog, chkSaveMatrix;
        private JButton btnRun;
        private JProgressBar progressBar;
        private JLabel statusLabel;
        
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

            // Header
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            headerPanel.setOpaque(false);
            ImageIcon logoIcon = loadLogo();
            if (logoIcon != null) headerPanel.add(new JLabel(logoIcon));
            JPanel textBlock = new JPanel();
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
            textBlock.setOpaque(false);
            JLabel titleLabel = new JLabel("FIA Image Aligner");
            titleLabel.setFont(FONT_HEADER_TITLE);
            titleLabel.setForeground(COLOR_THEME);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textBlock.add(titleLabel);
            textBlock.add(new JLabel(" ")); 
            headerPanel.add(textBlock);
            headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(headerPanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // Split Panel
            JPanel splitPanel = new JPanel(new BorderLayout(5, 0));
            splitPanel.setBorder(createRiaBorder("Alignment Parameters"));
            splitPanel.setOpaque(false);
            
            // Left
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
            leftCol.add(btnTranslation); leftCol.add(Box.createVerticalStrut(4));
            leftCol.add(btnRigid); leftCol.add(Box.createVerticalStrut(4));
            leftCol.add(btnAffine);
            
            JButton btnHelp = new JButton("Help");
            btnHelp.setFont(new Font("Arial", Font.PLAIN, 10));
            btnHelp.setMargin(new Insets(1,0,1,0));
            btnHelp.setFocusPainted(false);
            btnHelp.setBackground(new Color(245, 245, 245));
            btnHelp.setForeground(Color.GRAY);
            btnHelp.setMaximumSize(new Dimension(50, 20)); 
            btnHelp.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnHelp.addActionListener(e -> JOptionPane.showMessageDialog(this, "Modes:\n- Translation (Shift)\n- Rigid (Shift+Rotate)\n- Affine (Shear/Scale)", "Help", JOptionPane.INFORMATION_MESSAGE));
            leftCol.add(Box.createVerticalStrut(8));
            leftCol.add(btnHelp);
            selectMode(btnRigid);
            splitPanel.add(leftCol, BorderLayout.WEST);

            JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
            sep.setForeground(Color.LIGHT_GRAY);
            splitPanel.add(sep, BorderLayout.CENTER);

            // Right
            JPanel rightCol = new JPanel();
            rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
            rightCol.setOpaque(false);
            rightCol.setBorder(new EmptyBorder(0, 8, 0, 0));
            addCompactField(rightCol, "Max Iterations:", txtMaxIter = new JTextField("100"));
            addCompactField(rightCol, "<html>Precision (10<sup>-x</sup>):</html>", txtEpsilon = new JTextField("5"));
            rightCol.add(Box.createVerticalStrut(5));
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

            // Run
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
            btn.setFont(FONT_BTN_MODE); btn.setFocusPainted(false); btn.setMargin(new Insets(4, 5, 4, 5));
            btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 28));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT); btn.setBackground(Color.WHITE); btn.setForeground(Color.BLACK); 
            btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            return btn;
        }

        private ImageIcon loadLogo() {
            java.net.URL imgURL = getClass().getResource("/FIA.png");
            if (imgURL != null) return new ImageIcon(new ImageIcon(imgURL).getImage().getScaledInstance(-1, 40, Image.SCALE_SMOOTH));
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
            if (!openCVLoaded) { IJ.error("OpenCV Missing", "Failed to load bundled OpenCV."); return; }
            btnRun.setEnabled(false); btnRun.setText("Aligning..."); statusLabel.setText("Initializing...");
            String mode = "Rigid";
            if (btnTranslation.isSelected()) mode = "Translation"; else if (btnAffine.isSelected()) mode = "Affine";
            int maxIter = 100; int eps = 5;
            try { maxIter = Integer.parseInt(txtMaxIter.getText()); eps = Integer.parseInt(txtEpsilon.getText()); } catch (NumberFormatException ex) {}
            new AlignmentWorker(imp, mode, maxIter, eps, chkLog.isSelected(), chkSaveMatrix.isSelected()).execute();
        }

        class AlignmentWorker extends SwingWorker<Void, Integer> {
            ImagePlus srcImp, resImp; String mode; int maxIter, eps; boolean verbose, saveMatrix;
            List<String> matrixLog = new ArrayList<>();
            
            public AlignmentWorker(ImagePlus imp, String mode, int maxIter, int eps, boolean verbose, boolean saveMatrix) {
                this.srcImp = imp; this.mode = mode; this.maxIter = maxIter; this.eps = eps; this.verbose = verbose; this.saveMatrix = saveMatrix;
            }
            
            @Override protected Void doInBackground() throws Exception {
                publish(0);
                if (saveMatrix) matrixLog.add("Frame,m00,m01,m02,m10,m11,m12");

                ImageStack srcStack = srcImp.getStack();
                ImageStack resStack = srcStack.duplicate(); 
                resImp = new ImagePlus("FIA-" + srcImp.getTitle(), resStack);
                resImp.setCalibration(srcImp.getCalibration().copy());
                resImp.setDimensions(srcImp.getNChannels(), srcImp.getNSlices(), srcImp.getNFrames());

                int frames = srcImp.getNFrames(); int slices = srcImp.getNSlices(); int channels = srcImp.getNChannels();
                int nTimepoints = frames > 1 ? frames : slices;
                
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
                Mat tplRaw = imagePlusToMat(resImp.getStack().getProcessor(idx0)); 
                Mat tpl = new Mat();
                
                // NO BLUR (Strict)
                tplRaw.convertTo(tpl, CvType.CV_32F);
                Core.normalize(tpl, tpl, 0, 1, Core.NORM_MINMAX);
                
                Mat warp = Mat.eye(2, 3, CvType.CV_32F); 
                TermCriteria term = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, maxIter, Math.pow(10, -eps));
                if (saveMatrix) logMatrix(1, warp);

                for (int t=1; t<=nTimepoints; t++) {
                    if (isCancelled()) break;
                    if (t > 1) {
                         int idx = resImp.getStackIndex(refChannel, 1, t);
                         Mat currRaw = imagePlusToMat(resImp.getStack().getProcessor(idx));
                         Mat curr = new Mat();
                         
                         // NO BLUR
                         currRaw.convertTo(curr, CvType.CV_32F);
                         Core.normalize(curr, curr, 0, 1, Core.NORM_MINMAX);
                         
                         try {
                             Video.findTransformECC(tpl, curr, warp, warpMode, term, new Mat(), 5);
                             if (verbose) IJ.log("Frame " + t + ": OK");
                         } catch (Throwable ex) {
                             IJ.log("FIA Warn Frame " + t + ": " + ex.getMessage());
                         }
                         if (saveMatrix) logMatrix(t, warp);
                    }
                    
                    for (int c=1; c<=channels; c++) {
                        int idx = resImp.getStackIndex(c, 1, t);
                        ImageProcessor ip = resImp.getStack().getProcessor(idx); 
                        Mat src = imagePlusToMat(ip);
                        Mat dst = new Mat();
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
                try {
                    if (resImp != null) {
                        resImp.setDimensions(srcImp.getNChannels(), srcImp.getNSlices(), srcImp.getNFrames());
                        resImp.setOpenAsHyperStack(true);
                        if (srcImp.isComposite() || srcImp.getNChannels() > 1) {
                            CompositeImage outComp = new CompositeImage(resImp, CompositeImage.COMPOSITE);
                            if (srcImp instanceof CompositeImage) {
                                CompositeImage inComp = (CompositeImage) srcImp;
                                for (int c = 1; c <= srcImp.getNChannels(); c++) {
                                    LUT lut = inComp.getChannelLut(c);
                                    outComp.setChannelLut(lut, c);
                                    outComp.setPosition(c, 1, 1);
                                    outComp.setDisplayRange(lut.min, lut.max);
                                }
                            } else {
                                for (int c = 1; c <= srcImp.getNChannels(); c++) {
                                    outComp.setChannelLut(LUT.createLutFromColor(c==1?Color.RED : (c==2?Color.GREEN : Color.BLUE)), c);
                                    outComp.setPosition(c, 1, 1);
                                    outComp.resetDisplayRange();
                                }
                            }
                            outComp.show();
                        } else {
                            resImp.setDisplayRange(srcImp.getDisplayRangeMin(), srcImp.getDisplayRangeMax());
                            resImp.show();
                        }
                    }
                } catch (Exception e) {}
                IJ.showStatus("FIA: Finished"); 
                if (saveMatrix && !matrixLog.isEmpty()) saveMatrixFile(); 
            }
            private void saveMatrixFile() { SaveDialog sd = new SaveDialog("Save Matrix", "FIA_Matrix", ".csv"); if (sd.getDirectory() != null) { try (BufferedWriter w = new BufferedWriter(new FileWriter(sd.getDirectory() + sd.getFileName()))) { for (String l : matrixLog) { w.write(l); w.newLine(); } } catch (Exception e) {} } }
        }
        
        private Mat imagePlusToMat(ImageProcessor ip) { int w = ip.getWidth(); int h = ip.getHeight(); if (ip instanceof ByteProcessor) { Mat m = new Mat(h, w, CvType.CV_8UC1); m.put(0,0,(byte[])ip.getPixels()); return m; } else if (ip instanceof ShortProcessor) { Mat m = new Mat(h, w, CvType.CV_16UC1); m.put(0,0,(short[])ip.getPixels()); return m; } else if (ip instanceof FloatProcessor) { Mat m = new Mat(h, w, CvType.CV_32FC1); m.put(0,0,(float[])ip.getPixels()); return m; } return null; }
        private void updateImageProcessor(ImageProcessor ip, Mat m) { if (ip instanceof ByteProcessor) { if(m.type()!=CvType.CV_8UC1) m.convertTo(m, CvType.CV_8UC1); m.get(0,0,(byte[])ip.getPixels()); } else if (ip instanceof ShortProcessor) { if(m.type()!=CvType.CV_16UC1) m.convertTo(m, CvType.CV_16UC1); m.get(0,0,(short[])ip.getPixels()); } else if (ip instanceof FloatProcessor) { if(m.type()!=CvType.CV_32FC1) m.convertTo(m, CvType.CV_32FC1); m.get(0,0,(float[])ip.getPixels()); } }
    }
}