package bnkeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

/**
 * A nice little GUI for my {@link BNKEditor}.
 * @author marieismywaifu
 */
public class EditorWindow extends JFrame {
	private static final String STATUS_WORKING = "working ...", STATUS_DONE = "Done.", STATUS_FAILED = "Failed.";
	private static final FileFilter BNK_FILTER = new FileFilter() {
		@Override public boolean accept(File f) {
			String s = f.getAbsolutePath();
			return f.isDirectory() || ".bnk".equals(s.substring(s.length() - 4).toLowerCase());
		}

		@Override public String getDescription() {
			return "Audiokinetic Wwise SoundBanks (*.bnk)";
		}
	}, WEM_FILTER = new FileFilter() {
		@Override public boolean accept(File f) {
			String s = f.getAbsolutePath();
			return f.isDirectory() || ".wem".equals(s.substring(s.length() - 4).toLowerCase());
		}

		@Override public String getDescription() {
			return "Audiokinetic Wwise Encoded Media (*.wem)";
		}
	};
	
	private final JCheckBox littleEndian;
	private final JButton saveAllWEMsButton, saveBNKButton;
	private final JLabel bnkName, status;
	private final JPanel list;
	private final JFileChooser openBNK, saveWEM, openWEM, saveAllWEMs, saveBNK;
	
