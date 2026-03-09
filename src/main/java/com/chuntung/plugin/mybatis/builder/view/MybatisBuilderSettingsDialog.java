/*
 * Copyright (c) 2019-2021 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.mybatis.builder.view;

import com.chuntung.plugin.mybatis.builder.action.ConnectionSettingsListener;
import com.chuntung.plugin.mybatis.builder.action.SettingsHandler;
import com.chuntung.plugin.mybatis.builder.generator.plugins.RenamePlugin;
import com.chuntung.plugin.mybatis.builder.model.ConnectionInfo;
import com.chuntung.plugin.mybatis.builder.model.DriverTypeEnum;
import com.chuntung.plugin.mybatis.builder.util.StringUtil;
import com.chuntung.plugin.mybatis.builder.util.ViewUtil;
import com.chuntung.plugin.mybatis.builder.generator.DefaultParameters;
import com.chuntung.plugin.mybatis.builder.generator.plugins.selectwithlock.SelectWithLockConfig;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.Nullable;
import org.mybatis.generator.config.ModelType;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MybatisBuilderSettingsDialog extends DialogWrapper {
    private static final FileChooserDescriptor LIBRARY_FILE_DESCRIPTOR = new FileChooserDescriptor(false, false, true, false, false, false);
    private static final FileChooserDescriptor DATABASE_FILE_DESCRIPTOR = new FileChooserDescriptor(true, false, false, false, false, false);

    private JPanel contentPanel;
    private JList connectionList;
    private JButton addButton;
    private JButton removeButton;
    private JButton downButton;
    private JButton upButton;
    private JTextField connectionNameText;
    private JComboBox driverTypeComboBox;
    private TextFieldWithBrowseButton driverLibraryText;
    private JTextField driverClassText;
    private JTextField urlText;
    private JTextField descriptionText;
    private JPanel driverPanel;
    private JTextField hostText;
    private JSpinner portSpinner;
    private JTextField userText;
    private JPasswordField passwordText;
    private JTextField databaseText;
    private JCheckBox activeCheckBox;
    private JButton testConnectionButton;
    private JTextField javaFileEncodingText;
    private JComboBox defaultModelTypeComboBox;
    private JPanel hostPanel;
    private JPanel connectionPanel;
    private JTextField customAnnotationTypeText;
    private JTextField byPrimaryKeyOverrideText;
    private JTextField byExampleOverrideText;
    private JTextField mapperTypePatternText;
    private JTextField exampleTypePatternText;
    private JTextField sqlFileNamePatternText;
    private JTextField generatedCommentText;
    private JCheckBox forceBigDecimalsCheckbox;
    private JSpinner historySizeSpinner;
    private JButton clearAllButton;
    private JCheckBox useJSR310TypesCheckBox;
    private JLabel urlLabel;
    private TextFieldWithBrowseButton fileName;
    private JPanel fileSelection;
    private JCheckBox useSIDCheckBox;

    private final SettingsHandler settingsHandler;
    private Project project;
    private ConnectionInfo current;
    private Action applyAction;
    private boolean isSettingData = false;

    public MybatisBuilderSettingsDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        settingsHandler = SettingsHandler.getInstance(project);

        initGUI();

        init();
    }

    private void initGUI() {
        setTitle("MyBatis Builder - Settings");

        // default parameters
        initDefaultParameterPane();

        // connection info list
        List<ConnectionInfo> connectionInfoList = settingsHandler.loadConnectionInfoList();
        DefaultListModel<ConnectionInfo> listModel = new DefaultListModel();
        for (ConnectionInfo connectionInfo : connectionInfoList) {
            listModel.addElement(connectionInfo);
        }
        connectionList.setModel(listModel);
        connectionList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                ConnectionInfo item = (ConnectionInfo) value;
                return super.getListCellRendererComponent(list, item.getName(), index, isSelected, cellHasFocus);
            }
        });
        connectionList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                doSelect((JList) e.getSource());
            }
        });

        // view panel
        ViewUtil.makeAvailable(connectionPanel, false);

        driverPanel.setVisible(false);
        driverLibraryText.addBrowseFolderListener("Choose Library", "Library should contain java.sql.Driver implement ", null, LIBRARY_FILE_DESCRIPTOR);
        
        // file selection
        fileName.addBrowseFolderListener("Choose Database File", null, project, DATABASE_FILE_DESCRIPTOR);

        portSpinner.setModel(new SpinnerNumberModel(3306, 80, 65536, 1));
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));

        driverTypeComboBox.setModel(new DefaultComboBoxModel(DriverTypeEnum.values()));
        driverTypeComboBox.addItemListener(e -> {
            DriverTypeEnum item = (DriverTypeEnum) e.getItem();
            updateVisibility(item);
            
            if (!isFileBased(item)) {
                portSpinner.setValue(item.getDefaultPort());
            }
            updateOracleUrl();
        });
        driverTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                DriverTypeEnum item = (DriverTypeEnum) value;
                if (item.getIcon() != null) {
                    setIcon(ViewUtil.getIcon(item.getIcon()));
                }
                return this;
            }
        });

        // test connection button
        testConnectionButton.addActionListener(e -> doTest());

        // add button
        addButton.addActionListener(e -> doAdd(connectionList));

        // remove button
        removeButton.addActionListener(e -> doRemove(connectionList));

        upButton.addActionListener(e -> doMove(connectionList, -1));

        downButton.addActionListener(e -> doMove(connectionList, 1));

        // history size
        SpinnerNumberModel model = (SpinnerNumberModel) historySizeSpinner.getModel();
        model.setMinimum(0);
        model.setMaximum(100);

        // clear all history
        clearAllButton.addActionListener(e -> settingsHandler.clearHistory());

        useSIDCheckBox.addActionListener(e -> updateOracleUrl());

        DocumentListener urlUpdater = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateOracleUrl(); }
            public void removeUpdate(DocumentEvent e) { updateOracleUrl(); }
            public void changedUpdate(DocumentEvent e) { updateOracleUrl(); }
        };
        hostText.getDocument().addDocumentListener(urlUpdater);
        databaseText.getDocument().addDocumentListener(urlUpdater);
        portSpinner.addChangeListener(e -> updateOracleUrl());
    }
    
    private boolean isFileBased(DriverTypeEnum item) {
        return DriverTypeEnum.SqlLite.equals(item) ||
               DriverTypeEnum.DuckDB.equals(item);
    }
    
    private void updateVisibility(DriverTypeEnum item) {
        boolean isFileBased = isFileBased(item);
        boolean isOracle = DriverTypeEnum.Oracle.equals(item);
        boolean isCustom = DriverTypeEnum.Custom.equals(item);

        fileSelection.setVisible(isFileBased);
        hostPanel.setVisible(!isFileBased && !isCustom);
        driverPanel.setVisible(isCustom);

        // Hide/Show User, Password, Database and their labels
        boolean showCredentials = !isFileBased;
        setFieldAndLabelVisible(connectionPanel, userText, showCredentials, "User");
        setFieldAndLabelVisible(connectionPanel, passwordText, showCredentials, "Password");
        setFieldAndLabelVisible(connectionPanel, databaseText, showCredentials, "Database");
        setFieldAndLabelVisible(useSIDCheckBox, useSIDCheckBox, isOracle, "SID");
        if (connectionPanel != null) {
            connectionPanel.revalidate();
            connectionPanel.repaint();
        }
    }

    private void updateOracleUrl() {
        if (isSettingData) return;
        if (DriverTypeEnum.Oracle.equals(driverTypeComboBox.getSelectedItem())) {
            String host = hostText.getText();
            Object port = portSpinner.getValue();
            String database = databaseText.getText();
            String url;
            if (useSIDCheckBox.isSelected()) {
                url = String.format("jdbc:oracle:thin:@%s:%s:%s", host, port, database);
            } else {
                url = String.format("jdbc:oracle:thin:@//%s:%s/%s", host, port, database);
            }
            urlText.setText(url);
        }
    }

    private void setFieldAndLabelVisible(Container searchContainer, JComponent field, boolean visible, String labelTextHint) {
        if (field != null) {
            field.setVisible(visible);
        }

        if (searchContainer == null || labelTextHint == null) return;

        for (Component c : searchContainer.getComponents()) {
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                if (label.getText() != null && label.getText().startsWith(labelTextHint)) {
                    label.setVisible(visible);
                    break; 
                }
            }
        }
    }

    private void initDefaultParameterPane() {
        // init component
        defaultModelTypeComboBox.setModel(new DefaultComboBoxModel(ModelType.values()));

        // set data
        DefaultParameters defaultParameters = settingsHandler.getDefaultParameters();
        setData(defaultParameters);
    }

    private void doMove(JList list, int i) {
        int fromIndex = list.getSelectedIndex();
        if (fromIndex < 0) {
            return;
        }

        // check bound
        int toIndex = fromIndex + i;
        DefaultListModel model = (DefaultListModel) list.getModel();
        if (toIndex < 0 || toIndex > model.getSize() - 1) {
            return;
        }

        // swap
        Object from = model.getElementAt(fromIndex);
        Object to = model.getElementAt(toIndex);
        model.setElementAt(from, toIndex);
        model.setElementAt(to, fromIndex);
        list.setSelectedIndex(toIndex);
    }

    private void doTest() {
        // Apply the current UI state to the 'current' object before testing
        if (current != null) {
            getData(current);
        }
        settingsHandler.testConnection(current);
    }

    private void doSelect(JList list) {
        ConnectionInfo selected = (ConnectionInfo) list.getSelectedValue();
        if (selected == null) {
            // If nothing is selected, disable the panel
            ViewUtil.makeAvailable(connectionPanel, false);
            current = null;
            return;
        }

        // IMPORTANT: Save changes to the previously selected item before switching!
        if (current != null) {
            getData(current);
        }

        // Just load the data of the newly selected item into the UI
        setData(selected);
        current = selected;
    }

    private void doRemove(JList list) {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        DefaultListModel model = (DefaultListModel) list.getModel();
        model.remove(selectedIndex);
        
        // Reset current if the removed item was selected
        current = null;

        // re-select
        if (selectedIndex > model.getSize() - 1) {
            selectedIndex = model.getSize() - 1;
        }
        list.setSelectedIndex(selectedIndex);
    }

    private void doAdd(JList list) {
        ConnectionInfo blank = new ConnectionInfo();
        blank.setId(UUID.randomUUID().toString().replace("-", ""));
        blank.setName("unnamed");

        DefaultListModel model = (DefaultListModel) list.getModel();
        model.addElement(blank);
        list.setSelectedIndex(model.getSize() - 1);
    }

    private void saveAll() {
        // Apply the current UI state to the 'current' object before saving
        if (current != null) {
            getData(current);
        }

        DefaultListModel model = (DefaultListModel) connectionList.getModel();
        List<ConnectionInfo> list = new ArrayList<>(model.size());
        for (int i = 0; i < model.getSize(); i++) {
            list.add((ConnectionInfo) model.getElementAt(i));
        }

        DefaultParameters defaultParameters = new DefaultParameters();
        getData(defaultParameters);

        settingsHandler.saveAll(list, defaultParameters);
        
        // Notify listeners that settings have changed
        if (project != null) {
            project.getMessageBus().syncPublisher(ConnectionSettingsListener.TOPIC).settingsChanged();
        }
    }

    @Override
    protected void doOKAction() {
        saveAll();
        super.doOKAction();
    }

    protected void doApplyAction(ActionEvent e) {
        // simulate OK button but not close window
        saveAll();
        getApplyAction().setEnabled(false);
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{
                getCancelAction(),
                getApplyAction(),
                getOKAction(),
                getHelpAction()
        };
    }

    private Action getApplyAction() {
        if (applyAction == null) {
            applyAction = new AbstractAction("Apply") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doApplyAction(e);
                }
            };
        }

        return applyAction;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }

    public void setData(DefaultParameters defaultParameters) {
        if (defaultParameters.getDefaultModelType() == null) {
            defaultModelTypeComboBox.setSelectedIndex(0);
        } else {
            defaultModelTypeComboBox.setSelectedItem(defaultParameters.getDefaultModelType());
        }

        javaFileEncodingText.setText(defaultParameters.getJavaFileEncoding());

        generatedCommentText.setText(defaultParameters.getGeneratedComment());

        forceBigDecimalsCheckbox.setSelected(defaultParameters.getForceBigDecimals());
        useJSR310TypesCheckBox.setSelected(defaultParameters.getUseJSR310Types());

        historySizeSpinner.setValue(defaultParameters.getHistorySize());

        // plugins
        customAnnotationTypeText.setText(defaultParameters.getMapperAnnotationConfig().customAnnotationType);

        SelectWithLockConfig selectWithLockConfig = defaultParameters.getSelectWithLockConfig();
        byPrimaryKeyOverrideText.setText(selectWithLockConfig.byPrimaryKeyWithLockOverride);
        byExampleOverrideText.setText(selectWithLockConfig.byExampleWithLockOverride);

        RenamePlugin.Config renameConfig = defaultParameters.getRenameConfig();
        mapperTypePatternText.setText(renameConfig.mapperTypePattern);
        exampleTypePatternText.setText(renameConfig.exampleTypePattern);
        sqlFileNamePatternText.setText(renameConfig.sqlFileNamePattern);
    }

    public void getData(DefaultParameters defaultParameters) {
        defaultParameters.setDefaultModelType((ModelType) defaultModelTypeComboBox.getSelectedItem());
        defaultParameters.setJavaFileEncoding(javaFileEncodingText.getText());
        defaultParameters.setGeneratedComment(generatedCommentText.getText());
        defaultParameters.setForceBigDecimals(forceBigDecimalsCheckbox.isSelected());
        defaultParameters.setUseJSR310Types(useJSR310TypesCheckBox.isSelected());
        defaultParameters.setHistorySize((Integer) historySizeSpinner.getValue());

        // plugins
        defaultParameters.getMapperAnnotationConfig().customAnnotationType = customAnnotationTypeText.getText();

        SelectWithLockConfig selectWithLockConfig = defaultParameters.getSelectWithLockConfig();
        selectWithLockConfig.byPrimaryKeyWithLockOverride = byPrimaryKeyOverrideText.getText();
        selectWithLockConfig.byExampleWithLockOverride = byExampleOverrideText.getText();

        RenamePlugin.Config renameConfig = defaultParameters.getRenameConfig();
        renameConfig.mapperTypePattern = mapperTypePatternText.getText();
        renameConfig.exampleTypePattern = exampleTypePatternText.getText();
        renameConfig.sqlFileNamePattern = sqlFileNamePatternText.getText();
    }

    public void setData(ConnectionInfo data) {
        isSettingData = true;
        try {
        ViewUtil.makeAvailable(connectionPanel, true);

        connectionNameText.setText(data.getName());
        descriptionText.setText(data.getDescription());
        if (data.getDriverType() != null) {
            driverTypeComboBox.setSelectedItem(data.getDriverType());
        } else {
            driverTypeComboBox.setSelectedIndex(0);
        }
        
        updateVisibility(data.getDriverType());

        driverLibraryText.setText(data.getDriverLibrary());
        driverClassText.setText(data.getDriverClass());
        urlText.setText(data.getUrl());

        hostText.setText(data.getHost());
        if (data.getPort() != null) {
            portSpinner.setValue(data.getPort());
        }
        userText.setText(data.getUserName());
        passwordText.setText(data.getPassword());
        databaseText.setText(data.getDatabase());

        if (DriverTypeEnum.Oracle.equals(data.getDriverType())) {
            useSIDCheckBox.setSelected(data.getUrl() == null || !data.getUrl().contains("@//"));
        }
        
        if (isFileBased(data.getDriverType())) {
            fileName.setText(data.getDatabase());
        }

        activeCheckBox.setSelected(data.getActive());

        testConnectionButton.setEnabled(true);
        applyAction.setEnabled(true);
        } finally {
            isSettingData = false;
        }
    }

    public void getData(ConnectionInfo data) {
        data.setName(connectionNameText.getText());
        data.setDescription(descriptionText.getText());

        DriverTypeEnum driverType = (DriverTypeEnum) driverTypeComboBox.getSelectedItem();
        data.setDriverType(driverType);
        data.setDriverLibrary(driverLibraryText.getText());
        data.setDriverClass(driverClassText.getText());
        
        if (isFileBased(driverType)) {
            data.setDatabase(fileName.getText());
            String urlPattern = driverType.getUrlPattern();
            String url = urlPattern.replace("${db}", fileName.getText())
                                   .replace("${host}", "localhost")
                                   .replace("${port}", String.valueOf(driverType.getDefaultPort()));
            data.setUrl(url);
        } else {
            data.setUrl(urlText.getText());
            data.setDatabase(databaseText.getText());
        }

        data.setHost(hostText.getText());
        data.setPort((Integer) portSpinner.getValue());
        data.setUserName(userText.getText());
        data.setPassword(String.valueOf(passwordText.getPassword()));

        data.setActive(activeCheckBox.isSelected());
    }

    protected ValidationInfo doValidate() {
        ValidationInfo info = renameValidate(mapperTypePatternText, exampleTypePatternText, sqlFileNamePatternText);

        if (info != null) {
            ViewUtil.focusTab(info.component);
        }

        return info;
    }

    @Nullable
    private ValidationInfo renameValidate(JTextField... textFields) {
        ValidationInfo info = null;
        for (JTextField textField : textFields) {
            String pattern = textField.getText();
            if (StringUtil.stringHasValue(pattern)) {
                if (!pattern.contains(RenamePlugin.DOMAIN_NAME)) {
                    info = new ValidationInfo("Pattern should contain " + RenamePlugin.DOMAIN_NAME, textField);
                    break;
                }
            }
        }

        return info;
    }

    @Override // remember window position and size
    protected String getDimensionServiceKey() {
        return "MyBatisBuilder.SettingsDialog";
    }

    @Override
    protected String getHelpId() {
        return "https://mybatis.chuntung.com";
    }

    @Override
    protected void doHelpAction() {
        if (myHelpAction.isEnabled()) {
            BrowserUtil.browse(getHelpId());
        }
    }

    private void createUIComponents() {
        // place custom component creation code here
        urlLabel = LinkLabel.create("URL", () -> BrowserUtil.browse("https://chuntung.com/jdbc-url"));
        urlLabel.setToolTipText("Click to view URL syntax for common databases");
    }
}
