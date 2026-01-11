package com.github.epivitae.fia;

/**
 * PROJECT: FIA (Fluorescence Image Aligner)
 * AUTHOR: Kui Wang
 * VERSION: Dynamic (Reads from pom.xml)
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
import org.opencv.core.Scalar;
import nu.pattern.OpenCV;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
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
        if (!openCVLoaded) openCVLoaded = loadOpenCV();
        if (!openCVLoaded) return; 

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
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
        } catch (Exception ex) {}
    }

    private boolean loadOpenCV() {
        try {
            OpenCV.loadShared();
            IJ.log("FIA: OpenCV (Offline) loaded successfully.");
            return true;
        } catch (Throwable e) {
            IJ.log("FIA Error: " + e.getMessage());
            try { System.loadLibrary("opencv_java451"); return true; } 
            catch (Throwable ex) { IJ.error("FIA Critical", "Could not load bundled OpenCV.\n" + e.getMessage()); return false; }
        }
    }

    // --- GUI Class ---
    class FIAGui extends JFrame {
        private JToggleButton btnTranslation, btnRigid, btnAffine, btnElastic;
        private JTextField txtMaxIter, txtEpsilon, txtWinSize;
        private JCheckBox chkLog, chkSaveMatrix;
        private JButton btnRun;
        private JProgressBar progressBar;
        private JLabel statusLabel;
        
        // Colors & Fonts
        private final Font FONT_HEADER_TITLE = new Font("Arial", Font.BOLD, 18);
        private final Font FONT_HEADER_SUB = new Font("Arial", Font.PLAIN, 10);
        private final Font FONT_SECTION_HEAD = new Font("Arial", Font.BOLD, 11);
        private final Font FONT_LABEL = new Font("Arial", Font.PLAIN, 12);
        private final Font FONT_INPUT = new Font("Arial", Font.PLAIN, 13);
        private final Font FONT_BTN_RUN = new Font("Arial", Font.BOLD, 13);
        private final Font FONT_BTN_MODE = new Font("Arial", Font.BOLD, 12);
        private final Font FONT_SMALL = new Font("Arial", Font.PLAIN, 10);
        private final Font FONT_CHECKBOX = new Font("Arial", Font.PLAIN, 11);
        
        private final Color COLOR_BLUE = new Color(33, 100, 200);   
        private final Color COLOR_RED = new Color(220, 50, 50);     
        
        public FIAGui() {
            setTitle("FIA Controller " + APP_VERSION);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setResizable(false);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            setContentPane(mainPanel);

            // 1. Header
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            headerPanel.setOpaque(false);
            ImageIcon logoIcon = loadLogo();
            if (logoIcon != null) headerPanel.add(new JLabel(logoIcon));
            
            JPanel textBlock = new JPanel();
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
            textBlock.setOpaque(false);
            
            JLabel titleLabel = new JLabel("FIA Image Aligner");
            titleLabel.setFont(FONT_HEADER_TITLE);
            titleLabel.setForeground(COLOR_BLUE); 
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textBlock.add(titleLabel);
            
            JLabel subLabel = new JLabel("Version " + APP_VERSION + " | © 2026 Epivitae");
            subLabel.setFont(FONT_HEADER_SUB);
            subLabel.setForeground(Color.GRAY);
            subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textBlock.add(subLabel);
            
            headerPanel.add(textBlock);
            headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(headerPanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // 2. Split Panel
            JPanel splitPanel = new JPanel(new BorderLayout(5, 0));
            splitPanel.setBorder(createRiaBorder("Alignment Settings"));
            splitPanel.setOpaque(false);
            
            // Left Col
            JPanel leftCol = new JPanel();
            leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
            leftCol.setOpaque(false);
            leftCol.setPreferredSize(new Dimension(115, 185));

            // Step 1
            JLabel lblStep1 = new JLabel("Step 1: Rigid"); 
            lblStep1.setToolTipText("Run this first");
            lblStep1.setFont(FONT_SECTION_HEAD); lblStep1.setForeground(COLOR_BLUE); lblStep1.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftCol.add(lblStep1); leftCol.add(Box.createVerticalStrut(5));

            btnTranslation = createVerticalToggle("Translation");
            btnTranslation.addActionListener(e -> selectMode(btnTranslation));
            btnRigid = createVerticalToggle("Rigid");
            btnRigid.addActionListener(e -> selectMode(btnRigid));
            btnAffine = createVerticalToggle("Affine");
            btnAffine.addActionListener(e -> selectMode(btnAffine));

            leftCol.add(btnTranslation); leftCol.add(Box.createVerticalStrut(4));
            leftCol.add(btnRigid); leftCol.add(Box.createVerticalStrut(4));
            leftCol.add(btnAffine);
            
            // Step 2
            leftCol.add(Box.createVerticalStrut(12)); 
            JLabel lblStep2 = new JLabel("Step 2: Deformable"); 
            lblStep2.setToolTipText("Run after Step 1");
            lblStep2.setFont(FONT_SECTION_HEAD); lblStep2.setForeground(COLOR_RED); lblStep2.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftCol.add(lblStep2); leftCol.add(Box.createVerticalStrut(5));

            btnElastic = createVerticalToggle("Elastic"); 
            btnElastic.addActionListener(e -> selectMode(btnElastic));
            leftCol.add(btnElastic);

            leftCol.add(Box.createVerticalStrut(10));
            JButton btnHelp = new JButton("Help");
            btnHelp.setFont(new Font("Arial", Font.PLAIN, 10));
            btnHelp.setMargin(new Insets(1,0,1,0));
            btnHelp.setFocusPainted(false);
            btnHelp.setBackground(new Color(245, 245, 245));
            btnHelp.setForeground(Color.GRAY);
            btnHelp.setMaximumSize(new Dimension(50, 20)); 
            btnHelp.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnHelp.addActionListener(e -> showHelp());
            leftCol.add(btnHelp);
            
            splitPanel.add(leftCol, BorderLayout.WEST);

            JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
            sep.setForeground(Color.LIGHT_GRAY);
            splitPanel.add(sep, BorderLayout.CENTER);

            // Right Col
            JPanel rightCol = new JPanel();
            rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
            rightCol.setOpaque(false);
            rightCol.setBorder(new EmptyBorder(0, 8, 0, 0));
            
            addCompactField(rightCol, "Max Iterations:", txtMaxIter = new JTextField("100"));
            addCompactField(rightCol, "<html>Precision (10<sup>-x</sup>):</html>", txtEpsilon = new JTextField("5"));
            addCompactField(rightCol, "<html>Flow WinSize:</html>", txtWinSize = new JTextField("20")); 
            
            rightCol.add(Box.createVerticalStrut(5));
            chkLog = new JCheckBox("Verbose Log");
            chkLog.setFont(FONT_CHECKBOX); chkLog.setFocusPainted(false); chkLog.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightCol.add(chkLog);
            
            chkSaveMatrix = new JCheckBox("Save Matrix (.csv)");
            chkSaveMatrix.setFont(FONT_CHECKBOX); chkSaveMatrix.setFocusPainted(false); chkSaveMatrix.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightCol.add(chkSaveMatrix);
            
            splitPanel.add(rightCol, BorderLayout.EAST);
            mainPanel.add(splitPanel);
            mainPanel.add(Box.createVerticalStrut(10));

            // Run
            btnRun = new JButton("Run Alignment");
            btnRun.setFont(FONT_BTN_RUN); btnRun.setForeground(COLOR_BLUE); btnRun.setBackground(Color.WHITE);
            // Run 按钮也加上这个，防止点击后出现焦点框
            btnRun.setFocusable(false);
            btnRun.setAlignmentX(Component.CENTER_ALIGNMENT); btnRun.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
            btnRun.addActionListener(this::startAlignment);
            mainPanel.add(btnRun);
            mainPanel.add(Box.createVerticalStrut(5));

            // 4. Progress (Flat Blue)
            progressBar = new JProgressBar(0, 100);
            progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressBar.setPreferredSize(new Dimension(200, 12)); 
            progressBar.setUI(new BasicProgressBarUI() {
                @Override protected Color getSelectionBackground() { return Color.BLACK; } 
                @Override protected Color getSelectionForeground() { return Color.WHITE; }
            });
            progressBar.setForeground(COLOR_BLUE); 
            progressBar.setBackground(new Color(235, 235, 235)); 
            progressBar.setBorderPainted(false); 
            mainPanel.add(progressBar);
            
            statusLabel = new JLabel("Ready");
            statusLabel.setFont(FONT_SMALL); statusLabel.setForeground(Color.GRAY); statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(statusLabel);
            
            pack();
            selectMode(btnRigid); 
            setLocationRelativeTo(null);
        }

        private void showHelp() {
            String msg = "<html><body style='width: 400px; font-family: Arial; font-size: 11px;'>" +
                    "<h3>🔍 Mode Selection Guide</h3>" +
                    "<b><font color='#2164C8'>Step 1: Rigid</font></b> — <i>Corrects sample drift/rotation</i>" +
                    "<ul>" +
                    "<li><b>Translation:</b> Sample moved XY only.</li>" +
                    "<li><b>Rigid:</b> Sample moved + Rotated. (Standard)</li>" +
                    "<li><b>Affine:</b> Sample skewed/scaled. (Rare)</li>" +
                    "</ul>" +
                    "<b><font color='#DC3232'>Step 2: Deformable</font></b> — <i>Corrects growth/shape change</i>" +
                    "<ul>" +
                    "<li><b>Elastic:</b> Uses Optical Flow to fix non-rigid deformation.<br>" +
                    "<i>Note: Run Step 1 first to align the overall position.</i></li>" +
                    "</ul>" +
                    "<hr>" +
                    "<h3>⚙️ Parameters</h3>" +
                    "<b>Flow WinSize (Default: 20):</b><br>" +
                    "Controls how 'stiff' the correction is.<br>" +
                    "<ul>" +
                    "<li><b>10 - 25:</b> Fixes jitter/noise. (Recommended)</li>" +
                    "<li><b>100+:</b> Forces shape match (Cancels growth).</li>" +
                    "</ul>" +
                    "</body></html>";
            JOptionPane.showMessageDialog(this, msg, "FIA User Manual", JOptionPane.INFORMATION_MESSAGE);
        }

        private void addCompactField(JPanel container, String labelText, JTextField field) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS)); 
            row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT); 
            JLabel lbl = new JLabel(labelText);
            lbl.setFont(FONT_LABEL); lbl.setAlignmentX(Component.LEFT_ALIGNMENT); 
            row.add(lbl);
            field.setFont(FONT_INPUT); field.setMaximumSize(new Dimension(70, 24)); field.setAlignmentX(Component.LEFT_ALIGNMENT); 
            row.add(field);
            container.add(row); container.add(Box.createVerticalStrut(6));
        }

        private JToggleButton createVerticalToggle(String text) {
            JToggleButton btn = new JToggleButton(text);
            btn.setFont(FONT_BTN_MODE); 
            btn.setFocusPainted(false); 
            // [核心修复] 禁止按钮获取焦点，彻底根除双层边框问题！
            btn.setFocusable(false); 
            btn.setMargin(new Insets(4, 5, 4, 5));
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
            tb.setTitleFont(new Font("Arial", Font.BOLD, 12)); tb.setTitleColor(COLOR_BLUE);
            return new CompoundBorder(tb, new EmptyBorder(8, 8, 8, 8));
        }

        private void selectMode(JToggleButton target) {
            btnTranslation.setSelected(false); btnRigid.setSelected(false); btnAffine.setSelected(false); btnElastic.setSelected(false);
            target.setSelected(true);
            updateToggleStyles();
            boolean isECC = (target != btnElastic);
            if(txtMaxIter != null) txtMaxIter.setEnabled(isECC);
            if(chkSaveMatrix != null) chkSaveMatrix.setEnabled(isECC);
            if(txtWinSize != null) txtWinSize.setEnabled(!isECC);
        }

        private void updateToggleStyles() { 
            styleBtn(btnTranslation, COLOR_BLUE); 
            styleBtn(btnRigid, COLOR_BLUE); 
            styleBtn(btnAffine, COLOR_BLUE); 
            styleBtn(btnElastic, COLOR_RED); 
        }

        private void styleBtn(JToggleButton btn, Color activeColor) {
            if (btn.isSelected()) {
                btn.setForeground(activeColor); 
                btn.setBackground(Color.WHITE); 
                // 1px 极简内边框
                btn.setBorder(BorderFactory.createLineBorder(activeColor, 1));
            } else {
                btn.setForeground(Color.BLACK); 
                btn.setBackground(Color.WHITE); 
                btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            }
        }

        private void startAlignment(ActionEvent e) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp == null) { JOptionPane.showMessageDialog(this, "No image found."); return; }
            if (!openCVLoaded) { IJ.error("OpenCV Missing", "Failed to load bundled OpenCV."); return; }
            btnRun.setEnabled(false); btnRun.setText("Aligning..."); statusLabel.setText("Initializing...");
            String mode = "Rigid";
            if (btnTranslation.isSelected()) mode = "Translation"; else if (btnAffine.isSelected()) mode = "Affine"; else if (btnElastic.isSelected()) mode = "Elastic";
            int maxIter = 100; int eps = 5;
            int winSize = 20;
            try { 
                maxIter = Integer.parseInt(txtMaxIter.getText()); 
                eps = Integer.parseInt(txtEpsilon.getText()); 
                winSize = Integer.parseInt(txtWinSize.getText());
            } catch (NumberFormatException ex) {}
            new AlignmentWorker(imp, mode, maxIter, eps, winSize, chkLog.isSelected(), chkSaveMatrix.isSelected()).execute();
        }

        class AlignmentWorker extends SwingWorker<Void, Integer> {
            ImagePlus srcImp, resImp; String mode; int maxIter, eps, winSize; boolean verbose, saveMatrix;
            List<String> matrixLog = new ArrayList<>();
            Mat gridX, gridY, mapX, mapY;
            
            public AlignmentWorker(ImagePlus imp, String mode, int maxIter, int eps, int winSize, boolean verbose, boolean saveMatrix) {
                this.srcImp = imp; this.mode = mode; this.maxIter = maxIter; this.eps = eps; this.winSize = winSize; this.verbose = verbose; this.saveMatrix = saveMatrix;
            }
            
            @Override protected Void doInBackground() throws Exception {
                publish(0);
                if (saveMatrix && !mode.equals("Elastic")) matrixLog.add("Frame,m00,m01,m02,m10,m11,m12");

                ImageStack srcStack = srcImp.getStack();
                ImageStack resStack = srcStack.duplicate(); 
                resImp = new ImagePlus("FIA-" + srcImp.getTitle(), resStack);
                resImp.setCalibration(srcImp.getCalibration().copy());
                resImp.setDimensions(srcImp.getNChannels(), srcImp.getNSlices(), srcImp.getNFrames());

                int frames = srcImp.getNFrames(); int slices = srcImp.getNSlices(); int channels = srcImp.getNChannels();
                int nTimepoints = frames > 1 ? frames : slices;
                
                if (mode.equals("Elastic")) initMeshGrid(srcImp.getWidth(), srcImp.getHeight());

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
                tplRaw.convertTo(tpl, CvType.CV_32F);
                Core.normalize(tpl, tpl, 0, 1, Core.NORM_MINMAX);
                
                Mat warp = Mat.eye(2, 3, CvType.CV_32F); 
                TermCriteria term = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, maxIter, Math.pow(10, -eps));
                if (saveMatrix && !mode.equals("Elastic")) logMatrix(1, warp);

                for (int t=1; t<=nTimepoints; t++) {
                    if (isCancelled()) break;
                    if (t > 1) {
                         int idx = resImp.getStackIndex(refChannel, 1, t);
                         Mat currRaw = imagePlusToMat(resImp.getStack().getProcessor(idx));
                         Mat curr = new Mat();
                         currRaw.convertTo(curr, CvType.CV_32F);
                         Core.normalize(curr, curr, 0, 1, Core.NORM_MINMAX);
                         
                         if (mode.equals("Elastic")) {
                             Mat tpl8u = new Mat();
                             Mat curr8u = new Mat();
                             tpl.convertTo(tpl8u, CvType.CV_8UC1, 255.0);
                             curr.convertTo(curr8u, CvType.CV_8UC1, 255.0);

                             Mat flow = new Mat();
                             Video.calcOpticalFlowFarneback(tpl8u, curr8u, flow, 0.5, 5, winSize, 3, 5, 1.1, 0);
                             
                             Scalar meanFlow = Core.mean(flow);
                             double magnitude = Math.sqrt(meanFlow.val[0]*meanFlow.val[0] + meanFlow.val[1]*meanFlow.val[1]);
                             if (verbose) IJ.log(String.format("Frame %d Flow Mag: %.4f (WinSize: %d)", t, magnitude, winSize));

                             List<Mat> flowCh = new ArrayList<>();
                             Core.split(flow, flowCh); 
                             Core.add(gridX, flowCh.get(0), mapX);
                             Core.add(gridY, flowCh.get(1), mapY);
                         } else {
                             try {
                                 Video.findTransformECC(tpl, curr, warp, warpMode, term, new Mat(), 5);
                                 if (verbose) IJ.log("Frame " + t + ": OK");
                             } catch (Throwable ex) {
                                 IJ.log("FIA Warn Frame " + t + ": " + ex.getMessage());
                             }
                             if (saveMatrix) logMatrix(t, warp);
                         }
                    }
                    
                    for (int c=1; c<=channels; c++) {
                        int idx = resImp.getStackIndex(c, 1, t);
                        ImageProcessor ip = resImp.getStack().getProcessor(idx); 
                        Mat src = imagePlusToMat(ip);
                        Mat dst = new Mat();
                        
                        if (mode.equals("Elastic")) {
                             if (t > 1 && mapX != null) {
                                 Imgproc.remap(src, dst, mapX, mapY, Imgproc.INTER_LINEAR);
                                 updateImageProcessor(ip, dst);
                             }
                        } else {
                            Imgproc.warpAffine(src, dst, warp, src.size(), Imgproc.INTER_LINEAR + Imgproc.WARP_INVERSE_MAP);
                            updateImageProcessor(ip, dst);
                        }
                    }
                    publish((int)((double)t/nTimepoints*100));
                }
                return null;
            }
            
            private void initMeshGrid(int w, int h) {
                gridX = new Mat(h, w, CvType.CV_32F);
                gridY = new Mat(h, w, CvType.CV_32F);
                mapX = new Mat(h, w, CvType.CV_32F);
                mapY = new Mat(h, w, CvType.CV_32F);
                float[] rowX = new float[w];
                for(int i=0; i<w; i++) rowX[i] = i;
                for(int j=0; j<h; j++) gridX.put(j, 0, rowX);
                float[] colY = new float[w];
                for(int j=0; j<h; j++) {
                    for(int i=0; i<w; i++) colY[i] = j;
                    gridY.put(j, 0, colY);
                }
            }

            private void logMatrix(int frame, Mat m) { float[] data = new float[6]; m.get(0, 0, data); String line = String.format("%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", frame, data[0], data[1], data[2], data[3], data[4], data[5]); matrixLog.add(line); }
            @Override protected void process(List<Integer> chunks) { int val = chunks.get(chunks.size()-1); progressBar.setValue(val); statusLabel.setText("Processing: " + val + "%"); }
            @Override protected void done() { 
                btnRun.setEnabled(true); btnRun.setText("Run Alignment"); statusLabel.setText("Done"); 
                try { if (resImp != null) { resImp.setDimensions(srcImp.getNChannels(), srcImp.getNSlices(), srcImp.getNFrames()); resImp.setOpenAsHyperStack(true); if (srcImp.isComposite() || srcImp.getNChannels() > 1) { CompositeImage outComp = new CompositeImage(resImp, CompositeImage.COMPOSITE); if (srcImp instanceof CompositeImage) { CompositeImage inComp = (CompositeImage) srcImp; for (int c = 1; c <= srcImp.getNChannels(); c++) { LUT lut = inComp.getChannelLut(c); outComp.setChannelLut(lut, c); outComp.setPosition(c, 1, 1); outComp.setDisplayRange(lut.min, lut.max); } } else { for (int c = 1; c <= srcImp.getNChannels(); c++) { outComp.setChannelLut(LUT.createLutFromColor(c==1?Color.RED : (c==2?Color.GREEN : Color.BLUE)), c); outComp.setPosition(c, 1, 1); outComp.resetDisplayRange(); } } outComp.show(); } else { resImp.setDisplayRange(srcImp.getDisplayRangeMin(), srcImp.getDisplayRangeMax()); resImp.show(); } } } catch (Exception e) {}
                IJ.showStatus("FIA: Finished"); 
                if (saveMatrix && !matrixLog.isEmpty()) saveMatrixFile(); 
            }
            private void saveMatrixFile() { SaveDialog sd = new SaveDialog("Save Matrix", "FIA_Matrix", ".csv"); if (sd.getDirectory() != null) { try (BufferedWriter w = new BufferedWriter(new FileWriter(sd.getDirectory() + sd.getFileName()))) { for (String l : matrixLog) { w.write(l); w.newLine(); } } catch (Exception e) {} } }
        }
        
        private Mat imagePlusToMat(ImageProcessor ip) { int w = ip.getWidth(); int h = ip.getHeight(); if (ip instanceof ByteProcessor) { Mat m = new Mat(h, w, CvType.CV_8UC1); m.put(0,0,(byte[])ip.getPixels()); return m; } else if (ip instanceof ShortProcessor) { Mat m = new Mat(h, w, CvType.CV_16UC1); m.put(0,0,(short[])ip.getPixels()); return m; } else if (ip instanceof FloatProcessor) { Mat m = new Mat(h, w, CvType.CV_32FC1); m.put(0,0,(float[])ip.getPixels()); return m; } return null; }
        private void updateImageProcessor(ImageProcessor ip, Mat m) { if (ip instanceof ByteProcessor) { if(m.type()!=CvType.CV_8UC1) m.convertTo(m, CvType.CV_8UC1); m.get(0,0,(byte[])ip.getPixels()); } else if (ip instanceof ShortProcessor) { if(m.type()!=CvType.CV_16UC1) m.convertTo(m, CvType.CV_16UC1); m.get(0,0,(short[])ip.getPixels()); } else if (ip instanceof FloatProcessor) { if(m.type()!=CvType.CV_32FC1) m.convertTo(m, CvType.CV_32FC1); m.get(0,0,(float[])ip.getPixels()); } }
    }
}