	private BNKEditor editor;
	private int[] ids;
	private JLabel[] replacementNames;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override public void run() {
				EditorWindow window = new EditorWindow();
			}
		});
	}
	
	public EditorWindow () {
		list = new JPanel(new GridLayout(0, 5));
		littleEndian = new JCheckBox("Little Endian", true);
		littleEndian.setToolTipText("I don't even know if Wwise uses Big Endian at all. If you keep running into errors, try un-checking this box and re-opening the BNK.");
		bnkName = new JLabel();
		saveAllWEMsButton = new JButton("save all WEMs as ...");
		saveAllWEMsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllWEMsButtonPressed();
			}
		});
		saveAllWEMsButton.setEnabled(false);
		saveBNKButton = new JButton("save BNK as ...");
		saveBNKButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				saveBNKButtonPressed();
			}
		});
		saveBNKButton.setEnabled(false);
		status = new JLabel();
		openBNK = new JFileChooser();
		openBNK.setFileFilter(BNK_FILTER);
		openBNK.setDialogTitle("open the BNK you want to edit");
		saveWEM = new JFileChooser();
		saveWEM.setFileFilter(WEM_FILTER);
		saveWEM.setDialogTitle("choose a location to save the selected WEM");
		openWEM = new JFileChooser();
		openWEM.setFileFilter(WEM_FILTER);
		openWEM.setDialogTitle("open the WEM you want to hear instead");
		saveAllWEMs = new JFileChooser();
		saveAllWEMs.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		saveAllWEMs.setDialogTitle("choose a location to save the WEMs");
		saveBNK = new JFileChooser();
		saveBNK.setFileFilter(BNK_FILTER);
		saveBNK.setDialogTitle("choose a location to save the BNK");
		initComponents();
	}
	
	private void initComponents() {
		// complicated stuff first
		JPanel jp = new JPanel();
		jp.add(littleEndian);
		JButton jb = new JButton("open BNK ...");
		jb.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				openBNKButtonPressed();
			}
		});
		jp.add(jb);
		jp.add(bnkName);
		jp.add(saveAllWEMsButton);
		jp.add(saveBNKButton);
		jp.add(status);
		add(jp, BorderLayout.NORTH);
		add(new JScrollPane(list), BorderLayout.CENTER);
		
		// then the easy stuff
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(new Dimension((int) (d.width * 0.75), (int) (d.height * 0.75)));
		setTitle("BNKEditor v1.0");
		
		// last we call
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void openBNKButtonPressed() {
		try {
			if (openBNK.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
			status.setText(STATUS_WORKING);
			editor = new BNKEditor(openBNK.getSelectedFile(), littleEndian.isSelected());
			bnkName.setText(openBNK.getSelectedFile().getName());
			saveAllWEMsButton.setEnabled(true);
			saveBNKButton.setEnabled(true);
			ids = editor.getIDs();
			replacementNames = new JLabel[ids.length];
			list.removeAll();
			for (int i = 0; i < ids.length; i++) {
				final int id = i;
				list.add(new JLabel(Integer.toUnsignedString(ids[id])));
				JButton jb = new JButton("save WEM as ...");
				jb.addActionListener(new ActionListener() {
					@Override public void actionPerformed(ActionEvent ae) {
						saveWEMButtonPressed(id);
					}
				});
				list.add(jb);
				jb = new JButton("replace WEM with ...");
				jb.addActionListener(new ActionListener() {
					@Override public void actionPerformed(ActionEvent ae) {
						replaceWEMButtonPressed(id);
					}
				});
				list.add(jb);
				replacementNames[id] = new JLabel();
				list.add(replacementNames[id]);
				jb = new JButton("cancel");
				jb.addActionListener(new ActionListener() {
					@Override public void actionPerformed(ActionEvent ae) {
						cancelReplacementButtonPressed(id);
					}
				});
				list.add(jb);
			}
			revalidate();
			status.setText(STATUS_DONE);
		} catch (IOException ioe) {
			editor = null;
			saveAllWEMsButton.setEnabled(false);
			saveBNKButton.setEnabled(false);
			ids = null;
			replacementNames = null;
			list.removeAll();
			revalidate();
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "the following exception occured while reading from the file: " + ioe.getMessage(), "java.io.IOException", JOptionPane.ERROR_MESSAGE);
		} catch (IllegalArgumentException iae) {
			editor = null;
			saveAllWEMsButton.setEnabled(false);
			saveBNKButton.setEnabled(false);
			ids = null;
			replacementNames = null;
			list.removeAll();
			revalidate();
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "the following thing is wrong with the file you opened: " + iae.getMessage(), "java.lang.IllegalArgumentException", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			editor = null;
			saveAllWEMsButton.setEnabled(false);
			saveBNKButton.setEnabled(false);
			ids = null;
			replacementNames = null;
			list.removeAll();
			revalidate();
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "this happened: " + e.getMessage() + "\nand I have no idea how", e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void saveWEMButtonPressed(int id) {
		try {
			if (saveWEM.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
			status.setText(STATUS_WORKING);
			editor.writeWEM(id, false, saveWEM.getSelectedFile());
			status.setText(STATUS_DONE);
		} catch (IOException ioe) {
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "the following exception occured while reading from or writing to the file: " + ioe.getMessage(), "java.io.IOException", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void replaceWEMButtonPressed(int id) {
		try {
			if (openWEM.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
			status.setText(STATUS_WORKING);
			File replacement = openWEM.getSelectedFile();
			editor.replace(id, false, replacement);
			replacementNames[id].setText(replacement.getName());
			status.setText(STATUS_DONE);
		} catch (IllegalArgumentException iae) {
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "the following thing is wrong with the file you opened: " + iae.getMessage(), "java.lang.IllegalArgumentException", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void cancelReplacementButtonPressed(int id) {
		status.setText(STATUS_WORKING);
		editor.cancelReplacement(id, false);
		replacementNames[id].setText(null);
		status.setText(STATUS_DONE);
	}
	
	private void saveAllWEMsButtonPressed() {
		try {
			if (saveAllWEMs.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
			status.setText(STATUS_WORKING);
			File directory = saveAllWEMs.getSelectedFile();
			for (int i = 0; i < ids.length; i++) {
				File file = new File(directory, (i + 1) + "_" + ids[i] + ".wem");
				editor.writeWEM(i, false, file);
			}
			status.setText(STATUS_DONE);
		} catch (IOException ioe) {
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "the following exception occured while reading from or writing to the file: " + ioe.getMessage(), "java.io.IOException", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void saveBNKButtonPressed() {
		try {
			if (saveBNK.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
			status.setText(STATUS_WORKING);
			editor.writeBNK(saveBNK.getSelectedFile(), littleEndian.isSelected());
			status.setText(STATUS_DONE);
		} catch (IOException ioe) {
			status.setText(STATUS_FAILED);
			JOptionPane.showMessageDialog(this, "the following exception occured while reading from or writing to the file: " + ioe.getMessage(), "java.io.IOException", JOptionPane.ERROR_MESSAGE);
		}
	}
}