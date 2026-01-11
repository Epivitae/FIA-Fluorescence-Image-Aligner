package com.github.epivitae.fia;

/**
 * PROJECT: FIA (Fluorescence Image Aligner)
 * AUTHOR: Kui Wang
 * VERSION: v3.0.0 (Release)
 * MENU: Plugins > Biosensor Tool > FIA Image Aligner
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
import ij.process.ColorProcessor;
import ij.io.SaveDialog;

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
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// 菜单路径保持你修改后的 Biosensor Tool
@Plugin(type = Command.class, menuPath = "Plugins>Biosensor Tool>FIA Image Aligner")
public class FIA_Command implements Command {

    private static String APP_VERSION = "Unknown";
    private static boolean openCVLoaded = false;

    @Override
    public void run() {
        loadVersionInfo();
        if (!openCVLoaded) openCVLoaded = loadOpenCV();
        
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
                APP_VERSION = prop.getProperty("version", "v3.0.0"); 
            }
        } catch (Exception ex) {}
    }

    private boolean loadOpenCV() {
        try {
            OpenCV.loadShared();
            IJ.log("FIA: OpenCV (Offline) loaded successfully.");
            return true;
        } catch (Throwable e) {
            IJ.log("FIA Warning: OpenCV failed to load (" + e.getMessage() + "). Legacy mode only.");
            return false;
        }
    }

    // --- GUI Class ---
    class FIAGui extends JFrame {
        private JComboBox<String> cmbEngine, cmbPyramid;
        private JToggleButton btnTranslation, btnRigid, btnAffine, btnElastic;
        private JTextField txtMaxIter, txtEpsilon, txtWinSize, txtAlpha;
        private JCheckBox chkLog, chkSaveMatrix;
        private JButton btnRun;
        private JProgressBar progressBar;
        private JLabel statusLabel;
        
        private JPanel panelIter, panelEps, panelWin, panelPyr, panelAlpha;

        private final Font FONT_HEADER_TITLE = new Font("Arial", Font.BOLD, 18);
        private final Font FONT_HEADER_SUB = new Font("Arial", Font.PLAIN, 10);
        private final Font FONT_SECTION_HEAD = new Font("Arial", Font.BOLD, 11);
        private final Font FONT_LABEL = new Font("Arial", Font.PLAIN, 12);
        private final Font FONT_INPUT = new Font("Arial", Font.PLAIN, 13);
        private final Font FONT_BTN_RUN = new Font("Arial", Font.BOLD, 13);
        private final Font FONT_BTN_NORMAL = new Font("Arial", Font.PLAIN, 12);
        private final Font FONT_BTN_SELECTED = new Font("Arial", Font.BOLD, 12);
        private final Font FONT_SMALL = new Font("Arial", Font.PLAIN, 10);
        private final Font FONT_CHECKBOX = new Font("Arial", Font.PLAIN, 11);
        
        private final Color COLOR_THEME_BLUE = new Color(33, 100, 200);   
        private final Color COLOR_THEME_ORANGE = new Color(210, 80, 0);   
        private final Color COLOR_TEXT_NORMAL = new Color(60, 60, 60);    
        private final Color COLOR_BORDER_GRAY = new Color(200, 200, 200); 
        
        public FIAGui() {
            setTitle("FIA Controller v" + APP_VERSION);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setResizable(false);
            setJMenuBar(createMenuBar());
            
            // [新增] 设置窗口图标 (左上角的小 Logo)
            Image icon = loadRawLogoImage();
            if (icon != null) setIconImage(icon);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            setContentPane(mainPanel);

            // 1. Header
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            headerPanel.setOpaque(false);
            
            // [修改] 调用新的缩放 Logo 方法用于界面显示
            ImageIcon logoIcon = loadScaledLogo();
            if (logoIcon != null) headerPanel.add(new JLabel(logoIcon));
            
            JPanel textBlock = new JPanel(); textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS)); textBlock.setOpaque(false);
            JLabel titleLabel = new JLabel("FIA Image Aligner"); titleLabel.setFont(FONT_HEADER_TITLE); titleLabel.setForeground(COLOR_THEME_BLUE); titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); textBlock.add(titleLabel);
            JLabel subLabel = new JLabel("v" + APP_VERSION + " | © 2026 www.cns.ac.cn"); subLabel.setFont(FONT_HEADER_SUB); subLabel.setForeground(Color.GRAY); subLabel.setAlignmentX(Component.CENTER_ALIGNMENT); textBlock.add(subLabel);
            headerPanel.add(textBlock); headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(headerPanel); mainPanel.add(Box.createVerticalStrut(15));

            // 2. Split Panel
            JPanel splitPanel = new JPanel(new BorderLayout(5, 0));
            splitPanel.setBorder(createRiaBorder("Alignment Settings"));
            splitPanel.setOpaque(false);
            
            // LEFT COLUMN
            JPanel leftCol = new JPanel(); leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS)); leftCol.setOpaque(false); leftCol.setPreferredSize(new Dimension(130, 210)); 

            JLabel lblStep1 = new JLabel("Step 1: Global"); lblStep1.setFont(FONT_SECTION_HEAD); lblStep1.setForeground(COLOR_THEME_BLUE); lblStep1.setAlignmentX(Component.LEFT_ALIGNMENT); leftCol.add(lblStep1); leftCol.add(Box.createVerticalStrut(2));

            String[] engines = openCVLoaded ? new String[]{"Engine: OpenCV", "Engine: Legacy"} : new String[]{"Engine: Legacy"};
            cmbEngine = new JComboBox<>(engines); cmbEngine.setFont(new Font("Arial", Font.PLAIN, 11)); cmbEngine.setMaximumSize(new Dimension(Short.MAX_VALUE, 22)); cmbEngine.setAlignmentX(Component.LEFT_ALIGNMENT);
            cmbEngine.addItemListener(e -> updateUIState());
            leftCol.add(cmbEngine); leftCol.add(Box.createVerticalStrut(5));

            btnTranslation = createUnifiedButton("Translation"); btnTranslation.addActionListener(e -> selectMode(btnTranslation));
            btnRigid = createUnifiedButton("Rigid"); btnRigid.addActionListener(e -> selectMode(btnRigid));
            btnAffine = createUnifiedButton("Affine"); btnAffine.addActionListener(e -> selectMode(btnAffine));
            leftCol.add(btnTranslation); leftCol.add(Box.createVerticalStrut(4)); leftCol.add(btnRigid); leftCol.add(Box.createVerticalStrut(4)); leftCol.add(btnAffine);
            
            leftCol.add(Box.createVerticalStrut(12)); 
            JLabel lblStep2 = new JLabel("Step 2: Local"); lblStep2.setFont(FONT_SECTION_HEAD); lblStep2.setForeground(COLOR_THEME_ORANGE); lblStep2.setAlignmentX(Component.LEFT_ALIGNMENT); leftCol.add(lblStep2); leftCol.add(Box.createVerticalStrut(5));
            btnElastic = createUnifiedButton("Elastic"); btnElastic.addActionListener(e -> selectMode(btnElastic)); leftCol.add(btnElastic);
            
            splitPanel.add(leftCol, BorderLayout.WEST);
            JSeparator sep = new JSeparator(SwingConstants.VERTICAL); sep.setForeground(Color.LIGHT_GRAY); splitPanel.add(sep, BorderLayout.CENTER);

            // RIGHT COLUMN
            JPanel rightCol = new JPanel(); rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS)); rightCol.setOpaque(false); rightCol.setBorder(new EmptyBorder(0, 8, 0, 0));
            
            panelIter = createCompactField("Max Iterations:", txtMaxIter = new JTextField("200")); rightCol.add(panelIter);
            panelEps = createCompactField("<html>Precision (10<sup>-x</sup>):</html>", txtEpsilon = new JTextField("7")); rightCol.add(panelEps);
            panelWin = createCompactField("<html>Flow WinSize:</html>", txtWinSize = new JTextField("5")); rightCol.add(panelWin);
            
            JLabel lblPyramid = new JLabel("Pyramid Levels:"); lblPyramid.setFont(FONT_LABEL); lblPyramid.setAlignmentX(Component.LEFT_ALIGNMENT); 
            String[] levels = {"0", "1", "2", "3", "4"};
            cmbPyramid = new JComboBox<>(levels); cmbPyramid.setSelectedIndex(1); 
            cmbPyramid.setFont(FONT_INPUT); cmbPyramid.setMaximumSize(new Dimension(70, 24)); cmbPyramid.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelPyr = new JPanel(); panelPyr.setLayout(new BoxLayout(panelPyr, BoxLayout.Y_AXIS)); panelPyr.setOpaque(false); panelPyr.setAlignmentX(Component.LEFT_ALIGNMENT); 
            panelPyr.add(lblPyramid); panelPyr.add(cmbPyramid); panelPyr.add(Box.createVerticalStrut(6));
            rightCol.add(panelPyr);

            panelAlpha = createCompactField("Update Coeff:", txtAlpha = new JTextField("0.90")); rightCol.add(panelAlpha);

            rightCol.add(Box.createVerticalStrut(5));
            chkLog = new JCheckBox("Verbose Log"); chkLog.setFont(FONT_CHECKBOX); chkLog.setFocusPainted(false); chkLog.setAlignmentX(Component.LEFT_ALIGNMENT); rightCol.add(chkLog);
            chkSaveMatrix = new JCheckBox("Save Matrix (.csv)"); chkSaveMatrix.setFont(FONT_CHECKBOX); chkSaveMatrix.setFocusPainted(false); chkSaveMatrix.setAlignmentX(Component.LEFT_ALIGNMENT); rightCol.add(chkSaveMatrix);
            
            splitPanel.add(rightCol, BorderLayout.EAST);
            mainPanel.add(splitPanel); mainPanel.add(Box.createVerticalStrut(10));

            // Run
            btnRun = new JButton("Run Alignment"); btnRun.setFont(FONT_BTN_RUN); btnRun.setForeground(COLOR_THEME_BLUE); btnRun.setBackground(Color.WHITE); btnRun.setFocusable(false); btnRun.setAlignmentX(Component.CENTER_ALIGNMENT); btnRun.setMaximumSize(new Dimension(Short.MAX_VALUE, 35));
            btnRun.addActionListener(this::startAlignment); mainPanel.add(btnRun); mainPanel.add(Box.createVerticalStrut(5));

            // Progress
            progressBar = new JProgressBar(0, 100); progressBar.setAlignmentX(Component.CENTER_ALIGNMENT); progressBar.setPreferredSize(new Dimension(200, 12)); 
            progressBar.setUI(new BasicProgressBarUI() { @Override protected Color getSelectionBackground() { return Color.BLACK; } @Override protected Color getSelectionForeground() { return Color.WHITE; } });
            progressBar.setForeground(COLOR_THEME_BLUE); progressBar.setBackground(new Color(235, 235, 235)); progressBar.setBorderPainted(false); mainPanel.add(progressBar);
            statusLabel = new JLabel("Ready"); statusLabel.setFont(FONT_SMALL); statusLabel.setForeground(Color.GRAY); statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT); mainPanel.add(statusLabel);
            
            pack(); selectMode(btnRigid); setLocationRelativeTo(null);
        }

        private JMenuBar createMenuBar() {
            JMenuBar menuBar = new JMenuBar();
            JMenu menuHelp = new JMenu("Help");
            
            JMenuItem itemManual = new JMenuItem("User Manual");
            itemManual.addActionListener(e -> showHelp());
            menuHelp.add(itemManual);
            
            JMenuItem itemAbout = new JMenuItem("About");
            itemAbout.addActionListener(e -> JOptionPane.showMessageDialog(this, FIAHelp.getAbout(APP_VERSION), "About FIA", JOptionPane.INFORMATION_MESSAGE));
            menuHelp.add(itemAbout);
            
            menuBar.add(menuHelp);
            return menuBar;
        }

        private void updateUIState() {
            boolean isElastic = btnElastic.isSelected();
            boolean isLegacy = cmbEngine.getSelectedItem().toString().contains("Legacy");

            if (isLegacy && !isElastic) {
                if (btnRigid.isSelected()) { btnTranslation.setSelected(true); btnRigid.setSelected(false); updateAllButtonStyles(); }
                btnRigid.setEnabled(false); btnRigid.setToolTipText("Not available in Legacy mode");
            } else {
                btnRigid.setEnabled(true); btnRigid.setToolTipText(null);
            }

            if (isElastic) {
                panelIter.setVisible(false); panelEps.setVisible(false);
                panelWin.setVisible(true);
                panelPyr.setVisible(false); panelAlpha.setVisible(false);
            } else {
                panelWin.setVisible(false);
                panelIter.setVisible(true); panelEps.setVisible(true);
                if (isLegacy) {
                    panelPyr.setVisible(true); panelAlpha.setVisible(true);
                } else {
                    panelPyr.setVisible(false); panelAlpha.setVisible(false);
                }
            }
            pack();
        }

        private void showHelp() { JOptionPane.showMessageDialog(this, FIAHelp.getManual(), "FIA Help", JOptionPane.INFORMATION_MESSAGE); }
        private JPanel createCompactField(String labelText, JTextField field) { JPanel row = new JPanel(); row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT); JLabel lbl = new JLabel(labelText); lbl.setFont(FONT_LABEL); lbl.setAlignmentX(Component.LEFT_ALIGNMENT); row.add(lbl); field.setFont(FONT_INPUT); field.setMaximumSize(new Dimension(70, 24)); field.setAlignmentX(Component.LEFT_ALIGNMENT); row.add(field); JPanel outer = new JPanel(); outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS)); outer.setOpaque(false); outer.setAlignmentX(Component.LEFT_ALIGNMENT); outer.add(row); outer.add(Box.createVerticalStrut(6)); return outer; }
        private JToggleButton createUnifiedButton(String text) { JToggleButton btn = new JToggleButton(text); btn.setFont(FONT_BTN_NORMAL); btn.setFocusPainted(false); btn.setFocusable(false); btn.setMargin(new Insets(4, 5, 4, 5)); btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 28)); btn.setAlignmentX(Component.LEFT_ALIGNMENT); btn.setBackground(Color.WHITE); btn.setForeground(COLOR_TEXT_NORMAL); btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_GRAY)); return btn; }
        
        // [新增] 加载原始图片用于窗口图标
        private Image loadRawLogoImage() {
            java.net.URL imgURL = getClass().getResource("/FIA.png");
            if (imgURL != null) return new ImageIcon(imgURL).getImage();
            return null;
        }

        // [修改] 原 loadLogo 改名，专门用于界面头部缩放显示
        private ImageIcon loadScaledLogo() { 
            java.net.URL imgURL = getClass().getResource("/FIA.png"); 
            return imgURL != null ? new ImageIcon(new ImageIcon(imgURL).getImage().getScaledInstance(-1, 40, Image.SCALE_SMOOTH)) : null; 
        }
        
        private Border createRiaBorder(String title) { TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title); tb.setTitleFont(new Font("Arial", Font.BOLD, 12)); tb.setTitleColor(COLOR_THEME_BLUE); return new CompoundBorder(tb, new EmptyBorder(8, 8, 8, 8)); }
        
        private void selectMode(JToggleButton target) { 
            btnTranslation.setSelected(false); btnRigid.setSelected(false); btnAffine.setSelected(false); btnElastic.setSelected(false); 
            target.setSelected(true); 
            updateAllButtonStyles(); 
            updateUIState(); // Trigger visibility update
        }
        
        private void updateAllButtonStyles() { styleSingleBtn(btnTranslation); styleSingleBtn(btnRigid); styleSingleBtn(btnAffine); styleSingleBtn(btnElastic); }
        private void styleSingleBtn(JToggleButton btn) { if (btn.isSelected()) { btn.setForeground(COLOR_THEME_BLUE); btn.setBackground(Color.WHITE); btn.setFont(FONT_BTN_SELECTED); btn.setBorder(BorderFactory.createLineBorder(COLOR_THEME_BLUE, 1)); } else { btn.setForeground(COLOR_TEXT_NORMAL); btn.setBackground(Color.WHITE); btn.setFont(FONT_BTN_NORMAL); btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_GRAY)); } }

        private void startAlignment(ActionEvent e) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp == null) { JOptionPane.showMessageDialog(this, "No image found."); return; }
            if (!openCVLoaded && cmbEngine.getSelectedItem().toString().contains("OpenCV")) { IJ.error("OpenCV Error", "Legacy mode only."); return; }
            btnRun.setEnabled(false); btnRun.setText("Aligning..."); statusLabel.setText("Initializing...");
            
            String mode = "Rigid"; if (btnTranslation.isSelected()) mode = "Translation"; else if (btnAffine.isSelected()) mode = "Affine"; else if (btnElastic.isSelected()) mode = "Elastic";
            boolean isLegacy = cmbEngine.getSelectedItem().toString().contains("Legacy");
            
            int maxIter = 200; int eps = 7; int winSize = 5; double alpha = 0.90; int pyr = 1;
            try { 
                if(panelIter.isVisible()) maxIter = Integer.parseInt(txtMaxIter.getText()); 
                if(panelEps.isVisible()) eps = Integer.parseInt(txtEpsilon.getText()); 
                if(panelWin.isVisible()) winSize = Integer.parseInt(txtWinSize.getText());
                if(panelAlpha.isVisible()) alpha = Double.parseDouble(txtAlpha.getText()); 
                if(panelPyr.isVisible()) pyr = Integer.parseInt(cmbPyramid.getSelectedItem().toString());
            } catch (NumberFormatException ex) {}
            
            int refT = (imp.getNFrames() > 1) ? imp.getFrame() : imp.getCurrentSlice();
            IJ.log("FIA: Starting alignment. Reference Frame = " + refT);
            
            new AlignmentWorker(imp, mode, isLegacy, maxIter, eps, winSize, alpha, pyr, refT, chkLog.isSelected(), chkSaveMatrix.isSelected()).execute();
        }

        class AlignmentWorker extends SwingWorker<Void, Integer> {
            ImagePlus srcImp, resImp; String mode; boolean isLegacy; int maxIter, eps, winSize, pyr, refT; double alpha; boolean verbose, saveMatrix;
            List<String> matrixLog = new ArrayList<>();
            Mat gridX, gridY, mapX, mapY;
            
            public AlignmentWorker(ImagePlus imp, String mode, boolean isLegacy, int maxIter, int eps, int winSize, double alpha, int pyr, int refT, boolean verbose, boolean saveMatrix) {
                this.srcImp = imp; this.mode = mode; this.isLegacy = isLegacy;
                this.maxIter = maxIter; this.eps = eps; this.winSize = winSize; this.alpha = alpha; this.pyr = pyr; this.refT = refT;
                this.verbose = verbose; this.saveMatrix = saveMatrix;
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
                    double maxMean = -1; for (int c=1; c<=channels; c++) { int idx = resImp.getStackIndex(c, 1, 1); double mean = resImp.getStack().getProcessor(idx).getStats().mean; if (mean > maxMean) { maxMean = mean; refChannel = c; } } 
                }

                int idxRef = resImp.getStackIndex(refChannel, 1, refT);
                ImageProcessor ipRef = resImp.getStack().getProcessor(idxRef);
                ImageProcessor ipRefFloat = ipRef.convertToFloat();

                Mat tpl = null; Mat warp = null; TermCriteria term = null;
                if (!isLegacy && !mode.equals("Elastic")) {
                    Mat tplRaw = imagePlusToMat(ipRef); tpl = new Mat(); tplRaw.convertTo(tpl, CvType.CV_32F); Core.normalize(tpl, tpl, 0, 1, Core.NORM_MINMAX);
                    warp = Mat.eye(2, 3, CvType.CV_32F); term = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, maxIter, Math.pow(10, -eps));
                }

                for (int t=1; t<=nTimepoints; t++) {
                    if (isCancelled()) break;
                    double[][] legacyWp = null;

                    if (t == refT) {
                        if (saveMatrix) { 
                             if (isLegacy) logLegacyMatrix(t, new double[][]{{0},{0}}, LegacyAligner.TRANSLATION); 
                             else logMatrix(t, Mat.eye(2,3,CvType.CV_32F));
                        }
                        publish((int)((double)t/nTimepoints*100));
                        continue; 
                    }

                    if (true) { 
                         int idx = resImp.getStackIndex(refChannel, 1, t);
                         ImageProcessor ipCurr = resImp.getStack().getProcessor(idx);
                         
                         if (mode.equals("Elastic")) {
                             calculateElasticFlow(ipRef, ipCurr, t);
                         } else {
                             if (isLegacy) {
                                 try {
                                     int type = mode.equals("Translation") ? LegacyAligner.TRANSLATION : LegacyAligner.AFFINE;
                                     legacyWp = LegacyAligner.estimate(ipCurr, ipRefFloat, type, pyr, maxIter, Math.pow(10, -eps));
                                     if (verbose) {
                                         if(type==LegacyAligner.TRANSLATION) IJ.log(String.format("Frame %d (Legacy): dx=%.2f, dy=%.2f", t, legacyWp[0][0], legacyWp[1][0]));
                                         else IJ.log(String.format("Frame %d (Legacy): Affine Estimated", t));
                                     }
                                     if (saveMatrix) logLegacyMatrix(t, legacyWp, type);
                                 } catch (Exception ex) {
                                     IJ.log("Legacy Error Frame " + t + ": " + ex.getMessage());
                                     ex.printStackTrace();
                                 }
                             } else {
                                 Mat currRaw = imagePlusToMat(ipCurr); Mat curr = new Mat(); currRaw.convertTo(curr, CvType.CV_32F); Core.normalize(curr, curr, 0, 1, Core.NORM_MINMAX);
                                 try { Video.findTransformECC(tpl, curr, warp, Video.MOTION_AFFINE, term, new Mat(), 5); } catch(Exception e){}
                                 if (saveMatrix) logMatrix(t, warp);
                             }
                         }
                    }
                    
                    ImageProcessor ipAlignedRefChannel = null;
                    for (int c=1; c<=channels; c++) {
                        int idx = resImp.getStackIndex(c, 1, t);
                        ImageProcessor ip = resImp.getStack().getProcessor(idx); 
                        
                        if (mode.equals("Elastic")) {
                             Mat src = imagePlusToMat(ip); Mat dst = new Mat();
                             if (mapX != null) { Imgproc.remap(src, dst, mapX, mapY, Imgproc.INTER_LINEAR); updateImageProcessor(ip, dst); }
                        } else {
                            if (isLegacy && legacyWp != null) {
                                int type = mode.equals("Translation") ? LegacyAligner.TRANSLATION : LegacyAligner.AFFINE;
                                ImageProcessor alignedIp = LegacyAligner.warp(ip, legacyWp, type);
                                resImp.getStack().setPixels(alignedIp.getPixels(), idx); 
                                if (c == refChannel) ipAlignedRefChannel = alignedIp;
                            } else if (!isLegacy && warp != null) {
                                Mat src = imagePlusToMat(ip); Mat dst = new Mat();
                                Imgproc.warpAffine(src, dst, warp, src.size(), Imgproc.INTER_LINEAR + Imgproc.WARP_INVERSE_MAP);
                                updateImageProcessor(ip, dst);
                            }
                        }
                    }
                    
                    if (isLegacy && !mode.equals("Elastic") && ipAlignedRefChannel != null && alpha < 1.0 && t > refT) {
                        float[] refPix = (float[]) ipRefFloat.getPixels();
                        float[] newPix = (float[]) ipAlignedRefChannel.convertToFloat().getPixels();
                        double beta = 1.0 - alpha;
                        for (int i=0; i<refPix.length; i++) {
                            if (newPix[i] != 0) refPix[i] = (float) (alpha * refPix[i] + beta * newPix[i]);
                        }
                    }
                    publish((int)((double)t/nTimepoints*100));
                }
                return null;
            }
            
            private void calculateElasticFlow(ImageProcessor ipRef, ImageProcessor ipCurr, int t) {
                 Mat tpl8u = new Mat(); Mat curr8u = new Mat();
                 Mat mRef = imagePlusToMat(ipRef); mRef.convertTo(tpl8u, CvType.CV_8UC1);
                 Mat mCurr = imagePlusToMat(ipCurr); mCurr.convertTo(curr8u, CvType.CV_8UC1);
                 Mat flow = new Mat(); Video.calcOpticalFlowFarneback(tpl8u, curr8u, flow, 0.5, 5, winSize, 3, 5, 1.1, 0);
                 Scalar meanFlow = Core.mean(flow); double magnitude = Math.sqrt(meanFlow.val[0]*meanFlow.val[0] + meanFlow.val[1]*meanFlow.val[1]);
                 if (verbose) IJ.log(String.format("Frame %d Flow Mag: %.4f", t, magnitude));
                 List<Mat> flowCh = new ArrayList<>(); Core.split(flow, flowCh); 
                 Core.add(gridX, flowCh.get(0), mapX); Core.add(gridY, flowCh.get(1), mapY);
            }
            private void initMeshGrid(int w, int h) { gridX = new Mat(h, w, CvType.CV_32F); gridY = new Mat(h, w, CvType.CV_32F); mapX = new Mat(h, w, CvType.CV_32F); mapY = new Mat(h, w, CvType.CV_32F); float[] rowX = new float[w]; for(int i=0; i<w; i++) rowX[i] = i; for(int j=0; j<h; j++) gridX.put(j, 0, rowX); float[] colY = new float[w]; for(int j=0; j<h; j++) { for(int i=0; i<w; i++) colY[i] = j; gridY.put(j, 0, colY); } }
            private void logMatrix(int frame, Mat m) { float[] data = new float[6]; m.get(0, 0, data); String line = String.format("%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", frame, data[0], data[1], data[2], data[3], data[4], data[5]); matrixLog.add(line); }
            private void logLegacyMatrix(int frame, double[][] wp, int type) { if (type == LegacyAligner.TRANSLATION) { matrixLog.add(String.format("%d,1.0,0.0,%.6f,0.0,1.0,%.6f", frame, wp[0][0], wp[1][0])); } else { matrixLog.add(String.format("%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", frame, wp[0][0]+1.0, wp[0][1], wp[0][2], wp[1][0], wp[1][1]+1.0, wp[1][2])); } }
            @Override protected void process(List<Integer> chunks) { int val = chunks.get(chunks.size()-1); progressBar.setValue(val); statusLabel.setText("Processing: " + val + "%"); }
            @Override protected void done() { 
                btnRun.setEnabled(true); btnRun.setText("Run Alignment"); statusLabel.setText("Done"); 
                try { if (resImp != null) { resImp.setDimensions(srcImp.getNChannels(), srcImp.getNSlices(), srcImp.getNFrames()); resImp.setOpenAsHyperStack(true); if (srcImp.isComposite() || srcImp.getNChannels() > 1) { CompositeImage outComp = new CompositeImage(resImp, ((CompositeImage)srcImp).getMode()); for (int c = 1; c <= srcImp.getNChannels(); c++) { LUT lut = ((CompositeImage)srcImp).getChannelLut(c); outComp.setChannelLut(lut, c); outComp.setDisplayRange(lut.min, lut.max); outComp.setPosition(c, 1, 1); } outComp.setPosition(1, 1, 1); outComp.show(); } else { resImp.setDisplayRange(srcImp.getDisplayRangeMin(), srcImp.getDisplayRangeMax()); resImp.show(); } } } catch (Exception e) {}
                IJ.showStatus("FIA: Finished"); if (saveMatrix && !matrixLog.isEmpty()) saveMatrixFile(); 
            }
            private void saveMatrixFile() { SaveDialog sd = new SaveDialog("Save Matrix", "FIA_Matrix", ".csv"); if (sd.getDirectory() != null) { try (BufferedWriter w = new BufferedWriter(new FileWriter(sd.getDirectory() + sd.getFileName()))) { for (String l : matrixLog) { w.write(l); w.newLine(); } } catch (Exception e) {} } }
        }
        private Mat imagePlusToMat(ImageProcessor ip) { int w = ip.getWidth(); int h = ip.getHeight(); if (ip instanceof ByteProcessor) { Mat m = new Mat(h, w, CvType.CV_8UC1); m.put(0,0,(byte[])ip.getPixels()); return m; } else if (ip instanceof ShortProcessor) { Mat m = new Mat(h, w, CvType.CV_16UC1); m.put(0,0,(short[])ip.getPixels()); return m; } else if (ip instanceof FloatProcessor) { Mat m = new Mat(h, w, CvType.CV_32FC1); m.put(0,0,(float[])ip.getPixels()); return m; } return null; }
        private void updateImageProcessor(ImageProcessor ip, Mat m) { if (ip instanceof ByteProcessor) { if(m.type()!=CvType.CV_8UC1) m.convertTo(m, CvType.CV_8UC1); m.get(0,0,(byte[])ip.getPixels()); } else if (ip instanceof ShortProcessor) { if(m.type()!=CvType.CV_16UC1) m.convertTo(m, CvType.CV_16UC1); m.get(0,0,(short[])ip.getPixels()); } else if (ip instanceof FloatProcessor) { if(m.type()!=CvType.CV_32FC1) m.convertTo(m, CvType.CV_32FC1); m.get(0,0,(float[])ip.getPixels()); } }
    }
}