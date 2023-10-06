package com.minediversion.javel;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import okhttp3.*;
import okhttp3.internal.JavaNetCookieJar;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class net {
    public static Path cookiePath = Path.of(System.getenv("APPDATA") + "\\JetBrains\\JutgePlugin\\tkn");

    public static boolean isLoginAvailable(){return Files.exists(credEncryption.cred);}

    @SuppressWarnings("KotlinInternalInJava")
    public static void logIn(credEncryption.Credentials credentials, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/");

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            RequestBody body = new FormBody.Builder()
                    .add("email", credentials.usr.getText())
                    .add("password", String.valueOf(credentials.pass.getPassword()))
                    .add("submit", "")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                if(response.body().string().contains("Jutge.org - Dashboard")){
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "LogIn Successful");
                    if(!isLoginAvailable()) credEncryption.createCredFile(credentials, toolWindow);
                    Files.createFile(cookiePath);
                    Files.write(cookiePath, String.valueOf(cookieJar.loadForRequest(url).get(0)).getBytes());
                }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Wrong Credentials");
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static String checkToken(String cookie, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/dashboard");

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                String body = response.body().string();
                if(!body.contains("Did you sign in")) return body;
                else{
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "Token Expired/Invalid");
                    return null;
                }
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static List<String> getProblem(String cookie, String problemId,ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/problems/"+problemId);

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                List<String> problemStats = new ArrayList<>();
                String body = response.body().string();
                if(body.contains("Wrong URL")){
                    JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Wrong Problem Id");
                    return null;
                }else if (body.contains("Quiz")){
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "Quiz are not supported");
                    return null;
                }
                problemStats.add(body.split("</title>")[0].split(" - ")[2]); //Title
                if(body.contains("Not tried")){
                    problemStats.add(body.split("<div class='panel-heading'>\n" +
                            "                    Problem ")[1]
                            .split("""
                                    </div>
                                                    <div class='panel-body'>""")[0]
                            .split(": ")[1]);//Status
                }else{
                    problemStats.add(body.split("<div class='panel-heading'>\n" +
                            "                Problem ")[1]
                            .split("""
                                    </div>
                                                <div class='panel-body'>""")[0]
                            .split(": ")[1]);//Status
                }
                if(problemId.charAt(0) == 'P') {
                    problemStats.add(body.split("<!--HEVEA command line is: hevea html.tex -->\n" +
                            "<!--CUT STYLE article--><!--CUT DEF section 1 --><p>")[1].split("</p><!--CUT END -->\n" +
                            "<!--HTMLFOOT-->\n" +
                            "<!--ENDHTML-->\n" +
                            "<!--FOOTER-->")[0]);//Summary/Expected I/O
                    try {
                        problemStats.add(body.split("<div class='list-group'>")[1]
                                .split("</li>\n" +
                                        "            \n" +
                                        "                </div>\n" +
                                        "            </div>")[0]);//Public Test Case
                    }catch (ArrayIndexOutOfBoundsException e){
                        problemStats.add("");
                    }
                }else{
                    problemStats.add(body.split("<!--CUT DEF section 1 -->")[1]
                            .split("""
                                    <!--CUT END -->
                                    <!--HTMLFOOT-->
                                    <!--ENDHTML-->
                                    <!--FOOTER-->""")[0]);//Statement
                    problemStats.add(body.split("<div class='panel-body' style='padding: 0px;'>")[1]
                            .split("""
                                    </pre></div>
                                    </pre>
                                                    </div>
                                                </div>""")[0]);//Sample Session
                }
                problemStats.add(body.split("<div class='col-sm-4'>\n" +
                        "                            <input name='token_uid' type='hidden' value='")[1]
                        .split("' />\n" +
                                "                            <button id='submit' name='submit' type='btn' class='btn btn-block btn-primary btn-sm btn-center'>")[0]);
                //token_uid
                problemStats.add(body.split("<form class='form-horizontal' action='")[1]
                        .split("' method='post' role='form' enctype='multipart/form-data'>")[0]); //ProblemId
                //CompilerOptions
                //------------------------------------------------------------------------------------------------------
                int index = body.indexOf("</option>");
                while(index >= 0){
                    problemStats.add(body.substring(index-10, index).split(">")[1]);
                    index = body.indexOf("</option>", index+1);
                }
                //------------------------------------------------------------------------------------------------------
                return problemStats;
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static String sendFile(String cookie, String problemId, VirtualFile virtualFile, String uploadToken, String compilerId, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org"+problemId);

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", virtualFile.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), new File(virtualFile.getPath())))
                    .addFormDataPart("annotation", "")
                    .addFormDataPart("compiler_id", compilerId)
                    .addFormDataPart("token_uid", uploadToken)
                    .addFormDataPart("submit", "")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            if(response.body() != null){
                String body = response.body().string();
                if (!body.contains("Judging submission")) {
                    JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Upload Error");
                }else{
                    JutgeNotificationHandler.notifyInfo(toolWindow.getProject(), "Upload Successful");
                    return body.split("<p  class='text-center'>\n" +
                            "                            Submission ")[1]
                            .split("  under analysis.\n" +
                                    "                        </p>")[0];
                }
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static List<String> getSubmission(String cookie, String problemId, String submission, ToolWindow toolWindow){
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org" + problemId + "/" + submission);

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                String body = response.body().string();
                List<String> submissionStats = new ArrayList<>();
                submissionStats.add(body.split("<a style='color: inherit;' title='Problems' href='/problems'><i class='fa fa-fw fa-puzzle-piece'></i></a>")[1]
                        .split("<small class='pull-right'>")[0]);//Title
                submissionStats.add(body.split("<th class='pull-right'>Verdict</th>\n" +
                        "            <td>\n" +
                        "            <a\n" +
                        "                href='#'\n" +
                        "                title='<b>")[1]
                        .split("</b>'\n" +
                                "                data-content=\"")[0].split(": ")[1]);//Veredict
                submissionStats.add(body.split("<th class='pull-right'>Compiler</th>\n" +
                        "            <td>\n" +
                        "            <a\n" +
                        "                href='#'\n" +
                        "                title='<b>")[1]
                .split("</b>'\n" +
                        "                data-content=\"")[0]);//Compiler
                submissionStats.add(body.split("<div class='panel-body'>\n" +
                        "                <table>\n" +
                        "                    <tr>\n" +
                        "                        <td>\n" +
                        "                            <img src = '")[1].split("'/>\n" +
                        "                        </td>\n" +
                        "                        <td>")[0]);//Image
                submissionStats.add(body.split("<div class='panel panel-default'>\n" +
                        "                <div class='panel-heading'>\n" +
                        "                    Analysis\n" +
                        "                </div>")[1]
                        .split("</div>\n" +
                                "            </div>", 2)[0]);//Analysis
                return submissionStats;
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("KotlinInternalInJava")
    public static String getProblemList(String cookie, ToolWindow toolWindow) {
        try {
            HttpUrl url = HttpUrl.parse("https://jutge.org/problems");

            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            List<Cookie> list = new ArrayList<>();
            list.add(Cookie.parse(url, cookie));
            cookieJar.saveFromResponse(url, list);
            cookieJar.loadForRequest(url);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String message) {
                    //System.out.println(message);
                }
            });
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(cookieJar)
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if(response.body() != null){
                String body = response.body().string();
                return body.split("<div class='panel panel-default'>")[1].split("</table>\n" +
                        "                </div>")[0];
            }else JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Http Error");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
