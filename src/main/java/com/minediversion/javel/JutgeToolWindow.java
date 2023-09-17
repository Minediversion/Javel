package com.minediversion.javel;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.Timer;

//TODO: Add License and ReadMe.md

public class JutgeToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JutgeToolContent toolWindowContent = new JutgeToolContent(toolWindow, 0);
        ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "Jutge", false);
    }

    private static class JutgeToolContent{
        private final String[] fortune = {
                "Preparant el sem\u00E1for groc",
                "Segur que no t'has deixat un ;?",
                "Segmentation fault (core dumped)",
                "T'has deixat un punter penjant",
                "Que fas estudiant a aquestes hores?",
                "Segur que ara ho tens b\u00E9.",
                "Fes clic aqu\u00ED per veure la soluci\u00F3 ;)",
                "T'agraden els judicis? Proba l'ace attorney",
                "<html><center>Boss makes a dollar. I make a dime.<br/>That's why my algorithms run in exponential time</center></html>",
                "Donde está el peluche de linux?"
        };
        private final JPanel contentPanel = new JPanel();

        public JutgeToolContent(ToolWindow toolWindow, int mainId){
            try {
                contentPanel.setLayout(new BorderLayout(0, 20));
                contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 0, 20));

                if(Files.exists(net.cookiePath)){
                    String dashboard = net.checkToken(Files.readString(net.cookiePath), toolWindow);
                    if (dashboard == null) {
                        Files.delete(net.cookiePath);
                        startLogin(toolWindow);
                    }else{
                        switch (mainId) {
                            case 0 -> setupMain(toolWindow, false);
                            case 1 -> contentPanel.add(DashBoardPanel(dashboard), BorderLayout.PAGE_START);
                            case 2 -> contentPanel.add(ProblemsPanel(toolWindow), BorderLayout.PAGE_START);
                            case 3 -> contentPanel.add(ProblemsListPanel(toolWindow), BorderLayout.PAGE_START);
                        }
                    }
                }else startLogin(toolWindow);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        public JutgeToolContent(ToolWindow toolWindow, String submission, String problemId){
            contentPanel.setLayout(new BorderLayout(0, 20));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 0, 20));
            contentPanel.add(SubmissionPanel(toolWindow, submission, problemId), BorderLayout.PAGE_START);
        }

        //Content Panel
        //--------------------------------------------------------------------------------------------------------------
        //Button Functions
        private void firstLogin(credEncryption.Credentials credentials, ToolWindow toolWindow, JButton button) {
            button.setText("Loading...");
            button.setEnabled(false);
            if (!credentials.usr.getText().isEmpty() && credentials.pass.getPassword().length != 0){
                net.logIn(credentials, toolWindow);
                setupMain(toolWindow, true);
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Trying to log in without any credentials");
            button.setText("LogIn");
            button.setEnabled(true);
        }

        private void getCookieLogin(JPasswordField pass, ToolWindow toolWindow, JButton button){
            button.setText("Loading...");
            button.setEnabled(false);
            if (pass.getPassword().length != 0){
                credEncryption.Credentials credentials = credEncryption.getCredentials(pass, toolWindow);
                if (credentials == null) return;
                net.logIn(credentials, toolWindow);
                setupMain(toolWindow, true);
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Trying to log in without any credentials");
            button.setText("Unlock");
            button.setEnabled(true);
        }

        private String curProblemId = "";
        private List<String> problemStats = new ArrayList<>();

        private void searchProblem(JTextField problemId, ToolWindow toolWindow, JButton button, JButton uploadButton){
            button.setText("Loading...");
            button.setEnabled(false);
            uploadButton.setEnabled(false);
            compilerSelector.setEnabled(false);
            System.out.println(problemId.getText().trim());
            try {
                problemStats = net.getProblem(Files.readString(net.cookiePath), problemId.getText().trim(), toolWindow);
                if(problemStats == null) {
                    curProblemId = "";
                    problemViewer.setText("");
                    problemViewer.setVisible(false);
                    jScrollPane.setVisible(false);
                    button.setText("GO!");
                    button.setEnabled(true);
                    return;
                }else if(problemId.getText().trim().charAt(0) == 'P') {
                    problemViewer.setText(String.format(problemHtml[0],
                            problemStats.get(0),//Problem Title
                            problemStats.get(1),//Problem Status
                            problemStats.get(2),//Summary/Expected I/O
                            problemStats.get(3)//Public Test Cases
                    ));
                }else{
                    problemViewer.setText(String.format(problemHtml[1],
                            problemStats.get(0),//Problem Title
                            problemStats.get(1),//Problem Status
                            problemStats.get(2),//Statement
                            problemStats.get(3)//Sample Session
                    ));
                }
                for(int i = 5; i < problemStats.size(); i++){
                    compilerSelector.addItem(problemStats.get(i));
                }
                curProblemId = problemId.getText().trim();
                jScrollPane.setVisible(true);
                problemViewer.setVisible(true);
                compilerSelector.setEnabled(true);
                uploadButton.setEnabled(true);
                button.setText("GO!");
                button.setEnabled(true);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        private void uploadFile(ToolWindow toolWindow, JButton uploadButton) {
            try {
                uploadButton.setText("Uploading...");
                uploadButton.setEnabled(false);
                Editor editor = FileEditorManager.getInstance(toolWindow.getProject()).getSelectedTextEditor();
                if(editor == null){
                    JutgeNotificationHandler.notifyError(toolWindow.getProject(), "No file selected");
                    uploadButton.setText("Upload Current File");
                    uploadButton.setEnabled(true);
                    return;
                }
                Document document = editor.getDocument();
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
                String submission = net.sendFile(Files.readString(net.cookiePath), curProblemId, virtualFile, problemStats.get(4), (String) compilerSelector.getSelectedItem(), toolWindow);
                if(submission != null){
                    String body = net.checkToken(Files.readString(net.cookiePath), toolWindow);
                    if(body != null) {
                        updateDashboard(getStats(body));
                        ContentManager tabs = toolWindow.getContentManager();
                        for(Content tab : tabs.getContents())
                            if(tab.getDisplayName().equals("Submission")){
                                tabs.removeContent(tab, true);
                                break;
                            }
                        Content content = ContentFactory.getInstance()
                                .createContent(new JutgeToolContent(toolWindow, curProblemId, submission).getContentPanel(), "Submission", false);
                        content.setCloseable(true);
                        tabs.addContent(content);
                    }
                }
                uploadButton.setText("Upload Current File");
                uploadButton.setEnabled(true);
                updateProblemsList(toolWindow);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        //Creation
        private final JEditorPane problemViewer = new JEditorPane();
        private final JBScrollPane jScrollPane = new JBScrollPane(problemViewer);

        private final JComboBox<String> compilerSelector = new ComboBox<>();

        private JPanel ProblemsPanel(ToolWindow toolWindow){
            JPanel problemsPanel = new JPanel();

            HTMLEditorKit kit = new HTMLEditorKit();
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("h1 {font-weight:bold; color:white}");
            styleSheet.addRule("h2 {font-weight:bold; text-decoration:underline; color:white}");
            styleSheet.addRule("hr.solid {border-top: 3px solid #bbb;}");
            styleSheet.addRule("td, th {border: 1px solid #555555; text-align: left; padding: 8px;}");
            styleSheet.addRule("tr:nth-child(even) {background-color: #555555;}");
            styleSheet.addRule("p {font-weight:100; color:white}");
            jScrollPane.setVisible(false);
            jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            jScrollPane.setMinimumSize(new Dimension(10, 10));
            int height = (int) (Toolkit.getDefaultToolkit().getScreenSize().height*0.55);
            jScrollPane.setPreferredSize(new Dimension(1920, height));
            jScrollPane.setMaximumSize(new Dimension(1920, height));

            problemViewer.setEditorKit(kit);
            problemViewer.setContentType("text/html");
            problemViewer.setEditable(false);
            problemViewer.setVisible(false);

            JLabel title = new JLabel("PROBLEMS");
            JLabel problemIdHint = new JLabel("Problem ID:");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 50f));

            JPanel idTextField = new JPanel();
            JTextField problemId = new JTextField();
            JButton idSearch = new JButton("GO!");
            JPanel uploadField = new JPanel();
            JButton upload = new JButton("Upload Current File");
            upload.setEnabled(false);
            idTextField.setLayout(new BoxLayout(idTextField, BoxLayout.X_AXIS));
            uploadField.setLayout(new BoxLayout(uploadField, BoxLayout.X_AXIS));
            problemId.setAlignmentX(Component.LEFT_ALIGNMENT);
            idSearch.setAlignmentX(Component.RIGHT_ALIGNMENT);
            compilerSelector.setAlignmentX(Component.CENTER_ALIGNMENT);
            compilerSelector.setEnabled(false);
            compilerSelector.setEditable(false);
            upload.setAlignmentX(Component.CENTER_ALIGNMENT);
            upload.addActionListener(e -> uploadFile(toolWindow, upload));
            idSearch.addActionListener(e -> searchProblem(problemId, toolWindow, idSearch, upload));
            idTextField.add(problemId);
            idTextField.add(idSearch);
            uploadField.add(compilerSelector);
            uploadField.add(upload);

            problemsPanel.setLayout(new BoxLayout(problemsPanel, BoxLayout.Y_AXIS));

            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            problemIdHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            idTextField.setAlignmentX(Component.CENTER_ALIGNMENT);

            problemsPanel.add(title);
            problemsPanel.add(uploadField);
            problemsPanel.add(problemIdHint);
            problemsPanel.add(idTextField);
            problemsPanel.add(getFortune());
            problemsPanel.add(jScrollPane);

            return problemsPanel;
        }

        private JPanel SubmissionPanel(ToolWindow toolWindow, String problemId, String submission){
            JPanel submissionPanel = new JPanel();

            HTMLEditorKit kit = new HTMLEditorKit();
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("h1 {font-weight:bold; color:white}");
            styleSheet.addRule("hr.solid {border-top: 3px solid #bbb;}");
            styleSheet.addRule("td, th {border: 1px solid #555555; text-align: left; padding: 8px;}");
            styleSheet.addRule("tr:nth-child(even) {background-color: #555555;}");
            styleSheet.addRule("img {float: right;}");
            styleSheet.addRule("h2 {font-weight:bold; text-decoration:underline; color:white}");
            styleSheet.addRule("p1 {font-size:medium; color:white}");
            JEditorPane submissionViewer = new JEditorPane();
            JBScrollPane jScrollPaneSub = new JBScrollPane(submissionViewer);
            jScrollPaneSub.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPaneSub.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            jScrollPaneSub.setMinimumSize(new Dimension(10, 10));
            int height = (int) (Toolkit.getDefaultToolkit().getScreenSize().height*0.55);

            jScrollPaneSub.setPreferredSize(new Dimension(1920, height));
            jScrollPaneSub.setMaximumSize(new Dimension(1920, height));
            submissionViewer.setVisible(false);
            jScrollPaneSub.setVisible(false);

            JLabel judging = new JLabel("Judging submission...");
            judging.setFont(judging.getFont().deriveFont(50f));
            judging.setAlignmentX(Component.CENTER_ALIGNMENT);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        List<String> submissionStats = net.getSubmission(Files.readString(net.cookiePath), problemId, submission, toolWindow);
                        submissionViewer.setText(String.format(submissionHtml,
                                submissionStats.get(0),
                                submission,
                                submissionStats.get(1),
                                submissionStats.get(2),
                                submissionStats.get(3),
                                submissionStats.get(4)
                        ));
                        jScrollPaneSub.setVisible(true);
                        submissionViewer.setVisible(true);
                        judging.setVisible(false);
                    }catch (IOException e){
                        throw new RuntimeException(e);
                    }
                }
            }, 10000);

            submissionViewer.setEditorKit(kit);
            submissionViewer.setContentType("text/html");
            submissionViewer.setEditable(false);

            submissionPanel.setLayout(new BoxLayout(submissionPanel, BoxLayout.Y_AXIS));

            submissionPanel.add(judging);
            submissionPanel.add(jScrollPaneSub);
            submissionPanel.add(getFortune());

            return submissionPanel;
        }

        private final JLabel accProb = new JLabel("<html><font color='green'>Accepted Problems: </font>%s</html>");
        private final JLabel rejProb = new JLabel("<html><font color='red'>Rejected Problems: </font>%s</html>");
        private final JLabel sub = new JLabel("<html><font color='orange'>Submissions: </font>%s</html>");
        private final JLabel lvl = new JLabel("<html><font color='blue'>Jutge Level: </font>%s</html>");

        public void updateDashboard(List<String> dashboardInfo){
            accProb.setText(String.format(accProb.getText(), dashboardInfo.get(0)));
            rejProb.setText(String.format(rejProb.getText(), dashboardInfo.get(1)));
            sub.setText(String.format(sub.getText(), dashboardInfo.get(2)));
            lvl.setText(String.format(lvl.getText(), dashboardInfo.get(3)));
        }

        private JPanel DashBoardPanel(String body){
            JPanel dashboardPanel = new JPanel();

            List<String> dashboardInfo = getStats(body);
            JLabel title = new JLabel("DASHBOARD");

            updateDashboard(dashboardInfo);

            title.setFont(title.getFont().deriveFont(Font.BOLD, 50f));
            accProb.setFont(title.getFont().deriveFont(20f));
            rejProb.setFont(title.getFont().deriveFont(20f));
            sub.setFont(title.getFont().deriveFont(20f));
            lvl.setFont(title.getFont().deriveFont(20f));

            dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            accProb.setAlignmentX(Component.CENTER_ALIGNMENT);
            rejProb.setAlignmentX(Component.CENTER_ALIGNMENT);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);
            lvl.setAlignmentX(Component.CENTER_ALIGNMENT);

            dashboardPanel.add(title);
            dashboardPanel.add(accProb);
            dashboardPanel.add(rejProb);
            dashboardPanel.add(sub);
            dashboardPanel.add(lvl);

            return dashboardPanel;
        }

        private final JEditorPane problemListViewer = new JEditorPane();
        private final JBScrollPane jScrollPaneList = new JBScrollPane(problemListViewer);

        public void updateProblemsList(ToolWindow toolWindow){
            try{
                String body = String.format(problemsListHtml, net.getProblemList(Files.readString(net.cookiePath), toolWindow));
                body = body.replace("<i class='fa fa-thumbs-o-up fa-fw' style='color: green;'></i>",
                        "<a style='color: green; font-weight:bold'>O</a>");
                body = body.replace("<i class='fa fa-gavel fa-fw' style='color: DarkOrange;'></i>",
                        "<a style='color: orange; font-weight:bold'>/</a>");
                body = body.replace("<i class='fa fa-thumbs-o-down fa-fw' style='color: red;'></i>",
                        "<a style='color: red; font-weight:bold'>X</a>");
                body = body.replace("<span class='label label-success'>",
                        "<span class='label label-success' style='color: green; font-weight: bold'>");
                body = body.replace("<span class='label label-danger '>",
                        "<span class='label label-danger ' style='color: red; font-weight: bold'>");
                body = body.replace("<span class='label label-danger'>",
                        "<span class='label label-danger' style='color: red; font-weight: bold'>");
                body = body.replace("<span class='label label-primary'>",
                        "<span class='label label-primary' style='color: #1E90FF; font-weight: bold'>");
                body = body.replace("<a",
                        "<a style='color: white; font-weight: bold'");
                problemListViewer.setText(body);
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }

        private JPanel ProblemsListPanel(ToolWindow toolWindow) {
            JPanel problemsListPanel = new JPanel();

            jScrollPaneList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPaneList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            jScrollPaneList.setMinimumSize(new Dimension(10, 10));
            int height = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.55);

            jScrollPaneList.setPreferredSize(new Dimension(1920, height));
            jScrollPaneList.setMaximumSize(new Dimension(1920, height));

            problemsListPanel.setLayout(new BoxLayout(problemsListPanel, BoxLayout.Y_AXIS));

            problemListViewer.setContentType("text/html");
            problemListViewer.setEditable(false);
            problemListViewer.setAlignmentX(Component.CENTER_ALIGNMENT);

            updateProblemsList(toolWindow);

            problemsListPanel.add(jScrollPaneList);

            return problemsListPanel;
        }

        private JPanel LogInPanel(ToolWindow toolWindow){
            JPanel LogInPanel = new JPanel();

            JLabel title = new JLabel("JUTGE");
            JLabel usrHint = new JLabel("Username");
            JLabel passHint = new JLabel("Password");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 50f));

            JTextField usr = new JTextField();
            JPasswordField pass = new JPasswordField();

            JButton LogIn = new JButton("LogIn");
            LogIn.addActionListener(e -> firstLogin(new credEncryption.Credentials(usr, pass), toolWindow, LogIn));

            LogInPanel.setLayout(new BoxLayout(LogInPanel, BoxLayout.Y_AXIS));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            usrHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            usr.setAlignmentX(Component.CENTER_ALIGNMENT);
            passHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            pass.setAlignmentX(Component.CENTER_ALIGNMENT);
            LogIn.setAlignmentX(Component.CENTER_ALIGNMENT);

            LogInPanel.add(title);
            LogInPanel.add(usrHint);
            LogInPanel.add(usr);
            LogInPanel.add(passHint);
            LogInPanel.add(pass);
            LogInPanel.add(LogIn);
            LogInPanel.add(getFortune());

            return LogInPanel;
        }

        private JPanel EncryptionPanel(ToolWindow toolWindow){
            JPanel encryptionPanel = new JPanel();

            JLabel title = new JLabel("Unlock Account");
            JLabel passHint = new JLabel("Password");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));

            JPasswordField pass = new JPasswordField();

            JButton LogIn = new JButton("Unlock");
            LogIn.addActionListener(e -> getCookieLogin(pass, toolWindow, LogIn));

            encryptionPanel.setLayout(new BoxLayout(encryptionPanel, BoxLayout.Y_AXIS));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            passHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            pass.setAlignmentX(Component.CENTER_ALIGNMENT);
            LogIn.setAlignmentX(Component.CENTER_ALIGNMENT);

            encryptionPanel.add(title);
            encryptionPanel.add(passHint);
            encryptionPanel.add(pass);
            encryptionPanel.add(LogIn);
            encryptionPanel.add(getFortune());

            return encryptionPanel;
        }
        //--------------------------------------------------------------------------------------------------------------

        //Utility functions
        //--------------------------------------------------------------------------------------------------------------
        private JLabel getFortune(){
            JLabel fortune = new JLabel(this.fortune[new Random(System.currentTimeMillis()).nextInt(9)], SwingConstants.CENTER);
            fortune.setFont(new Font("Arial", Font.ITALIC, 11));
            fortune.setForeground(Color.YELLOW);
            fortune.setAlignmentX(Component.CENTER_ALIGNMENT);
            return fortune;
        }

        private void setupMain(ToolWindow toolWindow, boolean deleteLogInTab){
            ContentManager tabs = toolWindow.getContentManager();
            Content content = ContentFactory.getInstance()
                    .createContent(new JutgeToolContent(toolWindow, 1).getContentPanel(), "Dashboard", false);
            Content content1 = ContentFactory.getInstance()
                    .createContent(new JutgeToolContent(toolWindow, 2).getContentPanel(), "Problem", false);
            Content content2 = ContentFactory.getInstance()
                    .createContent(new JutgeToolContent(toolWindow, 3).getContentPanel(), "Problem List", false);
            content.setCloseable(false);
            content1.setCloseable(false);
            content2.setCloseable(false);
            tabs.addContent(content);
            tabs.addContent(content2);
            tabs.addContent(content1);
            if(deleteLogInTab) tabs.removeContent(tabs.getContent(0), true);
        }

        private void startLogin(ToolWindow toolWindow){
            if (!net.isLoginAvailable()) {
                contentPanel.add(LogInPanel(toolWindow), BorderLayout.PAGE_START);
            } else {
                contentPanel.add(EncryptionPanel(toolWindow), BorderLayout.PAGE_START);
            }
            ContentManager tabs = toolWindow.getContentManager();
            Content content = ContentFactory.getInstance()
                    .createContent(contentPanel, "LogIn", false);
            content.setCloseable(false);
            tabs.addContent(content);
        }

        private List<String> getStats(String body){
            List<String> dashboardInfo = new ArrayList<>();
            dashboardInfo.add(body.split("<small>Accepted Problems</small>\n" +
                    " {32}<div class='huge'>")[1]
                    .split("</div>")[0]);
            dashboardInfo.add(body.split("<small>Rejected Problems</small>\n" +
                    " {32}<div class='huge'>")[1]
                    .split("</div>")[0]);
            dashboardInfo.add(body.split("<small>Submissions</small>\n" +
                    " {32}<div class='huge'>")[1]
                    .split("</div>")[0]);
            dashboardInfo.add(body.split("<small>Judge Level</small>\n" +
                    " {32}<div class='huge'>")[1]
                    .split("</div>")[0]);
            return dashboardInfo;
        }
        //--------------------------------------------------------------------------------------------------------------

        public JPanel getContentPanel(){
            return contentPanel;
        }

        private final String problemsListHtml = """
                <html><head></head><body class="activity-stream">
                				<div class='panel panel-default'>
                				    %s
                				    </tbody></table>
                				</div>
                </body></html>""";

        private final String submissionHtml = """
                <html><head></head><body class="activity-stream">
                				<strong><h1>%s - %s</h1></strong>
                				<hr class="solid"><br>
                				<p1>Veredict: %s</p1><br>
                				<p1>Compiler: %s</p1><br>
                				<img src="https://jutge.org/%s"><br>
                				
                				%s
                		        </div>
                  </table></body></html>""";
        //1. Title
        //2. Submission
        //3. Veredict
        //4: Compiler
        //5: Image
        //6: Analytics

        private final String[] problemHtml = {"""
                <html><head></head><body>\t\t\t\t\t
                \t\t\t\t\t\t<strong><h1>%s - %s</h1></strong>
                \t\t\t\t\t\t<hr class="solid"><br>
                \t\t\t\t\t\t<p>%s</p>
                \t\t\t\t\t\t<table>
                \t\t\t\t\t\t<tbody><tr>
                \t\t\t\t\t\t\t<th>Input</th>
                \t\t\t\t\t\t\t<th>Output</th>
                \t\t\t\t\t\t<div class="list-group">"%s
                </li>
                                           \s
                                                </div>
                                            </div>""",
                """
                <html><head></head><body>\t\t\t\t\t
                \t\t\t\t\t\t<strong><h1>%s - %s</h1></strong>
                \t\t\t\t\t\t<hr class="solid"><br>
                \t\t\t\t\t\t<h2>Statement</h2>
                \t\t\t\t\t\t<p1>%s</p1>
                \t\t\t\t\t\t<h2>Sample Session</h2>
                \t\t\t\t\t\t<div class='panel-body' style='padding: 0px;'>%s</pre></div>
                                    </pre>
                                                    </div>
                                                </div>"""
        };
        /*
        1.Title
        2.Status
        3.Summary
        4.Expected Input
        5.Expected Output
        6.Public TestCases

        1.Title
        2.Status
        3.Statement
        4.Sample Session
         */
    }
}