package com.minediversion.javel;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//TODO: Script Upload (Update Dashboard with upload) (TOP PRIORITY)
//TODO: Problem Browser

public class JutgeToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JutgeToolContent toolWindowContent = new JutgeToolContent(toolWindow, 0);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "Jutge", false);
    }

    private static class JutgeToolContent{
        private final String[] fortune = {"Preparant el semáfor groc",
                "Segur que no t'has deixat un ;?",
                "Segmentation fault (core dumped)",
                "T'has deixat un punter penjant",
                "Que fas estudiant a aquestes hores?",
                "Segur que ara ho tens bé.",
                "Fes clic aquí per veure la solució ;)",
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
                            case 1 -> contentPanel.add(DashBoardPanel(toolWindow, dashboard), BorderLayout.PAGE_START);
                            case 2 -> contentPanel.add(ProblemsPanel(toolWindow), BorderLayout.PAGE_START);
                        }
                    }
                }else startLogin(toolWindow);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
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

        private void searchProblem(JTextField problemId, ToolWindow toolWindow, JButton button){
            button.setText("Loading...");
            button.setEnabled(false);
            try {
                List<String> problemStats = net.getProblem(Files.readString(net.cookiePath), problemId.getText(), toolWindow);
                if(problemStats == null) {
                    problemViewer.setText("");
                    problemViewer.setVisible(false);
                    jScrollPane.setVisible(false);
                    return;
                }else if(problemId.getText().charAt(0) == 'P') {
                    problemViewer.setText(String.format(problem[0],
                            problemStats.get(0),//Problem Title
                            problemStats.get(1),//Problem Status
                            problemStats.get(2),//Summary
                            problemStats.get(3),//Expected Input
                            problemStats.get(4),//Expected Output
                            problemStats.get(5)//Public Test Cases
                    ));
                }else{
                    problemViewer.setText(String.format(problem[1],
                            problemStats.get(0),//Problem Title
                            problemStats.get(1),//Problem Status
                            problemStats.get(2),//Statement
                            problemStats.get(3)//Sample Session
                    ));
                }
                jScrollPane.setVisible(true);
                problemViewer.setVisible(true);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
            button.setText("GO!");
            button.setEnabled(true);
        }

        private void uploadFile(ToolWindow toolWindow) {
            Document document = FileEditorManager.getInstance(toolWindow.getProject()).getSelectedTextEditor().getDocument();
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            System.out.println(virtualFile.getPath());
        }

        //Creation
        private final JEditorPane problemViewer = new JEditorPane();
        private final JBScrollPane jScrollPane = new JBScrollPane(problemViewer);

        private JPanel ProblemsPanel(ToolWindow toolWindow){
            JPanel problemsPanel = new JPanel();

            HTMLEditorKit kit = new HTMLEditorKit();
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("h1 {font-weight:bold; color:white}");
            styleSheet.addRule("h2 {font-weight:bold; text-decoration:underline; color:white}");
            styleSheet.addRule("p {font-weight:100; color:white}");
            jScrollPane.setVisible(false);
            jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            jScrollPane.setMinimumSize(new Dimension(10, 10));
            int height = (int) (Toolkit.getDefaultToolkit().getScreenSize().height*0.65);
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
            JButton upload = new JButton("Upload Current File");
            idTextField.setLayout(new BoxLayout(idTextField, BoxLayout.X_AXIS));
            problemId.setAlignmentX(Component.LEFT_ALIGNMENT);
            idSearch.setAlignmentX(Component.RIGHT_ALIGNMENT);
            upload.addActionListener(e -> uploadFile(toolWindow));
            idSearch.addActionListener(e -> searchProblem(problemId, toolWindow, idSearch));
            idTextField.add(problemId);
            idTextField.add(idSearch);

            problemsPanel.setLayout(new BoxLayout(problemsPanel, BoxLayout.Y_AXIS));
            upload.setAlignmentX(Component.CENTER_ALIGNMENT);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            problemIdHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            idTextField.setAlignmentX(Component.CENTER_ALIGNMENT);

            problemsPanel.add(title);
            problemsPanel.add(upload);
            problemsPanel.add(problemIdHint);
            problemsPanel.add(idTextField);
            problemsPanel.add(getFortune());
            problemsPanel.add(jScrollPane);

            return problemsPanel;
        }

        private JPanel DashBoardPanel(ToolWindow toolWindow, String body){
            JPanel dashboardPanel = new JPanel();

            List<String> dashboardInfo = getStats(body);
            JLabel title = new JLabel("DASHBOARD");
            JLabel accProb = new JLabel("<html><font color='green'>Accepted Problems: </font>" + dashboardInfo.get(0) + "</html>");
            JLabel rejProb = new JLabel("<html><font color='red'>Rejected Problems: </font>" + dashboardInfo.get(1) + "</html>");
            JLabel sub = new JLabel("<html><font color='orange'>Submissions: </font>" + dashboardInfo.get(2) + "</html>");
            JLabel lvl = new JLabel("<html><font color='blue'>Jutge Level: </font>" + dashboardInfo.get(3) + "</html>");
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
            JLabel fortune = new JLabel(this.fortune[new Random().nextInt(9)], SwingConstants.CENTER);
            fortune.setFont(fortune.getFont().deriveFont(Font.ITALIC));
            fortune.setForeground(Color.YELLOW);
            fortune.setAlignmentX(Component.CENTER_ALIGNMENT);
            return fortune;
        }

        private void setupMain(ToolWindow toolWindow, boolean deleteLogInTab){
            ContentManager tabs = toolWindow.getContentManager();
            tabs.addContent(ContentFactory.getInstance()
                    .createContent(new JutgeToolContent(toolWindow, 1).getContentPanel(), "Dashboard", false));
            tabs.addContent(ContentFactory.getInstance()
                    .createContent(new JutgeToolContent(toolWindow, 2).getContentPanel(), "Problem", false));
            if(deleteLogInTab) tabs.removeContent(tabs.getContent(0), true);
        }

        private void startLogin(ToolWindow toolWindow){
            if (!net.isLoginAvailable()) {
                contentPanel.add(LogInPanel(toolWindow), BorderLayout.PAGE_START);
            } else {
                contentPanel.add(EncryptionPanel(toolWindow), BorderLayout.PAGE_START);
            }
            ContentManager tabs = toolWindow.getContentManager();
            tabs.addContent(ContentFactory.getInstance()
                    .createContent(contentPanel, "LogIn", false));
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

        private String[] problem = {"""
                <html><head></head><body>\t\t\t\t\t
                \t\t\t\t\t\t<strong><h1>%s - %s</h1></strong>
                \t\t\t\t\t\t<hr class="solid"><br>
                \t\t\t\t\t\t<p1>%s</p1><br>
                \t\t\t\t\t\t<h2>Expected Input</h2>
                \t\t\t\t\t\t<p1>%s</p1>
                \t\t\t\t\t\t<h2>Expected Output</h2>
                \t\t\t\t\t\t<p1>%s</p1>
                \t\t\t\t\t\t<h2>Public test case</h2>
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